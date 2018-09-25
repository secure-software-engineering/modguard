package moduleanalysis;


import com.google.common.base.Stopwatch;
import exception.analysis.ExceptionAnalysis;
import heros.solver.CountingThreadPoolExecutor;
import moduleanalysis.entrypoints.ModuleEntryPointCreator;
import moduleanalysis.utils.AnalysisReport;
import moduleanalysis.utils.doop.PTSQueryAdapter;
import moduleanalysis.utils.doop.PTSQueryObject;
import org.apache.commons.io.FileUtils;
import org.clyze.analysis.AnalysisOption;
import org.clyze.doop.core.*;
import org.clyze.utils.Helper;
import soot.*;
import soot.jimple.spark.pag.SparkField;
import soot.jimple.spark.sets.*;
import soot.options.Options;
import soot.toolkits.scalar.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

//import soot.boxing.transformation.BoxingTransformerUtility;


/**
 * Created by adann on 24.08.16.
 * <p>
 * This class represents the module escape analysis
 */
public class DoopModuleAnalysis extends AbstractModuleAnalysis {


    protected DoopModuleAnalysis(ModuleAnalysisBuilder builder) {
        super(builder);
    }


    public void doAnalysis() throws IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        //Reset Soot
        G.reset();
        PTSQueryAdapter.DoopNode.nodeLoadingCache.invalidateAll();
        //Reset Exception
        ExceptionAnalysis.reset();

        print("[Analysis] START Escape Analysis of " + moduleName + " in " + modulePath + "\n");


        print("[Setup] Start " + "\n");

        setupAnalysis();

        //FIXME: hack since boxing is not ready for multi threading
        // before generating the entrypoints, we have to lift their signature
        // by using retrieveActiveBody() on the entrypoints methods
        //transform all application classes first, to generate reasonable, entrypoints
        runBoxTransformationFor(Scene.v().getApplicationClasses());


        //collect critical fields, classes methods
      /*  Set<SparkField> criticalFields = finder.getCriticalFields();
        Set<SootMethod> criticalMethods = finder.getCriticalMethods();
        Set<SootClass> criticalClasses = finder.getCriticalClasses();*/


        if (utils.getAllowedExportedClasses().size() == 0) {
            throw new RuntimeException("There exists no exported packages, thus no entry-points can be computed!");
        }

        print("[Setup] Complete: " + stopwatch.toString() + "\n");


        ModuleEntryPointCreator entryPointCreator = new ModuleEntryPointCreator(new HashSet<>(), utils);

        //generate dummyMainClasses for abstract classes
        Iterator<SootClass> classIterator = Scene.v().getClasses().snapshotIterator();
        while (classIterator.hasNext()) {
            SootClass cl = (SootClass) classIterator.next();
            // note: the following is a snapshot iterator;
            // this is necessary because it can happen that phantom methods
            // are added during resolution
            //FIXME: added case for module info (not required module-info files)
            if (cl.resolvingLevel() < SootClass.SIGNATURES)
                continue;

            if ((cl.isInterface() || cl.isAbstract()) && cl.isExportedByModule() && !cl.isFinal() && cl.isPublic()) {
                if (cl.moduleName.equals(this.moduleName)) {
                    //generate dummy class
                    entryPointCreator.getDummyClass(cl);
                }
            }

        }


        stopwatch.reset().start();
        print("[Doop] Started");
        // DoopModuleAnalysis.retrieveAllSceneClassesBodies();


        //Doop uses a lot of memory, thus we have to clean the old cache
        String doopHome = System.getenv("DOOP_HOME");
        String cache = doopHome + File.separator + "cache";
        String outDir = doopHome + File.separator + "out";
        File cacheFolder = new File(cache);
        File outFolder = new File(outDir);
        FileUtils.cleanDirectory(cacheFolder);
        FileUtils.cleanDirectory(outFolder);


        //run doop here

        //   String args[] = new String[]{"--reuseclasses", "--modulename","\"de.upb.mod2\"",   "--modulemode", "-a", "context-insensitive", "-i", "/home/adann/IdeaProjects/jdk_escape_analysis/test_cases/main/mod.jar", "--ignore-main-method", "--open-programs-context-insensitive-entrypoints", "--lb"};
        //    org.clyze.doop.Main.main(args);

        DoopAnalysis analysis = setUpDoop();


        analysis.run();


        print("[Doop] Complete: " + stopwatch.toString() + "\n");


        stopwatch.reset().start();

