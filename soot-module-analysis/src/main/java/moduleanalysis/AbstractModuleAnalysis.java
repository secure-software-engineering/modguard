package moduleanalysis;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import moduleanalysis.criticalfinder.ICriticalFinder;
import moduleanalysis.utils.AnalysisReport;
import moduleanalysis.utils.ModuleAnalysisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.boxing.transformation.BoxingTransformerUtility;
import soot.jimple.ArrayRef;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.spark.pag.SparkField;
import soot.jimple.spark.sets.EqualsSupportingPointsToSet;
import soot.jimple.spark.sets.PointsToSetEqualsWrapper;
import soot.options.Options;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by adann on 12.10.17.
 */
public abstract class AbstractModuleAnalysis {
    protected final String modulePath;
    final Logger logger = LoggerFactory.getLogger(getClass());
    private final ICriticalFinder finder;
    final boolean verbose;
    protected final String moduleName;
    //FIXME: make nice results
    protected final Collection<AnalysisReport> identifiedViolationFields = new ArrayList<>();
    protected final Collection<AnalysisReport> identifiedViolationMethods = new ArrayList<>();
    protected final Collection<AnalysisReport> identifiedViolationClasses = new ArrayList<>();


    //TODO: currently for JDK Collections/MAps are ignored
    protected boolean ignoreCollection = true;
    private final int moduleAccessCheckMode;
    private String appendToClassPath;
    private List<String> excludes;
    protected Path logPath;
    protected BufferedWriter printStream = null;
    protected ModuleAnalysisUtils utils = null;

    protected boolean useDoopReflection = false;


    private boolean doBoxing;

    protected boolean onlyApplicationClasses;

    protected Set<SparkField> criticalFields;
    protected Set<SootClass> criticalClasses;
    protected Set<SootMethod> criticalMethods;

    public Collection<AnalysisReport> getIdentifiedViolationFields() {
        return identifiedViolationFields;
    }

    public Collection<AnalysisReport> getIdentifiedViolationFieldsFiltered() {
        HashSet<AnalysisReport> reportHashSetFields = new HashSet<>(identifiedViolationFields);
        return reportHashSetFields;
    }


    public Collection<AnalysisReport> getIdentifiedViolationMethods() {
        return identifiedViolationMethods;
    }


    public Collection<AnalysisReport> getIdentifiedViolationMethodsFiltered() {
        HashSet<AnalysisReport> reportHashSetMethods = new HashSet<>(identifiedViolationMethods);

        return reportHashSetMethods;
    }


    public Collection<AnalysisReport> getIdentifiedViolationClasses() {
        return identifiedViolationClasses;
    }

    public Collection<AnalysisReport> getIdentifiedViolationClassesFiltered() {
        HashSet<AnalysisReport> reportHashSetClasses = new HashSet<>(identifiedViolationClasses);

        return reportHashSetClasses;
    }


