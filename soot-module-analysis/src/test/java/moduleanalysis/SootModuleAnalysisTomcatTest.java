package moduleanalysis;

import moduleanalysis.criticalfinder.ReadFileCriticalFinder;
import moduleanalysis.utils.AnalysisReport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import soot.dava.toolkits.base.AST.analysis.Analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertFalse;

/**
 * Created by adann on 16.06.17.
 */

@RunWith(Parameterized.class)
public class SootModuleAnalysisTomcatTest {

    private String targetPath;
    private String modulePath;
    private String moduleName;
    public static String outputPath = "/home/adann/module_analysis_tomcat_RESULTS2/";
    public static String outputFileName = "tomcat_results.txt";
    public static String codePath;


    public static boolean useStrict = false;
    public static boolean useDoop = true;

    public static boolean onlyApplicationClasses = true;

    static {
        if (useStrict)
            codePath = "/home/adann/tomcat_modularization_2017_09_19/TOMCAT_8_5_21/outputNew/classes";
        else
            codePath = "/home/adann/tomcat_modularization_2017_09_19/TOMCAT_8_5_21/output/classes";

    }


    public SootModuleAnalysisTomcatTest(String targetPath, String modulePath, String moduleName) {
        this.targetPath = targetPath;
        this.moduleName = moduleName;
        this.modulePath = modulePath;
    }

    //FIXME: check results
    @Test
    public void testModuleAnalysis() throws Exception {
        //if(!moduleName.equals("annotations.api"))
         //   return;

        String excludeFile = "/home/adann/SuSi/identified_catalina.txt";
        System.out.println("Starting Analysis of Module " + modulePath);
        ModuleAnalysisBuilder builder = new ModuleAnalysisBuilder(moduleName, modulePath).setLogPath(targetPath);
        String modulePath = codePath + ":/home/adann/tomcat-build-libs/ecj-4.5.1/ecj-4.5.1.jar";
        //  if(moduleName.equals("catalina.ant") && useDoop){
        //    modulePath+=":/opt/apache-ant-1.10.1/lib/ant.jar";
        // }
        builder.appendToClassPath(modulePath);

        if (onlyApplicationClasses) {
            ArrayList<String> excludes = new ArrayList<>();
            excludes.add("org.apache.tools.*");
            excludes.add("org.eclipse.*");
            excludes.add("java.*");
            excludes.add("sun.*");
            excludes.add("javax.*");
            excludes.add("com.sun.*");
            excludes.add("com.ibm.*");
            excludes.add("org.xml.*");
            excludes.add("org.w3c.*");
            excludes.add("apple.awt.*");
            excludes.add("com.apple.*");
            builder.excludes(excludes);
        }


        builder.setCriticalFinder(new ReadFileCriticalFinder(new String[]{excludeFile}));

        //TODO: tomcat does not need boxing
        builder.setBoxing(false);


        AbstractModuleAnalysis escapeAnalysis;

        if (useDoop) {
            // builder.setOnlyApplicationClasses(false);
            escapeAnalysis = builder.buildDoopOnly();
        } else {
            escapeAnalysis = builder.build();
        }
        escapeAnalysis.doAnalysis();
        //  Collection<AbstractModuleAnalysis.AnalysisReport> results = escapeAnalysis.getIdentifiedViolations();

        Path dummyMainDir = Paths.get(outputPath);
        if (!Files.exists(dummyMainDir)) {
            Files.createDirectory(dummyMainDir);
        }

        String fileName = outputFileName;
        if (useStrict) {
            fileName = "strict_" + outputFileName;
        } else {
            fileName = "open_" + outputFileName;
        }
        if (useDoop) {
            fileName = "doop_" + fileName;
        } else {
            fileName = "soot_" + fileName;
        }


        Path dummyOutput = dummyMainDir.resolve(fileName);
        Charset charset = Charset.defaultCharset();
        if (!Files.exists(dummyOutput)) {
            try {
                Files.createFile(dummyOutput);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        BufferedWriter dummyOutPutStream = Files.newBufferedWriter(dummyOutput, charset, StandardOpenOption.APPEND);

        dummyOutPutStream.write("################################\n");

        dummyOutPutStream.write("Results for " + moduleName + "\n");

        dummyOutPutStream.write("################################\n");


        Collection<AnalysisReport> methods = escapeAnalysis.getIdentifiedViolationMethodsFiltered();
        Collection<AnalysisReport> classes = escapeAnalysis.getIdentifiedViolationClassesFiltered();
        Collection<AnalysisReport> fiedls = escapeAnalysis.getIdentifiedViolationFieldsFiltered();

        dummyOutPutStream.write("Methods: " + methods .size());
        dummyOutPutStream.write("\n");
        dummyOutPutStream.write("Fields: " + fiedls.size());
        dummyOutPutStream.write("\n");
        dummyOutPutStream.write("Classes: " + classes.size());
        dummyOutPutStream.write("\n");




        dummyOutPutStream.write("-- Methods -- \n");

        for (Object res : methods) {
            dummyOutPutStream.write(res.toString());
            dummyOutPutStream.write("\n");

        }

        dummyOutPutStream.write("-- Fields -- \n");

        for (Object res : fiedls) {
            dummyOutPutStream.write(res.toString());
            dummyOutPutStream.write("\n");

        }
        dummyOutPutStream.write("-- Classes -- \n");
        for (Object res : classes) {
            dummyOutPutStream.write(res.toString());
            dummyOutPutStream.write("\n");

        }


        dummyOutPutStream.close();
        assertFalse(methods.isEmpty() && classes.isEmpty() && fiedls.isEmpty());


    }


    @Parameterized.Parameters
    public static Collection testsCollection() throws Exception {
        //the directory containing the catalina/tomcat exploded modules
        File file = new File(codePath);
        File[] files = file.listFiles();
        Arrays.sort(files);
        Object[][] parameters = new Object[files.length][3];
        for (File f : files) {
            System.out.println(f.getName());
        }


        int i = 0;
        for (File f : files) {
            String targetPath = outputPath + File.separator + f.getName();
            String modulePath = f.getAbsolutePath() + File.separator;
            String moduleName = f.getName();
            parameters[i] = new Object[]{targetPath, modulePath, moduleName};
            i++;
        }

        return Arrays.asList(parameters);

    }


}