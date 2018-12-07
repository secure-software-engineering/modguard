package moduleanalysis;


import com.google.common.base.Stopwatch;
import exception.analysis.ExceptionAnalysis;
import groovy.lang.Closure;
import heros.solver.CountingThreadPoolExecutor;
import moduleanalysis.entrypoints.ModuleEntryPointCreator;
import moduleanalysis.utils.AnalysisReport;
import moduleanalysis.utils.ClassViolationReport;
import moduleanalysis.utils.FieldViolationReport;
import moduleanalysis.utils.MethodViolationReport;
import moduleanalysis.utils.doop.PTSQueryAdapter;
import org.apache.commons.io.FileUtils;
import org.clyze.analysis.AnalysisOption;
import org.clyze.doop.core.Doop;
import org.clyze.doop.core.DoopAnalysis;
import org.clyze.doop.core.DoopAnalysisFamily;
import org.clyze.doop.core.ReuseSceneDoopAnalysisFactory;
import org.clyze.utils.CPreprocessor;
import org.clyze.utils.Helper;
import soot.*;
import soot.jimple.spark.pag.SparkField;
import soot.options.Options;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

//import soot.boxing.transformation.BoxingTransformerUtility;


/**
 * Created by adann on 24.08.16.
 * <p>
 * This class represents the module escape analysis
 */
public class DoopOnlyModuleAnalysis extends AbstractModuleAnalysis {


    private boolean runInformationFlowAnalysis = false;


