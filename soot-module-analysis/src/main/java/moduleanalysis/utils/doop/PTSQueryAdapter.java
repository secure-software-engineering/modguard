package moduleanalysis.utils.doop;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import groovy.lang.Closure;
import org.clyze.analysis.Analysis;
import soot.*;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.sets.*;
import soot.toolkits.scalar.Pair;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by adann on 29.08.17.
 */
public class PTSQueryAdapter {


    public Analysis doopAnalysis = null;


    public static class DoopNode extends Node {

        public int getHeap() {
            return heap;
        }

        public String getID(){
            return id;
        }

        int heap = 0;
        String id;

        protected void  setID(String id){
            this.id=id;
        }

        DoopNode(Type type, int heap,String id) {
            super(type);
            this.heap = heap;
            this.id=id;
        }

        DoopNode(Type type, int heap) {
            super(type);
            this.heap = heap;
            this.id=id;
        }

        public static final LoadingCache<Pair<Type, Integer>, DoopNode> nodeLoadingCache = CacheBuilder.newBuilder().initialCapacity(100).concurrencyLevel(Runtime.getRuntime().availableProcessors()).build(
                new CacheLoader<Pair<Type, Integer>, DoopNode>() {
                    @Override
                    public DoopNode load(Pair<Type, Integer> key) throws Exception {
                        return new DoopNode(key.getO1(), key.getO2());
                    }
                }
        );

        public static DoopNode makeDoopNode(String type, String heap) {
            //FIXME: here we use a dirty workaround
            // since the class is not existent in soot but created in doop only
            // we use the superclass to gather information
            if (type.startsWith("$Proxy$for$")) {
                String orgName = type;
                type = type.replaceFirst("\\$Proxy\\$for\\$", "");

                SootClass orgClass = ((RefType) PTSQueryAdapter.getSootType(type)).getSootClass();


                //make a dummy class
                if (orgClass.isAbstract() || orgClass.isInterface()) {

                    SootClass dummyClass = new SootClass(orgName, null);
                    dummyClass.setModifiers(Modifier.PUBLIC);

                    if (orgClass.isInterface()) {
                        dummyClass.addInterface(orgClass);
                        dummyClass.setSuperclass(ModuleScene.v().getSootClass("java.lang.Object", Optional.of("java.base")));

                    } else {
                        //we have an abstract class
                        dummyClass.setSuperclass(orgClass);
                    }
                    Scene.v().addClass(dummyClass);
                    dummyClass.setApplicationClass();
                    type = orgName;
                }


            }


            Type sootType = PTSQueryAdapter.getSootType(type);


            //cut of the star at the start
            heap = heap.substring(1, heap.length());

            int heapNumber = Integer.parseInt(heap);
            try {
                return nodeLoadingCache.get(new Pair(sootType, heapNumber));
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return null;
        }


    }


    private static Type getSootType(final String type) {
        Type sootType;
        String typeName = type;
        int dimension = 0;

        if (typeName.endsWith("[]")) {
            //get the dimension, by counting the number of "[]"
            dimension = (type.length() - type.replace("[]", "").length()) / 2;
            typeName = type.substring(0, type.indexOf("["));
        }

        switch (typeName) {
            case "null_type":
                sootType = NullType.v();
                break;
            case "int":
                sootType = IntType.v();
                break;
            case "char":
                sootType = CharType.v();
                break;
            case "long":
                sootType = LongType.v();
                break;
            case "short":
                sootType = ShortType.v();
                break;
            case "double":
                sootType = DoubleType.v();
                break;
            case "float":
                sootType = FloatType.v();
                break;
            default:
                sootType = RefType.v(type);
                break;
        }
        if (dimension > 0) {
            //FIXME: check if dimension is correct
            sootType = ArrayType.v(sootType, dimension);
        }
        return sootType;

    }
    //TODO: use sorted array set for pts...

    public static class DoopPTS extends PointsToSetInternal {

        private final HashSet<Node> s = new HashSet<Node>(4);
        private boolean empty = true;
        private int size = 0;

        public DoopPTS(Type type) {
            super(type);
        }


        @Override
        public boolean isEmpty() {
            return size == 0;
        }

        @Override
        public boolean forall(P2SetVisitor v) {
            for (Iterator<Node> it = new ArrayList<Node>(s).iterator(); it.hasNext(); ) {
                v.visit(it.next());
            }
            return v.getReturnValue();
        }

        @Override
        public boolean add(Node n) {
            // if( pag.getTypeManager().castNeverFails( n.getType(), type ) ) {

            return s.add(n);
            //  }
            // return false;
        }