        print("[Collecting Accessible PTS] Start" + "\n");

        PTSQueryAdapter adapter = new PTSQueryAdapter();
        //  adapter.doopAnalysis = org.clyze.doop.Main.getAnalysis();
        adapter.doopAnalysis = analysis;

        HashSet<PointsToSetEqualsWrapper> accessiblePTS = this.collectReachablePTS(adapter);


        print("[Collecting Accessible PTS] Complete  " + stopwatch.toString() + "\n");


        //FIXME: get the PTS of the main and dummy classes

        ArrayList<AnalysisReport> criticalFieldsPTS = new ArrayList<>();
        ArrayList<PTSQueryObject> queries = new ArrayList<>();
        for (SparkField field : criticalFields) {
            if (field instanceof SootField) {
                PTSQueryObject queryObject;
                if (((SootField) field).isStatic()) {

                    queryObject = adapter.getStaticFieldPTS((SootField) field);
                } else {
                    queryObject = adapter.getInstanceFieldPTS((SootField) field);

                }
                queries.add(queryObject);
            }
        }
        //execute the queries
        PTSQueryObject.execute(queries, analysis);

        for (PTSQueryObject query : queries) {
            AnalysisReport report;
            report = new AnalysisReport(null, query.getPts(), query.getSootObject());
            criticalFieldsPTS.add(report);
        }


        stopwatch.reset().start();

        print("[Check Violations] Start");


        //printout the collection PTS
        super.collecectAcessibleCriticalObject(criticalFieldsPTS, accessiblePTS);


        print("[Check Violations] Complete: " + stopwatch.toString() + "\n");

       /* print("========= Collection Critical Objects ============\n");


        //check fields
        checkForCriticalFields(criticalFieldsPTS, accessiblePTS);

        //FIXME: check what happens to ArrayTypes
        HashSet<Type> accessibleTypes = new HashSet<>();
        //add to accessible types
        for (PointsToSetEqualsWrapper pts : accessiblePTS) {
            accessibleTypes.addAll(pts.possibleTypes().stream().filter(RefLikeType.class::isInstance).collect(Collectors.toList()));
        }


        s

        //check classes
        checkIfCriticalClassIsAccessible(criticalClasses, accessibleTypes);

        //check methods
        // returns all accessible methods, since they are checked here anyway
        Collection<SootMethod> accessibleMethods = checkIfCriticalMethodsIsAccessible(criticalMethods, accessibleTypes);


        //check modification of critical fields in methods
        //should be returned by checkIfCriticalMethodsIsAccessible
        // since there all methods are checked if they are accessible
        checkIfCriticalFieldIsModifiedInMethod(accessibleMethods, criticalFields);
*/