    protected DoopOnlyModuleAnalysis(ModuleAnalysisBuilder builder) {
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

        String doopHome = System.getenv("DOOP_HOME");


        // get critical fields of primitive type
        Collection<SparkField> primitiveFields = new ArrayList<>();
        for (SparkField field : this.criticalFields) {
            if (field.getType() instanceof PrimType) {
                primitiveFields.add(field);
            }

            if (field.getType() instanceof ArrayType) {
                ArrayType arrayType = (ArrayType) field.getType();
                if (arrayType.baseType instanceof PrimType) {
                    primitiveFields.add(field);
                }
            }

        }


        //we have primitive fields // write them as tainted for doops analysis
        if (!primitiveFields.isEmpty()) {
            this.runInformationFlowAnalysis = true;
            String fileName = "module-sources-and-sinks.logic";
            Path sourceSinkFiles = Paths.get(doopHome, "logic", "addons", "information-flow", fileName);

            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("#include \"common-transfer-methods.logic\" \n");

            String fieldString = "TaintedField(?field) <- Field:Id(?field:\"%s\").\n";

            for (SparkField field : criticalFields) {

                stringBuilder.append(String.format(fieldString, field.toString()));
            }

            Files.write(sourceSinkFiles, stringBuilder.toString().getBytes(), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);


        }


        // before generating the entrypoints, we have to lift their signature
        // by using retrieveActiveBody() on the entrypoints methods
        //transform all application classes first, to generate reasonable, entrypoints
        runBoxTransformationFor(Scene.v().getApplicationClasses());


        if (utils.getAllowedExportedClasses().size() == 0) {
            throw new RuntimeException("There exists no exported packages, thus no entry-points can be computed!");
        }

        print("[Setup] Complete: " + stopwatch.toString() + "\n");

        SootModuleInfo moduleInfo = null;
        java.util.Optional moduleInfoOpt = Scene.v().getApplicationClasses().stream().filter((p) -> p.getName().equals(SootModuleInfo.MODULE_INFO) && p.moduleName.equals(moduleName)).findFirst();
        if (moduleInfoOpt.isPresent())
            moduleInfo = (SootModuleInfo) moduleInfoOpt.get();


        ModuleEntryPointCreator entryPointCreator = new ModuleEntryPointCreator(new HashSet<>(), utils);

        //generate dummyMainClasses for abstract classes
        Iterator<SootClass> classIterator = Scene.v().getClasses().snapshotIterator();
        while (classIterator.hasNext()) {
            SootClass cl = (SootClass) classIterator.next();
            // note: the following is a snapshot iterator;
            // this is necessary because it can happen that phantom methods
            // are added during resolution
            //not required module-info files
            if (cl.resolvingLevel() < SootClass.SIGNATURES)
                continue;

            if ((cl.isInterface() || cl.isAbstract()) && cl.isExportedByModule() && !cl.isFinal() && cl.isPublic()) {
                if (cl.moduleName.equals(this.moduleName)) {
                    //generate dummy class
                    entryPointCreator.getDummyClass(cl, moduleInfo);
                }
            }

        }


        stopwatch.reset().start();
        print("[Doop] Started");
        // DoopModuleAnalysis.retrieveAllSceneClassesBodies();


        //Doop uses a lot of memory, thus we have to clean the old cache
        String cache = doopHome + File.separator + "cache";
        String outDir = doopHome + File.separator + "out";


        File cacheFolder = new File(cache);
        File outFolder = new File(outDir);
        if (!cacheFolder.exists()) {
            Files.createDirectories(Paths.get(cacheFolder.toURI()));
        }
        if (!outFolder.exists()) {
            Files.createDirectories(Paths.get(outFolder.toURI()));
        }

        FileUtils.cleanDirectory(cacheFolder);
        FileUtils.cleanDirectory(outFolder);


        //run doop here

        //   String args[] = new String[]{"--reuseclasses", "--modulename","\"de.upb.mod2\"",   "--modulemode", "-a", "context-insensitive", "-i", "/home/adann/IdeaProjects/jdk_escape_analysis/test_cases/main/mod.jar", "--ignore-main-method", "--open-programs-context-insensitive-entrypoints", "--lb"};
        //    org.clyze.doop.Main.main(args);

        DoopAnalysis analysis = setUpDoop();


        analysis.run();


        print("[Doop] Complete: " + stopwatch.toString() + "\n");


        stopwatch.reset().start();

        print("[Handing Critical Entities to Doop] Start" + "\n");

        PTSQueryAdapter adapter = new PTSQueryAdapter();
        //  adapter.doopAnalysis = org.clyze.doop.Main.getAnalysis();
        adapter.doopAnalysis = analysis;
        File outDirDoop = adapter.doopAnalysis.getOutDir();

        String templateFileName = "moduleAnalysisQueryTemplate.logic";
        String finaleFileName = "moduleAnalysisQuery.logic";

        InputStream queryFileSourceInputStream = null;
        Charset charset = Charset.defaultCharset();
        {
            Path queryFileSource = Paths.get(templateFileName);

            if (!Files.exists(queryFileSource)) {
                //else take the one packaged


                queryFileSourceInputStream = getClass().getResourceAsStream(File.separator + templateFileName);


            } else {
                queryFileSourceInputStream = Files.newInputStream(queryFileSource);

            }
        }

        //get the cpp of doop
        CPreprocessor cpp = null;
        try {
            Field field = null;

            field = DoopAnalysis.class.getDeclaredField("cpp");
            field.setAccessible(true);

            cpp = (CPreprocessor) field.get(analysis);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }


        Path queryFileTarget = Paths.get(outDirDoop.toString(), templateFileName);
        Files.copy(queryFileSourceInputStream, queryFileTarget, StandardCopyOption.REPLACE_EXISTING);


        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\n");

        String fieldString = "_sensitiveField(?field) <-Field:Id(?field:\"%s\").\n";

        for (SparkField field : criticalFields) {

            stringBuilder.append(String.format(fieldString, field.toString()));
        }


        String classString = "_sensitiveType(?type) <-Type:Id(?type:\"%s\").\n";

        for (SootClass cl : criticalClasses) {

            stringBuilder.append(String.format(classString, cl.toString()));
        }

        String methodString = "_sensitiveMethod(?method) <-Method:Id(?method:\"%s\").\n";

        for (SootMethod sootMethod : criticalMethods) {

            stringBuilder.append(String.format(methodString, sootMethod.toString()));
        }

        String contentToAppend = stringBuilder.toString();

        try {
            Files.write(queryFileTarget, contentToAppend.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Problem occurs when deleting the directory : " + queryFileTarget);
            e.printStackTrace();
        }


        Path queryFileFinal = Paths.get(outDirDoop.toString(), finaleFileName);
        String macros = cpp.getMacroCli();
        if (!this.ignoreCollection) {
            macros += " -DARRAYCOL";
        } else {
            print("[Collections] Ignored");

        }
        cpp.setMacroCli(macros);


        //for query use the optin
        cpp.preprocess(queryFileFinal.toAbsolutePath().toString(), queryFileTarget.toAbsolutePath().toString());


        print("[Handing Critical Entities to Doop] Complete  " + stopwatch.toString() + "\n");


        stopwatch.reset().start();

        print("[Check Violations] Start");


        //printout the collection PTS


        Closure myCl = new Closure(null) {
            Violation currentViolation = null;

            @Override
            public Object call(Object... args) {
                String line = (String) args[0];
                //get the QueryObject with the corresponding rule name
                if (line.startsWith("+")) {
                    if (line.contains(Violation.CLASS.getRuleName())) {
                        currentViolation = Violation.CLASS;
                    } else if (line.contains(Violation.METHOD.getRuleName())) {
                        currentViolation = Violation.METHOD;
                    } else if (line.contains(Violation.FIELD.getRuleName())) {
                        currentViolation = Violation.FIELD;
                    } else {
                        currentViolation = null;
                    }
                    return null;
                }

                if (currentViolation == null)
                    return null;
                if (line.isEmpty())
                    return null;

                AnalysisReport report;

                switch (currentViolation) {
                    case CLASS:
                        report = new ClassViolationReport(line);
                        print(line);
                        identifiedViolationClasses.add(report);
                        break;
                    case METHOD:
                        report = new MethodViolationReport(line);
                        print(line);
                        identifiedViolationMethods.add(report);
                        break;
                    case FIELD:
                        report = new FieldViolationReport(line);
                        print(line);
                        identifiedViolationFields.add(report);
                        break;
                    default:
                        break;
                }

                return null;
            }
        };


        String queryToFile = "-file " + queryFileFinal.toAbsolutePath().toString();
        adapter.doopAnalysis.processRelation(queryToFile, myCl);


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


    private DoopAnalysis setUpDoop() {

        if (System.getenv("DOOP_HOME").isEmpty()) {
            System.err.println("[Error] DOOP_HOME is not specified");
        }

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
        //String name = "context-insensitive";
        String name = "context-insensitive-plusplus";

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
                case "information-flow":
                    if (this.runInformationFlowAnalysis) {
                        option.setValue("module");
                    }
                    break;
                case "only-application-classes-fact-gen":
                    option.setValue(this.onlyApplicationClasses);
                    break;
//                case "reflection":
//                    option.setValue(true);
//                    break;
                case "reflection-classic":
                    option.setValue(this.useDoopReflection);
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


    private enum Violation {
        METHOD("_checkMethodViolation"),
        CLASS("_checkClassViolation"),
        FIELD("_checkViolationField");

        private final String name;

        private Violation(String s) {
            name = s;
        }


        public String getRuleName() {
            return name;
        }
    }
}
