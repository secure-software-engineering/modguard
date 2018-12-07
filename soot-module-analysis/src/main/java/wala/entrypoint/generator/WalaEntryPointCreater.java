package wala.entrypoint.generator;

import com.google.common.base.Optional;
import soot.*;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.*;
import soot.jimple.infoflow.entryPointCreators.BaseEntryPointCreator;
import soot.options.Options;
import soot.tagkit.LineNumberTag;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by adann on 08.02.17.
 */
public class WalaEntryPointCreater extends BaseEntryPointCreator {

    private final Collection<SootMethod> methodsToCall;

    private static String dummyThreadName = "DummyThread";

    public Collection<SootClass> getDummyThreads() {
        return dummyThreads;
    }

    public SootClass getDummyMain() {
        return dummyMain;
    }

    private Collection<SootClass> dummyThreads = new HashSet<>();

    private SootClass dummyMain;

    public WalaEntryPointCreater(Collection<SootMethod> methodsToCall) {
        this.methodsToCall = methodsToCall;

    }

    @Override
    public Collection<String> getRequiredClasses() {
        return Collections.EMPTY_LIST;
    }

    @Override
    protected SootMethod createDummyMainInternal(SootMethod emptySootMethod) {
        this.dummyMain = emptySootMethod.getDeclaringClass();


        //set init method

        SootMethod constructor = new SootMethod("<init>", Collections.EMPTY_LIST, VoidType.v(), Modifier.PUBLIC);
        Body constBody = Jimple.v().newBody();
        LocalGenerator lg2 = new LocalGenerator(constBody);

        //get Object constructor
        SootClass object = ModuleScene.v().forceResolve("java.lang.Object", SootClass.BODIES, Optional.of("java.base"));
        SootMethod co = object.getMethodByName("<init>");
        //add this reference to body
        Local paramThis = lg2.generateLocal(dummyMain.getType());
        constBody.getUnits().add(Jimple.v().newIdentityStmt(paramThis,
                Jimple.v().newThisRef(dummyMain.getType())));

        //call to super const
        constBody.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(paramThis, co.makeRef())));

        constBody.getUnits().add(Jimple.v().newReturnVoidStmt());
        constBody.setMethod(constructor);
        constructor.setActiveBody(constBody);

        dummyMain.addMethod(constructor);

        dummyMain.setSuperclass(object);


        Map<SootClass, Set<SootMethod>> classMap = new HashMap<SootClass, Set<SootMethod>>();
        for (SootMethod method : methodsToCall) {
            if (!classMap.containsKey(method.getDeclaringClass())) {
                classMap.put(method.getDeclaringClass(), new HashSet<SootMethod>());
            }

            classMap.get(method.getDeclaringClass()).add(method);
        }


        Body body = emptySootMethod.getActiveBody();
        LocalGenerator lg = new LocalGenerator(body);

        // create new class:
//        HashMap<SootClass, Local> localVarsForClasses = new HashMap<SootClass, Local>();

        // create constructors:
        for (SootClass className : classMap.keySet()) {

            ModuleScene.v().forceResolve(className.toString(), SootClass.BODIES, com.google.common.base.Optional.fromNullable(className.moduleName));
            className.setApplicationClass();
            Local localVal = generateClassConstructor(className, body);
            if (localVal == null) {
                logger.warn("Cannot generate constructor for class: {}", className);
                continue;
            }
            localVarsForClasses.put(className.getType().getClassName(), localVal);
            new LineNumberTag(1);
        }
        int numOfThread = (int) Math.ceil((double) localVarsForClasses.size() / 250);
        if(numOfThread==0){
            numOfThread=1;
            System.out.println("Zero Para !!");
        }
        List<String> threadParameters = new ArrayList<>();
        threadParameters.addAll(localVarsForClasses.keySet());
        int parameterIndex=0;
        int widthOfList = (int) Math.floor((double) (threadParameters.size()/numOfThread));
        for (int i = 0; i < numOfThread; i++) {
            //FIXME: make this nice
            int lastParmeterIndex = parameterIndex+widthOfList;
            if(lastParmeterIndex>threadParameters.size()){
                lastParmeterIndex = threadParameters.size();
            }
            List<String> threadParameter = threadParameters.subList(parameterIndex,lastParmeterIndex);
            parameterIndex=parameterIndex+(threadParameters.size()/numOfThread)+1;
            SootClass threadDummyKlass = createDummyThread(threadParameter);


            //generate the dummyThread
            Local localVal = generateClassConstructor(threadDummyKlass, body);
            if (localVal == null && Options.v().verbose()) {
                logger.warn("Cannot generate constructor for class: {}", threadDummyKlass);
            }
            localVarsForClasses.put(threadDummyKlass.getType().getClassName(), localVal);
            new LineNumberTag(1);
        }
        body.getUnits().add(Jimple.v().newReturnVoidStmt());


        return emptySootMethod;
    }


    public SootClass createDummyThread(Collection<String> parameters) {
        SootClass dummyThreadClass = null;
//        if (Scene.v().containsClass(dummyThreadName)) {
//            int methodIndex = 0;
//            dummyThreadClass = Scene.v().getSootClass(dummyThreadName);
//        } else {
//            dummyThreadClass = new SootClass(dummyThreadName);
//            this.dummyThread = dummyThreadClass;
//        }
        String klassName = dummyThreadName;
        if (Scene.v().containsClass(klassName)) {
            int methodIndex = 0;
            while (Scene.v().containsClass(klassName))
                klassName = dummyThreadName + "_" + methodIndex++;
        }
            dummyThreadClass = new SootClass(klassName);
            this.dummyThreads.add(dummyThreadClass);



        SootClass klassThread = ModuleScene.v().forceResolve("java.lang.Thread", SootClass.BODIES, Optional.of("java.base"));

        dummyThreadClass.setSuperclass(klassThread);

        Body body = Jimple.v().newBody();


        SootClass thread = ModuleScene.v().forceResolve("java.lang.Thread", SootClass.BODIES, Optional.of("java.base"));
        SootMethod threadCon = thread.getMethod("void <init>()");

        List<Type> parameterTypes = parameters.stream().map(x -> RefType.v(x)).collect(Collectors.toList());

        SootMethod constructor = new SootMethod("<init>", parameterTypes, VoidType.v(), Modifier.PUBLIC);

        LocalGenerator lg = new LocalGenerator(body);

        //add this reference to body
        Local paramThis = lg.generateLocal(klassThread.getType());
        body.getUnits().add(Jimple.v().newIdentityStmt(paramThis,
                Jimple.v().newThisRef(klassThread.getType())));


        // Add a parameter reference to the body
        int i = 0;
        for (Type type : parameterTypes) {
            Local paramLocal = lg.generateLocal(type);
            body.getUnits().add(Jimple.v().newIdentityStmt(paramLocal,
                    Jimple.v().newParameterRef(type, i)));
            i++;
        }


        //call to super const
        body.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(paramThis, threadCon.makeRef())));

        body.getUnits().add(Jimple.v().newReturnVoidStmt());


        body.setMethod(constructor);
        constructor.setActiveBody(body);

        dummyThreadClass.addMethod(constructor);

        Scene.v().addClass(dummyThreadClass);
        return dummyThreadClass;
    }


    @Override
    protected Local generateClassConstructor(SootClass createdClass, Body body,
                                             Set<SootClass> constructionStack, Set<SootClass> parentClasses) {
        if (createdClass == null || this.failedClasses.contains(createdClass))
            return null;

        // We cannot create instances of phantom classes as we do not have any
        // constructor information for them
        if (createdClass.isPhantom() || createdClass.isPhantomClass()) {
            logger.warn("Cannot generate constructor for phantom class {}", createdClass.getName());
            failedClasses.add(createdClass);
            return null;
        }

        LocalGenerator generator = new LocalGenerator(body);

        // if sootClass is simpleClass:
        if (isSimpleType(createdClass.toString())) {
            Local varLocal = generator.generateLocal(getSimpleTypeFromType(createdClass.getType()));

            AssignStmt aStmt = Jimple.v().newAssignStmt(varLocal, getSimpleDefaultValue(createdClass.toString()));
            body.getUnits().add(aStmt);
            return varLocal;
        }

        boolean isInnerClass = createdClass.getName().contains("$");
        String outerClass = isInnerClass ? createdClass.getName().substring
                (0, createdClass.getName().lastIndexOf("$")) : "";

        // Make sure that we don't run into loops
        if (!constructionStack.add(createdClass)) {
            logger.warn("Ran into a constructor generation loop for class " + createdClass
                    + ", substituting with null...");
            Local tempLocal = generator.generateLocal(RefType.v(createdClass));
            AssignStmt assignStmt = Jimple.v().newAssignStmt(tempLocal, NullConstant.v());
            body.getUnits().add(assignStmt);
            return tempLocal;
        }
        if (createdClass.isInterface() || createdClass.isAbstract()) {
            if (substituteCallParams) {
                // Find a matching implementor of the interface
                List<SootClass> classes;
                if (createdClass.isInterface())
                    classes = Scene.v().getActiveHierarchy().getImplementersOf(createdClass);
                else
                    classes = Scene.v().getActiveHierarchy().getSubclassesOf(createdClass);

                // Generate an instance of the substitution class. If we fail,
                // try the next substitution. If we don't find any possible
                // substitution, we're in trouble
                for (SootClass sClass : classes)
                    if (substituteClasses.contains(sClass.toString())) {
                        Local cons = generateClassConstructor(sClass, body, constructionStack, parentClasses);
                        if (cons == null)
                            continue;
                        return cons;
                    }
                logger.warn("Cannot create valid constructor for {}, because it is {} and cannot substitute with subclass", createdClass,
                        (createdClass.isInterface() ? "an interface" : (createdClass.isAbstract() ? "abstract" : "")));
                this.failedClasses.add(createdClass);
                return null;
            } else {
                logger.warn("Cannot create valid constructor for {}, because it is {} and cannot substitute with subclass", createdClass,
                        (createdClass.isInterface() ? "an interface" : (createdClass.isAbstract() ? "abstract" : "")));
                this.failedClasses.add(createdClass);
                return null;
            }
        } else {
            // Find a constructor we can invoke. We do this first as we don't want
            // to change anything in our method body if we cannot create a class
            // instance anyway.
            for (SootMethod currentMethod : createdClass.getMethods()) {
                if (currentMethod.isPrivate() || !currentMethod.isConstructor())
                    continue;

                List<Value> params = new LinkedList<Value>();
                for (Type type : currentMethod.getParameterTypes()) {
                    // We need to check whether we have a reference to the
                    // outer class. In this case, we do not generate a new
                    // instance, but use the one we already have.
                    String typeName = type.toString().replaceAll("\\[\\]]", "");
                    if (type instanceof RefType
                            && (isInnerClass && typeName.equals(outerClass))
                            && this.localVarsForClasses.containsKey(typeName))
                        params.add(this.localVarsForClasses.get(typeName));

                        //FIXME: adapted code here to reuse initialized classes
                    else if (this.dummyThreads.contains(createdClass)) {
                        params.add(this.localVarsForClasses.get(typeName));
                    } else {
                        params.add(getValueForType(body, generator, type, constructionStack, parentClasses));
                    }
                }

                // Build the "new" expression
                NewExpr newExpr = Jimple.v().newNewExpr(RefType.v(createdClass));
                Local tempLocal = generator.generateLocal(RefType.v(createdClass));
                AssignStmt assignStmt = Jimple.v().newAssignStmt(tempLocal, newExpr);
                body.getUnits().add(assignStmt);

                // Create the constructor invocation
                InvokeExpr vInvokeExpr;
                if (params.isEmpty() || params.contains(null))
                    vInvokeExpr = Jimple.v().newSpecialInvokeExpr(tempLocal, currentMethod.makeRef());
                else
                    vInvokeExpr = Jimple.v().newSpecialInvokeExpr(tempLocal, currentMethod.makeRef(), params);

                // Make sure to store return values
                if (!(currentMethod.getReturnType() instanceof VoidType)) {
                    Local possibleReturn = generator.generateLocal(currentMethod.getReturnType());
                    AssignStmt assignStmt2 = Jimple.v().newAssignStmt(possibleReturn, vInvokeExpr);
                    body.getUnits().add(assignStmt2);
                } else
                    body.getUnits().add(Jimple.v().newInvokeStmt(vInvokeExpr));

                return tempLocal;
            }

            logger.warn("Could not find a suitable constructor for class {}", createdClass.getName());
            this.failedClasses.add(createdClass);
            return null;
        }
    }
}