        print("END");
        printStream.close();


    }


    private HashSet<PointsToSetEqualsWrapper> collectReachablePTS(PTSQueryAdapter ptsQueryAdapter) {
        //Collection of Types that escape a Module
        HashSet<PointsToSetEqualsWrapper> accessiblePTS = new HashSet<>();
        /*
          Start collecting the reachable PTS
         */

        Stopwatch stopwatch = Stopwatch.createStarted();

        ///TODO: collect Any_Subtype_Nodes

        /*
          start looking for escaped objects

         */

        print("[Start] Computing PTS for Locals \n");

        //phase 1: get  Points-To-Set for the collected Locals (in DummyMainClass and the generated DummyClasses)
        //add the initial points to set of these Locals  to accessiblePTS
        Collection<PointsToSet> pointsToSets = ptsQueryAdapter.getPTSofMain();

        for (PointsToSet pts : pointsToSets) {
            addPointsToSetEqualsWrapperToCollection(accessiblePTS, pts, accessiblePTS);
        }

        print("[End] Computing PTS for Locals " + stopwatch.toString() + "\n");


        stopwatch.reset().start();


        Collection<PointsToSetEqualsWrapper> collectionMapPts = new HashSet<>();


        //phase 2:  get the exceptions that propagate to the public accessible Module's methods


        //phase 2.3:  get pts of abstract klasses
       /* for (SootClass abstractKlass : abstractKlasses) {

            PointsToSet ret = ((PAG) pta).getSetFactory().newSet(abstractKlass.getType(), ((PAG) pta));
            if (ret != null) {
                addPointsToSetEqualsWrapperToCollection(accessiblePTS, ret, accessiblePTS);
            }
        }*/
        print("[Start] Computing PTS for Fields \n");


        // add cache for already considered types and theory PTS
        HashSet<Pair<Type, PointsToSet>> visitedType = new HashSet<>();
        // Phase 3:get iteratively the fields of escaping Objects, and add their PointsToSet to the accessiblePTS
        Stack<PointsToSetEqualsWrapper> escapingPointsToKeys = new Stack<>();
        Set<PTSQueryObject> ptsQueryObjectSet = new HashSet<>();
        int numberOfClasses = Scene.v().getClasses().size();
        escapingPointsToKeys.addAll(accessiblePTS);
        while (!escapingPointsToKeys.isEmpty()) {
            PointsToSetEqualsWrapper pts = escapingPointsToKeys.pop();

            if (pts.possibleTypes().size() >= numberOfClasses) {
                logger.warn("!!![WARNING] pts with Everything!!!");
                continue;
            }
            for (Type type : pts.possibleTypes()) {
                if (type instanceof RefLikeType) {
                    Pair<Type, PointsToSet> pair = new Pair<>(type, pts);
                    if (!visitedType.add(pair)) {
                        continue;
                    }


                    PTSQueryObject ptsQueryObject = null;
                    if (type instanceof ArrayType) {
                        //FIXME: adapt here
                        System.out.println("Reachable array");
                        //newPts = pta.reachingObjectsOfArrayElement(pts.unwarp());

                        ptsQueryObject = ptsQueryAdapter.getPTSOfArrayIndex((PTSQueryAdapter.DoopPTS) pts.unwarp());

                        // addPointsToSetEqualsWrapperToCollection(newEscapingPointsToKeys, newPts, accessiblePTS);
                        ptsQueryObjectSet.add(ptsQueryObject);

                    } else if (type instanceof RefType) {

                        SootClass klass = ((RefType) type).getSootClass();


                            /*workaround to deal with collection objects for further info, see
                              {@link #ptsForCollection(SootClass, PointsToSet, PointsToAnalysis, CallGraph)}

                             */
                        if (klass.implementsInterface("java.util.Collection")) {
                            if (this.ignoreCollection) {
                                continue;

                            }


                            //TODO: current shortcut to distinguish between collection vs. normal points to set
                            // addPointsToSetEqualsWrapperToCollection(collectionMapPts, ptsQueryObject, accessiblePTS);
                            ptsQueryObjectSet.add(ptsQueryObject);

                        } else if (klass.implementsInterface("java.util.Map")) {
                            if (this.ignoreCollection) {
                                continue;

                            }


                            // addPointsToSetEqualsWrapperToCollection(collectionMapPts, ptsQueryObject, accessiblePTS);
                            ptsQueryObjectSet.add(ptsQueryObject);

                        } else {
                            //in fact du to the rules of the module system we don't have access to fields of internal classes
                            //even if we "try" to use reflection
                            if (!utils.isClassAccessible(klass)) {
                                continue;
                            }

                            //here we also include the fields of accessible superclasses
                            // since if the superclass is exported its field are accessible from outside, to
                            Collection<SootField> fields = utils.getAccessibleFieldsIncludingSuperClass(klass);
                            for (SootField field : fields) {

                                //look for the static fields
                                if (field.isStatic()) {
                                    ptsQueryObject = ptsQueryAdapter.getStaticFieldPTS(field);
                                } else if (!field.isStatic()) {
                                    //look for instance fields
                                    //FIXME: soot uses context here //the base object
                                    //newPts = pta.reachingObjects(pts.unwarp(), field);
                                    ptsQueryObject = ptsQueryAdapter.getInstanceFieldPTS(field);
                                }

                                // addPointsToSetEqualsWrapperToCollection(newEscapingPointsToKeys, ptsQueryObject, accessiblePTS);
                                ptsQueryObjectSet.add(ptsQueryObject);

                            }
                            //ptsQueryObject = EmptyPointsToSet.v();
                            ptsQueryObject = null;
                        }
                    }

                }
            }
            if (escapingPointsToKeys.isEmpty() && !ptsQueryObjectSet.isEmpty()) {

                PTSQueryObject.execute(ptsQueryObjectSet, ptsQueryAdapter.doopAnalysis);

                for (PTSQueryObject queryObject : ptsQueryObjectSet) {
                    //run the queries and add to the stack
                    PointsToSet newPTS = queryObject.getPts();
                    addPointsToSetEqualsWrapperToCollection(accessiblePTS, newPTS, accessiblePTS);
                    addPointsToSetEqualsWrapperToCollection(escapingPointsToKeys, newPTS, accessiblePTS);
                }

            }

        }
        print("[End] Computing PTS for Fields " + stopwatch.toString() + "\n");

        //clean cache afterwards
        return accessiblePTS;

    }


    private boolean PTSIsIllegal(PointsToSet pts) {

        return false;
    }


    /**
     * Adds newPTs to the collection if its not already contained in the referenceSet
     *
     * @param collectionToAddTo
     * @param newPts
     * @param referenceSet
     * @param collectionToAddTo
     */
    private void addPointsToSetEqualsWrapperToCollection(Collection<PointsToSetEqualsWrapper> collectionToAddTo, PointsToSet newPts, HashSet<? extends PointsToSet> referenceSet) {
        if (PTSIsIllegal(newPts)) {
            return;
        }
        PointsToSetEqualsWrapper wrapper = new PointsToSetEqualsWrapper((EqualsSupportingPointsToSet) newPts);
        if (!referenceSet.contains(wrapper)) {
            collectionToAddTo.add(wrapper);
        }
    }


    private DoopAnalysis setUpDoop() {

        Doop.initDoop(System.getenv("DOOP_HOME"), System.getenv("DOOP_OUT"), System.getenv("DOOP_CACHE"));
        try {
            Helper.initLogging("INFO", Doop.getDoopHome() + "/build/logs", true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ReuseSceneDoopAnalysisFactory doopAnalysisFactory = new ReuseSceneDoopAnalysisFactory();
        Map<String, AnalysisOption> options = new HashMap<>();
        for (AnalysisOption option : DoopAnalysisFamily.instance.supportedOptions()) {
            try {
                options.put(option.getId(), option.clone());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
        String name = "context-insensitive";


        for (AnalysisOption option : options.values()) {
            String optionName = option.getName();
            if (optionName == null)
                continue;
            switch (optionName) {
                case "modulename":
                    option.setValue(moduleName);
                    break;
                case "modulemode":
                    option.setValue(true);
                    break;
                case "reuseclasses":
                    option.setValue(true);
                    break;
                case "ignore-main-method":
                    option.setValue(true);
                    break;
                case "open-programs-context-insensitive-entrypoints":
                    option.setValue(true);
                    break;
                case "lb":
                    option.setValue(true);
                    break;
                case "analysis":
                    option.setValue(name);
                    break;
                case "featherweight-analysis":
                    //   option.setValue(true);
                    break;
                case "only-application-classes-fact-gen":
                    option.setValue(this.onlyApplicationClasses);
                    break;
                case "reflection-classic":
                    option.setValue(true);
                    break;
                default:
                    break;
            }
        }

        return doopAnalysisFactory.newAnalysis(DoopAnalysisFamily.instance, null, name, options);
    }

    @Override
    protected void setUpSoot() {
        super.setUpSoot();
        Options.v().set_via_shimple(true);
    }


    public static void retrieveAllSceneClassesBodies() {
        // The old coffi front-end is not thread-safe
        int threadNum = Options.v().coffi() ? 1 : Runtime.getRuntime().availableProcessors();
        CountingThreadPoolExecutor executor = new CountingThreadPoolExecutor(threadNum, threadNum, 30, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());

        Iterator<SootClass> clIt = Scene.v().getClasses().snapshotIterator();
        while (clIt.hasNext()) {
            SootClass cl = (SootClass) clIt.next();
            // note: the following is a snapshot iterator;
            // this is necessary because it can happen that phantom methods
            // are added during resolution
            //FIXME: added case for module info (not required module-info files)
            if (cl.resolvingLevel() < SootClass.SIGNATURES)
                continue;

            Iterator<SootMethod> methodIt = cl.getMethods().iterator();
            while (methodIt.hasNext()) {
                final SootMethod m = methodIt.next();
                if (m.isConcrete()) {
                    executor.execute(new Runnable() {

                        @Override
                        public void run() {
                            m.retrieveActiveBody();
                        }

                    });
                }
            }
        }

        // Wait till all method bodies have been loaded
        try {
            executor.awaitCompletion();
            executor.shutdown();
        } catch (InterruptedException e) {
            // Something went horribly wrong
            throw new RuntimeException("Could not wait for loader threads to " + "finish: " + e.getMessage(), e);
        }

        // If something went wrong, we tell the world
        if (executor.getException() != null)
            throw (RuntimeException) executor.getException();
    }
}
