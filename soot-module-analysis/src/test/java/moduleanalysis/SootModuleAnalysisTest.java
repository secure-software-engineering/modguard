package moduleanalysis;

import moduleanalysis.utils.AnalysisReport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import soot.Scene;
import soot.SootClass;
import soot.SootResolver;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Created by adann on 16.06.17.
 */

@RunWith(Parameterized.class)
public class SootModuleAnalysisTest {

    private String targetPath;
    private String modulePath;
    private String moduleName;

    private static int counter = 0;

    public SootModuleAnalysisTest(String targetPath, String modulePath, String moduleName) {
        this.targetPath = targetPath;
        this.moduleName = moduleName;
        this.modulePath = modulePath;
    }

    //FIXME: check results
    @Test
    public void testModuleAnalysis() throws Exception {

        System.out.println("Starting Analysis of Module " + modulePath);
        if (counter > 36) {
            counter++;
            AbstractModuleAnalysis escapeAnalysis = new ModuleAnalysisBuilder(moduleName, modulePath).setLogPath(targetPath).appendToClassPath("/home/adann/IdeaProjects/jdk_escape_analysis/examples_module_conf_int/myannotation_module").build();
            escapeAnalysis.doAnalysis();
            Collection<AnalysisReport> results = escapeAnalysis.getIdentifiedViolations();
            assertFalse(results.isEmpty());
        }
        else{

                counter++;
                assertFalse(false);

        }

    }


    @Parameterized.Parameters
    public static Collection testsCollection() throws Exception {
        Collection parameters = new ArrayList();
        // parameters.addAll(patch());
        counter = 0;
        parameters.addAll(testFieldsCase01());
        parameters.addAll(testFieldsCase02());
        parameters.addAll(testFieldsCase03());
        parameters.addAll(testMethods());
        parameters.addAll(testClasses());
        return parameters;
    }


    public static Collection patch() throws Exception {
        File file = new File("src/test/resources/a");
        File[] files = file.listFiles();
        Arrays.sort(files);
        Object[][] parameters = new Object[files.length][3];
        int i = 0;
        for (File f : files) {
            String targetPath = "/home/adann/module_analysis_int_conf_RESULTS" + File.separator + f.getName();
            String modulePath = f.getAbsolutePath() + File.separator + "modules";
            String moduleName = "de.upb.mod2";
            parameters[i] = new Object[]{targetPath, modulePath, moduleName};
            i++;
        }

        return Arrays.asList(parameters);
    }

    public static Collection testFieldsCase01() throws Exception {
        File file = new File("src/test/resources/fields/case_01");
        File[] files = file.listFiles();
        Arrays.sort(files);
        Object[][] parameters = new Object[files.length][3];
        int i = 0;
        for (File f : files) {
            String targetPath = "/home/adann/module_analysis_int_conf_RESULTS" + File.separator + f.getName();
            String modulePath = f.getAbsolutePath() + File.separator + "modules";
            String moduleName = "de.upb.mod2";
            parameters[i] = new Object[]{targetPath, modulePath, moduleName};
            i++;
        }

        return Arrays.asList(parameters);
    }


    public static Collection testFieldsCase02() throws Exception {
        File file = new File("src/test/resources/fields/case_02");
        File[] files = file.listFiles();
        Arrays.sort(files);
        Object[][] parameters = new Object[files.length][3];
        int i = 0;
        for (File f : files) {
            String targetPath = "/home/adann/module_analysis_int_conf_RESULTS" + File.separator + f.getName();
            String modulePath = f.getAbsolutePath() + File.separator + "modules";
            String moduleName = "de.upb.mod2";
            parameters[i] = new Object[]{targetPath, modulePath, moduleName};
            i++;
        }

        return Arrays.asList(parameters);
    }

    public static Collection testFieldsCase03() throws Exception {
        File file = new File("src/test/resources/fields/case_03");
        File[] files = file.listFiles();
        Arrays.sort(files);
        Object[][] parameters = new Object[files.length][3];
        int i = 0;
        for (File f : files) {
            String targetPath = "/home/adann/module_analysis_int_conf_RESULTS" + File.separator + f.getName();
            String modulePath = f.getAbsolutePath() + File.separator + "modules";
            String moduleName = "de.upb.mod2";
            parameters[i] = new Object[]{targetPath, modulePath, moduleName};
            i++;
        }

        return Arrays.asList(parameters);
    }

    public static Collection testClasses() throws Exception {
        File file = new File("src/test/resources/classes");
        File[] files = file.listFiles();
        Arrays.sort(files);
        Object[][] parameters = new Object[files.length][3];
        int i = 0;
        for (File f : files) {
            String targetPath = "/home/adann/module_analysis_int_conf_RESULTS" + File.separator + f.getName();
            String modulePath = f.getAbsolutePath() + File.separator + "modules";
            String moduleName = "de.upb.mod2";
            parameters[i] = new Object[]{targetPath, modulePath, moduleName};
            i++;
        }

        return Arrays.asList(parameters);
    }

    public static Collection testMethods() throws Exception {
        File file = new File("src/test/resources/methods");
        File[] files = file.listFiles();
        Arrays.sort(files);
        Object[][] parameters = new Object[files.length][3];
        int i = 0;
        for (File f : files) {
            String targetPath = "/home/adann/module_analysis_int_conf_RESULTS" + File.separator + f.getName();
            String modulePath = f.getAbsolutePath() + File.separator + "modules";
            String moduleName = "de.upb.mod2";
            parameters[i] = new Object[]{targetPath, modulePath, moduleName};
            i++;
        }

        return Arrays.asList(parameters);
    }

}