    protected AbstractModuleAnalysis(ModuleAnalysisBuilder builder) {
        this.verbose = builder.verbose;
        this.finder = builder.finder;
        this.appendToClassPath = builder.classPath;
        utils = null;
        this.doBoxing = builder.boxingEnabled;
        this.excludes = builder.excludes;
        this.moduleAccessCheckMode = builder.moduleAccessMode;
        this.modulePath = builder.modulePath;
        this.moduleName = builder.moduleName;
        this.onlyApplicationClasses = builder.onlyApplicationClasses;
        this.ignoreCollection = builder.ignoreCollectionArrays;

        this.useDoopReflection = builder.useDoopReflection;

        if (builder.logPath != null) {
            try {
                logPath = Paths.get(builder.logPath, moduleName);
                if (!Files.exists(logPath)) {
                    Files.createDirectories(logPath);
                }
                Path logFile = Paths.get(builder.logPath, moduleName, moduleName + "_result.log");

                Charset charset = Charset.defaultCharset();
                if (!Files.exists(logFile)) {
                    try {
                        Files.createFile(logFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                printStream = Files.newBufferedWriter(logFile, charset);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void print(String msg) {
        System.out.println(msg);
        if (printStream != null)
            try {
                printStream.write(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public Collection<AnalysisReport> getIdentifiedViolations() {
        ArrayList<AnalysisReport> violations = new ArrayList<>();
        violations.addAll(identifiedViolationMethods);
        violations.addAll(identifiedViolationFields);
        violations.addAll(identifiedViolationClasses);
        return violations;


    }


    public Collection<AnalysisReport> getFilteredIdentifiedViolations() {
        ArrayList<AnalysisReport> reports = new ArrayList<>();
        reports.addAll(getIdentifiedViolationMethodsFiltered());
        reports.addAll(getIdentifiedViolationFieldsFiltered());
        reports.addAll(getIdentifiedViolationClassesFiltered());
        return reports;

    }

    /**
     * loads the module-info.class
     * and loads the classes
     */
    private void loadModuleAndClasses(String modulePath) {
        //first we have to load all module-info files in the modulepath to build up
        //the module graph, which we use to resolve dependencies/references
        //  Scene.v().loadModuleInformation(modulePath);

        //now we can load the classes of the module as application classes
        Map<String, List<String>> map = ModulePathSourceLocator.v().getClassUnderModulePath(modulePath);
        for (String module : map.keySet()) {
            for (String klass : map.get(module)) {
                logger.info("Loaded Class: " + klass + "\n");
                ModuleAnalysisUtils.loadClass(klass, false, module);

            }
            SootModuleResolver.v().resolveClass(SootModuleInfo.MODULE_INFO, SootClass.BODIES, Optional.of(module));

        }


        //this must be called after all classes are resolved; because it sets doneResolving in Scene
        Scene.v().loadNecessaryClasses();
    }

    protected void runBoxTransformationFor(Collection<SootClass> classes) {
        if (!this.doBoxing)
            return;
        for (SootClass klass : classes) {
            if (klass.isPhantom())
                continue;
            for (int i = 0; i < klass.getMethods().size(); i++) {
                SootMethod method = klass.getMethods().get(i);
                if (method.isConcrete()) {
                    if (!method.hasActiveBody()) {
                        method.retrieveActiveBody();
                    }
                } else {
                    //FIXME: currently dirty workaround
                    BoxingTransformerUtility.adaptMethodSignature(method);
                }

            }
        }
    }

    protected void setupAnalysis() {
        setUpSoot();
        finder.initialize();

        loadModuleAndClasses(this.modulePath);

        //at first compute the allowed exported packages
        utils = new ModuleAnalysisUtils(this.moduleName, this.moduleAccessCheckMode);

        //collect critical fields, classes methods

        criticalFields = finder.getCriticalFields();
        criticalMethods = finder.getCriticalMethods();
        criticalClasses = finder.getCriticalClasses();

    }

    public abstract void doAnalysis() throws IOException;

    protected void setUpSoot() {
        print("[Soot] Setting Up Soot ... \n");
        Options.v().set_throw_analysis(Options.throw_analysis_unit);
        Options.v().set_include_all(true);
        Options.v().set_asm_backend(true);
        Options.v().set_debug(verbose);
        Options.v().set_debug_resolver(verbose);
        Options.v().set_prepend_classpath(true);
        Options.v().set_keep_line_number(true);
        Options.v().set_whole_program(true);

        Options.v().setPhaseOption("cg", "enabled:" + true);
        Options.v().setPhaseOption("cg.cha", "enabled:" + false);

        Options.v().setPhaseOption("cg.spark", "enabled:" + true);


       /* Options.v().setPhaseOption("cg.spark", "ignore-types:false");
        Options.v().setPhaseOption("cg.spark", "force-gc:false");
        Options.v().setPhaseOption("cg.spark", "pre-jimplify:false");
        Options.v().setPhaseOption("cg.spark", "vta:false");
        Options.v().setPhaseOption("cg.spark", "rta:false");
        Options.v().setPhaseOption("cg.spark", "field-based:false");
        Options.v().setPhaseOption("cg.spark", "types-for-sites:false");
        Options.v().setPhaseOption("cg.spark", "merge-stringbuffer:true");
        Options.v().setPhaseOption("cg.spark", "string-constants:false");
        Options.v().setPhaseOption("cg.spark", "simulate-natives:true");
        Options.v().setPhaseOption("cg.spark", "ple-edges-bidirectional:false");
        Options.v().setPhaseOption("cg.spark", "on-fly-cg:true");

        Options.v().setPhaseOption("cg.spark", "simplify-offline:false");
        Options.v().setPhaseOption("cg.spark", "simplify-sccs:false");
        Options.v().setPhaseOption("cg.spark", "ignore-types-for-sccs:false");
        Options.v().setPhaseOption("cg.spark", "propagator:worklist");
        Options.v().setPhaseOption("cg.spark", "set-impl:double");
        Options.v().setPhaseOption("cg.spark", "double-set-old:hybrid");
        Options.v().setPhaseOption("cg.spark", "double-set-new:hybrid");
        Options.v().setPhaseOption("cg.spark", "dump-html:false");
        Options.v().setPhaseOption("cg.spark", "dump-pag:false");
        Options.v().setPhaseOption("cg.spark", "dump-solution:false");
        Options.v().setPhaseOption("cg.spark", "topo-sort:false");
        Options.v().setPhaseOption("cg.spark", "dump-types:true");
        Options.v().setPhaseOption("cg.spark", "class-method-var:true");
        Options.v().setPhaseOption("cg.spark", "dump-answer:false");
        Options.v().setPhaseOption("cg.spark", "add-tags:false");
        Options.v().setPhaseOption("cg.spark", "set-mass:false");
        Options.v().setPhaseOption("cg.spark","lazy-pts:false");*/


        Options.v().setPhaseOption("cg", "verbose:" + verbose);
        Options.v().setPhaseOption("cg", "safe-forname:" + false);
        if (excludes != null) {
            Options.v().set_exclude(excludes);
            Options.v().set_no_bodies_for_excluded(true);
            Options.v().set_allow_phantom_refs(true);
        }
        //FIXME: deactivated for Tomcat because currently unnecessary
        if (this.doBoxing) {
            Options.v().set_use_boxing(true);
        }
        //TODO: added module-path option here ---!!!!
        if (!this.appendToClassPath.isEmpty() && !this.appendToClassPath.startsWith(":")) {
            appendToClassPath = ":" + appendToClassPath;
        }
        Options.v().set_soot_modulepath(modulePath + appendToClassPath);


    }

    //FIXME: add to module Analysis Results
    //FIXME: what to do with constructors, since there are fields always modified
    //check if accessible methods manipulate critical allocNodes
    private void checkIfCriticalFieldIsModifiedInMethod(Collection<SootMethod> accessibleMethods, Collection<SparkField> criticalFields) {


        //check the PTS of the locals of these methods
        for (SootMethod method : accessibleMethods) {
            if (!method.hasActiveBody()) {
                System.err.println("Method is accessible but has no active body???");
                continue;
            }
            // check if a local to a field exists, if yes cool, else not necessary to analyse the method at all?
            //TODO: do I have to look if local pts --> critical Field?
            for (Unit unit : method.getActiveBody().getUnits()) {
                if (unit instanceof DefinitionStmt) {
                    Value leftSide = ((DefinitionStmt) unit).getLeftOp();
                    if (leftSide instanceof ArrayRef) {
                        Value value = ((ArrayRef) leftSide).getBase();
                        if (value instanceof FieldRef) {
                            leftSide = value;
                        }
                    }

                    if (leftSide instanceof FieldRef) {
                        if (criticalFields.contains(((FieldRef) leftSide).getField())) {
                            print("Critical Field " + ((FieldRef) leftSide).getField() + " is modified in " + method.getName() + "\n");
                        }
                    }

                }
            }


        }


    }

    /**
     * Checks if a critical method is accessible and returns all accessible methods
     *
     * @param criticalMethods the critical Methods if null or empty, then all
     * @param accessibleTypes the Types accessible outside of the module
     * @return the accessible methods (outside of the module) from the accessible types
     */
    private Collection<SootMethod> checkIfCriticalMethodsIsAccessible(Set<SootMethod> criticalMethods, Collection<Type> accessibleTypes) {
        ArrayList<SootMethod> accessibleMethods = new ArrayList<>();
        boolean checkMethods = !(criticalMethods == null || criticalMethods.isEmpty());
        Hierarchy hierarchy = Scene.v().getActiveHierarchy();

        for (Type type : accessibleTypes) {
            if (type instanceof RefType) {
                SootClass klass = ((RefType) type).getSootClass();
                for (SootMethod method : klass.getMethods()) {
                    if (utils.isMethodAccessible(method)) {
                        accessibleMethods.add(method);
                        if (checkMethods) {

                            Collection<SootClass> classHierarchy;
                            if (klass.isInterface()) {
                                classHierarchy = new HashSet<>(klass.getInterfaces());
                                classHierarchy.add(klass);
                            } else {//the specical case here is needed, because soot dont wants to give the hierachy for interfaces
                                classHierarchy = hierarchy.getSuperclassesOfIncluding(klass);
                            }


                            /* okay, the method is accessible
                             * now we, have to check if this is an overridden, implementation
                             * of an interface, abstract method that has been marked as critical
                             */
                            for (SootClass c : classHierarchy) {
                                SootMethod sm = c.getMethodUnsafe(method.getSubSignature());
                                if (sm != null && criticalMethods.contains(sm)) {
                                    String message = "Critical Method: " + method.getSignature() + " is accessible in Class:" + c.getName() + " \n";
                                    print(message);
                                    AnalysisReport report = new AnalysisReport(message, null, method);
                                    identifiedViolationMethods.add(report);
                                    continue;
                                }
                                for (SootClass cInterface : c.getInterfaces()) {
                                    SootMethod smInter = cInterface.getMethodUnsafe(method.getSubSignature());
                                    if (smInter != null && criticalMethods.contains(smInter)) {
                                        String message = "Critical Method: " + method.getSignature() + " is accessible in Class:" + c.getName() + " \n";
                                        print(message);
                                        AnalysisReport report = new AnalysisReport(message, null, method);

                                        identifiedViolationMethods.add(report);
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
        return accessibleMethods;

      /*  SootCallGraphAdapter sootCallGraphAdapter = new SootCallGraphAdapter(cg);

        DijkstraShortestPath dijkstraShortestPath = new DijkstraShortestPath(sootCallGraphAdapter);
        ShortestPathAlgorithm.SingleSourcePaths singleSourcePaths = dijkstraShortestPath.getPaths(generatedDummyMain);
        for (SootMethod criticalMethod : criticalMethods) {
            GraphPath path = singleSourcePaths.getPath(criticalMethod);
            if (path != null && path.getLength() != 0) {
                System.out.println("Escape Method");
            }
        }*/


    }

    private void checkIfCriticalClassIsAccessible(Set<SootClass> criticalClasses, HashSet<Type> accessibleTypes) {
        /* add code for abstract class/ interface class to
         * check if accessibleType contains/implements/Extends criticalClass
         */
        if (criticalClasses == null || criticalClasses.isEmpty()) {
            return;
        }
        Hierarchy hierarchy = Scene.v().getActiveHierarchy();

        // iterate through accessibleTypes
        for (Type accessibeType : accessibleTypes) {
            if (!(accessibeType instanceof RefType)) {
                continue;
            }
            SootClass accessibleKlass = ((RefType) accessibeType).getSootClass();
            Collection<SootClass> classHierarchy;
            if (accessibleKlass.isInterface()) {
                classHierarchy = new HashSet<>(accessibleKlass.getInterfaces());
                classHierarchy.add(accessibleKlass);
            } else {//the specical case here is needed, because soot dont wants to give the hierachy for interfaces
                classHierarchy = hierarchy.getSuperclassesOfIncluding(accessibleKlass);
            }

            for (SootClass c : classHierarchy) {
                if (criticalClasses.contains(c)) {
                    String message = "Critical Class " + c + "  is accessible in form of Class: " + accessibleKlass + "\n";
                    print(message);
                    AnalysisReport report = new AnalysisReport(message, null, c);
                    identifiedViolationClasses.add(report);
                    continue;
                }
                for (SootClass cInterface : c.getInterfaces()) {
                    if (criticalClasses.contains(cInterface)) {
                        String message = "Critical Interface " + cInterface + "  is accessible in form of Class: " + accessibleKlass + "\n";
                        print(message);
                        AnalysisReport report = new AnalysisReport(message, null, cInterface);
                        identifiedViolationClasses.add(report);
                    }
                }
            }

        }
        //or add all implements of critical classes to critical classes
    }

    private void checkForCriticalFields(Collection<AnalysisReport> criticalFieldsPTS, HashSet<PointsToSetEqualsWrapper> accesiblePTS) {

        //check if critical allocNodes maybecome reachable
        for (PointsToSetEqualsWrapper pts : accesiblePTS) {
            EqualsSupportingPointsToSet equalsSupportingPointsToSet = pts.unwarp();
            for (AnalysisReport report : criticalFieldsPTS) {
                PointsToSet fieldsPTS = report.getPts();
                if (equalsSupportingPointsToSet.hasNonEmptyIntersection(fieldsPTS)) {
                    String message = "Escape Field: " + report.getSootObject().toString() + " via: " + fieldsPTS.toString() + "\n";
                    print(message);
                    report.setMessage(message);
                    identifiedViolationFields.add(report);
                }
            }
        }

    }


    protected void collecectAcessibleCriticalObject(Collection<AnalysisReport> criticalFieldsPTS, HashSet<PointsToSetEqualsWrapper> accessiblePTS) {

        Stopwatch stopwatch = Stopwatch.createStarted();
        print("[Analysis] ========= Checking Violations of Critical Entities ============ \n");


        //check fields
        checkForCriticalFields(criticalFieldsPTS, accessiblePTS);

        //FIXME: check what happens to ArrayTypes
        HashSet<Type> accessibleTypes = new HashSet<>();
        //add to accessible types
        for (PointsToSetEqualsWrapper pts : accessiblePTS) {
            accessibleTypes.addAll(pts.possibleTypes().stream().filter(RefLikeType.class::isInstance).collect(Collectors.toList()));
        }


        //check classes
        checkIfCriticalClassIsAccessible(criticalClasses, accessibleTypes);

        //check methods
        // returns all accessible methods, since they are checked here anyway
        Collection<SootMethod> accessibleMethods = checkIfCriticalMethodsIsAccessible(criticalMethods, accessibleTypes);


        //check modification of critical fields in methods
        //should be returned by checkIfCriticalMethodsIsAccessible
        // since there all methods are checked if they are accessible
        checkIfCriticalFieldIsModifiedInMethod(accessibleMethods, criticalFields);
        print("[Analysis] ========= Completed Checking Violations of Critical Entities: " + stopwatch.toString() + " ============ \n");

    }
}
