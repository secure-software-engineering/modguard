package wala.entrypoint.generator;

import moduleanalysis.utils.ModuleAnalysisUtils;
import moduleanalysis.utils.accesschecker.AbstractModuleAccessChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.JasminClass;
import soot.options.Options;
import soot.util.JasminOutputStream;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by adann on 08.02.17.
 */
public class GenerateModule2ThreadEntryPoint {


    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private String modulePath;
    private String outputPath;
    private String moduleName;

    private ModuleAnalysisUtils utils = null;

    public GenerateModule2ThreadEntryPoint(String moduleName, String modulePath, String outputPath) {
        this.moduleName = moduleName;
        this.modulePath = modulePath;
        this.outputPath = outputPath;
        utils = null;
    }

    public void generateEntryPoint() throws IOException {
        G.v().reset();


        System.out.println("[Entrypoint] Generate Entrypoints for " + moduleName + "\n");

        setUpSoot(modulePath);


        //first we have to load all module-info files in the modulepath to build up
        //the module graph, which we use to resolve dependencies/references
       // Scene.v().loadModuleInformation(modulePath);
        //FIXME: broken

/*        //now we can load the classes of the module as application classes
        //Map<String, List<String>> map = SourceLocator.v().getClassesUnderModulePath(modulePath);
        for (String module : map.keySet()) {
            for (String klass : map.get(module)) {
                logger.info("Loaded Class: " + klass + "\n");
                ModuleAnalysisUtils.loadClass(klass, false, module);
            }

        }*/


        //this must be called after all classes are resolved; because it sets doneResolving in Scene
        Scene.v().loadNecessaryClasses();


        //List of EntryPoints
        List<SootMethod> moduleEntrypoints = new ArrayList<SootMethod>();
        Hierarchy hierarchy = Scene.v().getActiveHierarchy();


        //gather the entryPoints
        for (SootClass klass : Scene.v().getApplicationClasses()) {
            //Check if class is exported

            if (!klass.isExportedByModule()) {
                continue;
            }
            //so this klass is exported, thus we have to look for public methods / constructors
            for (SootMethod method : klass.getMethods()) {
                if (method.isConcrete() && method.isPublic()) {
                    if (method.isConstructor()) {
                        moduleEntrypoints.add(method);
                        continue;
                    }

                    moduleEntrypoints.add(method);

                }
            }
        }

        //make dummy mainClass
        WalaEntryPointCreater entryPointCreator = new WalaEntryPointCreater(moduleEntrypoints);
        entryPointCreator.setDummyClassName("DummyClass");
        entryPointCreator.setDummyMethodName("main");
        entryPointCreator.createDummyMain();

        SootClass dummyMain = entryPointCreator.getDummyMain();
        Collection<SootClass> dummyThreads = entryPointCreator.getDummyThreads();

        //generate the class Files
        List<SootClass> classesToGenerate = new ArrayList();
        classesToGenerate.add(dummyMain);
        classesToGenerate.addAll(dummyThreads);


        //safe entrypoint as ClassFile
        File dir = Paths.get(outputPath, moduleName).toFile();
        int i = 0;
        while (dir.exists()) {
            dir = Paths.get(outputPath, moduleName, "_" + i).toFile();
            i++;
        }
        dir.mkdir();
        for (SootClass klass : classesToGenerate) {
            String fileName = SourceLocator.v().getFileNameFor(klass, Options.output_format_class);
            fileName = fileName.substring(fileName.indexOf("/") + 1);

            fileName = dir.toString() + File.separator + fileName;

            OutputStream streamOut = null;
            try {
                System.out.println("Write output to: " + fileName);
                streamOut = new JasminOutputStream(new FileOutputStream(fileName));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));

            JasminClass jasminClass = new soot.jimple.JasminClass(klass);
            jasminClass.print(writerOut);
            writerOut.flush();
            streamOut.close();
        }

        System.out.println("Generate allowed classes");


        //initialize allowed Exported Classes
        utils = new ModuleAnalysisUtils(this.moduleName, AbstractModuleAccessChecker.DENY_ILLEGAL_ACCESS | AbstractModuleAccessChecker.CHECK_PRIVATE);


        //write classes that are allowed to be reachable to file
        {
            FileWriter fileWriter =
                    new FileWriter(dir.toString() + File.separator + "allowed_classes.sed");

            try (
                    BufferedWriter bufferedWriter =
                            new BufferedWriter(fileWriter))

            {

                for (SootClass klasss : Scene.v().getApplicationClasses()) {
                    if (!utils.isEscapingType(klasss.getType())) {
                        bufferedWriter.write(formatToSedScript(klasss.getName()));
                        bufferedWriter.newLine();
                    }
                }
            }
        }

        {
            //write exported classes to file
            FileWriter fileWriter =
                    new FileWriter(dir.toString() + File.separator + "exported_classes.log");

            try (BufferedWriter bufferedWriter =
                         new BufferedWriter(fileWriter)) {

                for (SootClass klasss : Scene.v().getClasses()) {
                    if (klasss.isExportedByModule()) {
                        bufferedWriter.write(klasss.getName());
                        bufferedWriter.newLine();

                    }
                }
            }
        }
        System.out.println("Done Generating");
    }


    private void setUpSoot(String modulePath) {
        System.out.println("[Soot] Setting Up Soot ... \n");
        Options.v().set_throw_analysis(Options.throw_analysis_unit);
        Options.v().set_include_all(true);
        Options.v().set_asm_backend(true);
        Options.v().set_debug(false);
        Options.v().set_debug_resolver(false);
        Options.v().set_prepend_classpath(true);
        //this must be deactivated to generate valid class files (see google)
        Options.v().set_no_output_inner_classes_attribute(true);
        Options.v().set_keep_line_number(true);
        Options.v().set_whole_program(true);

        Options.v().setPhaseOption("cg", "verbose:" + false);
        Options.v().setPhaseOption("cg", "safe-forname:" + false);

       // Options.v().set_module_mode(true);
        Options.v().set_soot_modulepath(modulePath);
    }


    public static String formatToSedScript(String line) {
        return "/.*" + line.replaceAll("\\.", "\\\\/") + ".*/d";
    }
}