        @Override
        public boolean contains(Node n) {
            return s.contains(n);
        }

        public static P2SetFactory getFactory() {
            return new P2SetFactory() {
                public PointsToSetInternal newSet(Type type, PAG pag) {
                    return new DoopPTS(type);
                }
            };
        }

        //FIXME: overrided to deal with dummy types form doop
        @Override
        public Set<Type> possibleTypes() {
            final HashSet<Type> ret = new HashSet<Type>();
            forall(new P2SetVisitor() {
                public void visit(Node n) {
                    Type t = n.getType();

                    ret.add(t);
                }
            });
            return ret;
        }
    }

    public PTSQueryObject getPTSOfArrayIndex(DoopPTS arrayPTS){
        return new PTSArrayQuery(arrayPTS);
    }
   /* public PointsToSet getPTSOfArrayIndex(DoopPTS arrayPTS) {
        List<Integer> hctxs = new ArrayList<>();
        List<Type> types = new ArrayList<>();
        arrayPTS.forall(new P2SetVisitor() {
            @Override
            public void visit(Node n) {
                if (n instanceof DoopNode) {
                    hctxs.add(((DoopNode) n).getHeap());
                    types.add(n.getType());
                }
            }
        });

        //query for doop statement
        // ArrayIndexPointsTo(?hctx, ?value, ?basehctx, ?basevalue),
        //aber über nummer finktioniert irgendwie nicht
        //_ar(?ctx,?heap) <-   ArrayIndexPointsTo(?ctx, ?heap, _, ?val), Value:Num[?val]="46126"
        //'_ar(?ctx,?heap) <-   ArrayIndexPointsTo(?ctx, ?heap, _, ?val), Value(?val), Value:Num[?val]="*46126"


        //FIXME: dirty workaround, since I have no clue how to query by key (see above queries)
        String query = " '_ar(?ctx,?type,?heap,?basearray) <-   ArrayIndexPointsTo(?ctx, ?heap, _, ?basearray), Value:Type[?heap]=?type.'";
        PointsToSet pointsToSets = null;

        ArrayList<DoopNode> nodeList = new ArrayList<>();
        final String baseArrayValue = "*" + hctxs.get(0);

        Closure myCl = new Closure(null) {


            @Override
            public Object call(Object... args) {
                //we get the output as one arg??? because it is read from STDOUT?
                //thus we split it here
                String split[] = ((String) args[0]).split(", ");
                if (split.length != 4)
                    return null;
                String ctx = split[0];
                String type = split[1];
                String heap = split[2];
                String basearray = split[3];
                if (!basearray.equals(baseArrayValue)) {
                    return null;
                }


                DoopNode node = DoopNode.makeDoopNode(type, heap);
                nodeList.add(node);
                return null;
            }
        };
        doopAnalysis.processRelation(query, myCl);


        if (nodeList.isEmpty()) {
            pointsToSets = EmptyPointsToSet.v();
        } else {
            //get the type for the new created pts
            // must be done per local ...
            //FIXME: for now a dirty little hack
            Type type = nodeList.get(0).getType();
            DoopPTS pts = new DoopPTS(type);
            for (DoopNode n : nodeList) {
                pts.add(n);
            }

            pointsToSets = pts;
        }


        return pointsToSets;
    }*/

    public Collection<PointsToSet> getPTSofMain() {

        P2SetFactory p2SetFactory = DoopPTS.getFactory();
        //Execute the doop query ...

        ArrayList<PointsToSet> pointsToSets = new ArrayList<>();
/*
          Collecting the Locals that are accessible from the outside of a Module
          phase 1: get locals in dummy main Method
         */


        //1. get the return values of the entrypoints
        // String query = "'_var(?var,?meth2,?type, ?heap) <- EntryPoint(?meth), ReturnVar(?var,?meth), VarPointsTo(_,?heap,_,?var), Value:Type[?heap]=?type, Method:DeclaringType[?meth2] = ?type.'";
        String query = "'_var(?var,?type, ?heap, ?id) <- EntryPoint(?meth), ReturnVar(?var,?meth), VarPointsTo(_,?heap,_,?var), Value:Type[?heap]=?type, Value:Id[?heap]=?id.'";
        ArrayList<ArrayList<DoopNode>> nodeList = new ArrayList<>();

        Closure myCl = new Closure(null) {
            ArrayList<DoopNode> currentVarPtS = null;

            String lastVar = "";

            @Override
            public Object call(Object... args) {
                //we get the output as one arg??? because it is read from STDOUT?
                //thus we split it here
                String split[] = ((String) args[0]).split(", ");
                if (split.length != 4)
                    return null;
                String var = split[0].trim();
                String type = split[1].trim();
                String heap = split[2].trim();
                String id = split[3].trim();

                if (!var.equals(lastVar)) {
                    currentVarPtS = new ArrayList<>();
                    nodeList.add(currentVarPtS);
                    lastVar = var;
                }

                DoopNode node = DoopNode.makeDoopNode(type, heap);
                node.setID(id);
                currentVarPtS.add(node);
                return null;
            }
        };
        doopAnalysis.processRelation(query, myCl);


        //also add the mock objects (instances from entrypoints <init> calls, and their parameters
        String mockObjQuery = "'_pts(?mock,?type,?heap,?id) <- Value:Mock:Cons[?mock]=?heap, Value:Type[?heap]=?type, MockObjFromEntryPoint(?type), Value:Id[?heap]=?id.'";


        doopAnalysis.processRelation(mockObjQuery, myCl);


        //FIXME: add MockObjects for Formals
        //        MockForFormal(?value, ?formal, ?type, ?method)
        String mockForFormal = "'_pts(?formal, ?type, ?heap, ?id) <- MockForFormal(?heap,?formal, ?type, _), Value:Id[?heap]=?id.'";

        doopAnalysis.processRelation(mockForFormal, myCl);


        String callBackParameters = "'_formOfMack(?formal, ?type, ?formalType, ?method, ?methodFormal, ?retType, ?heap, ?id) <- MockForFormal(?formalValue, ?formal, ?type, _), Value:Type[?formalValue] = ?formalType, Method:DeclaringType[?method] = ?formalType, Method:Modifier(\"public\", ?method), !Method:Modifier(\"final\",?method) , Method:DeclaringType[?method] = ?declType, ClassModifier(\"public\", ?declType), !ClassModifier(\"final\", ?declType), \n" +
                "FormalParam[_, ?method] = ?methodFormal, VarPointsTo(_, ?heap, _, ?methodFormal), Value:Type[?heap] = ?retType, Value:Id[?heap]=?id.'";


        Closure myClosureForCallBack = new Closure(null) {
            ArrayList<DoopNode> currentVarPtS = null;

            String lastVar = "";

            @Override
            public Object call(Object... args) {
                //we get the output as one arg??? because it is read from STDOUT?
                //thus we split it here
                String split[] = ((String) args[0]).split(", ");
                if (split.length != 8)
                    return null;
                String var = split[4].trim();
                String type = split[5].trim();
                String heap = split[6].trim();
                String id = split[7].trim();

                if (!var.equals(lastVar)) {
                    currentVarPtS = new ArrayList<>();
                    nodeList.add(currentVarPtS);
                    lastVar = var;
                }

                DoopNode node = DoopNode.makeDoopNode(type, heap);
                node.setID(id);
                currentVarPtS.add(node);
                return null;
            }
        };




        doopAnalysis.processRelation(callBackParameters,myClosureForCallBack);

        for (ArrayList<DoopNode> nodes : nodeList) {
            if (nodes.isEmpty()) {
                pointsToSets.add(EmptyPointsToSet.v());
            }
            //get the type for the new created pts
            // must be done per local ...
            //FIXME: for now a dirty little hack
            Type type = nodes.get(0).getType();
            DoopPTS pts = new DoopPTS(type);
            for (DoopNode n : nodes) {
                pts.add(n);
            }
            pointsToSets.add(pts);

        }


        //String get


  /*       phase 1.5: get locals of newly generated dummyClasses
          in order to get the parameter passed to this classes in a callback

          @implNote here the locals of the dummy classes are added to deal with callbacks of the module classes
         */

        //phase 2: get the exceptions that propagate to the public accessible Module's methods


        // Phase 3: get iteratively the fields of escaping Objects, and add their PointsToSet to the accessiblePTS
        // add cache for already considered types and theor PTS


        return pointsToSets;
    }


    PointsToSet getLocalPTS(Local local, SootMethod sootMethod) {
        //a query for the heap of a var
        //_pt(?heap) <-VarPointsTo(_,?heap,_,"<java.text.AttributedString: java.lang.Object access$400(java.text.AttributedString,java.text.AttributedCharacterIterator$Attribute,int,int,int)>/$r2")
        // get the types
        // Value:Type[?heap] = ?retType


        //check if local is contained in method
        Body body = sootMethod.getActiveBody();
        if (body == null) {
            throw new RuntimeException("Method does not have a body");
        }
        if (!body.getLocals().contains(local)) {
            throw new RuntimeException("Local is not contained in method");
        }


        //FIXME: do we have to distinguish for a query between, parameter locals, return locals, locals, etc..
        //IMHO: no

        String context = "_";
        String ccontext = "_";
        //create the var name for bloxbatch/doop query
        //eg. <de.upb.mod2.api.Leaker: java.lang.Integer test2()>/$r0,
        String var = sootMethod.toString() + "/" + local.getName();


        String query = String.format("'_varpts(?type,?heap) <- VarPointsTo(%s,?heap,%s,%s), Value:Type[?heap]=?type.'", context, ccontext, var);
        ArrayList<DoopNode> nodes = new ArrayList<>();

        Closure myCl = new Closure(null) {


            @Override
            public Object call(Object... args) {
                //we get the output as one arg??? because it is read from STDOUT?
                //thus we split it here
                String split[] = ((String) args[0]).split(", ");
                if (split.length != 2)
                    return null;
                String type = split[0];
                String heap = split[1];
                DoopNode node = DoopNode.makeDoopNode(type, heap);
                nodes.add(node);

                return null;
            }
        };

        doopAnalysis.processRelation(query, myCl);
        //get the type
        //FIXME: hack lock into the first one
        if (nodes.isEmpty())
            return EmptyPointsToSet.v();

        Type type = nodes.get(0).getType();
        DoopPTS pts = new DoopPTS(type);
        for (DoopNode n : nodes) {
            pts.add(n);
        }
        return pts;
    }

    public PTSQueryObject getInstanceFieldPTS(SootField field) {
        return new PTSFieldQuery(field);
    }

    /*
        public PointsToSet getInstanceFieldPTS(SootField field) {
            if (field.isStatic()) {
                throw new RuntimeException("Field is static");
            }
            ArrayList<DoopNode> nodes = new ArrayList<>();

            String context = "_";
            String basecontext = "_";
            String basevalue = "_";
            String var = field.toString();
            String query = String.format("'_pts(?type,?heap) <- InstanceFieldPointsTo(%s,?heap,\"%s\",%s,%s), Value:Type[?heap]=?type.'", context, var, basecontext, basevalue);
            Closure myCl = new Closure(null) {


                @Override
                public Object call(Object... args) {
                    //we get the output as one arg??? because it is read from STDOUT?
                    //thus we split it here
                    System.out.println(args[0]);
                    String split[] = ((String) args[0]).split(", ");
                    if (split.length != 2)
                        return null;
                    String type = split[0];
                    String heap = split[1];

                    DoopNode node = DoopNode.makeDoopNode(type, heap);
                    nodes.add(node);
                    return null;
                }
            };
            doopAnalysis.processRelation(query, myCl);


            //get the type
            //FIXME: hack lock into the first one
            if (nodes.isEmpty())
                return EmptyPointsToSet.v();

            Type type = nodes.get(0).getType();
            DoopPTS pts = new DoopPTS(type);
            for (DoopNode n : nodes) {
                pts.add(n);
            }


            return pts;


        }
    */
    public PTSQueryObject getStaticFieldPTS(SootField field) {
        return new PTSStaticFieldQuery(field);
    }

    /*public PointsToSet getStaticFieldPTS(SootField field) {
        if (!field.isStatic()) {
            throw new RuntimeException("Field is NOT static");
        }
        ArrayList<DoopNode> nodes = new ArrayList<>();

        String context = "_";
        String var = field.toString();
        String query = String.format("'_pts(?type,?heap) <- StaticFieldPointsTo(%s,?heap,\"%s\"), Value:Type[?heap]=?type.'", context, var);
        Closure myCl = new Closure(null) {


            @Override
            public Object call(Object... args) {
                //we get the output as one arg??? because it is read from STDOUT?
                //thus we split it here
                String split[] = ((String) args[0]).split(", ");
                if (split.length != 2)
                    return null;
                String type = split[0];
                String heap = split[1];

                DoopNode node = DoopNode.makeDoopNode(type, heap);
                nodes.add(node);
                return null;
            }
        };
        doopAnalysis.processRelation(query, myCl);

        //get the type
        //FIXME: hack lock into the first one
        if (nodes.isEmpty())
            return EmptyPointsToSet.v();

        Type type = nodes.get(0).getType();
        DoopPTS pts = new DoopPTS(type);
        for (DoopNode n : nodes) {
            pts.add(n);
        }


        return pts;

    }*/

}
