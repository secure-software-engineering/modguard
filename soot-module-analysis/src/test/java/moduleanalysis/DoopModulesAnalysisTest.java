package moduleanalysis;

import moduleanalysis.utils.AnalysisReport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertFalse;

/**
 * Created by adann on 16.06.17.
 */

@RunWith(Parameterized.class)
public class DoopModulesAnalysisTest {

    private String targetPath;
    private String modulePath;
    private String moduleName;
    private static int counter = 0;

    private static boolean ignoreArrayColl = false;

    public DoopModulesAnalysisTest(String targetPath, String modulePath, String moduleName) {
        this.targetPath = targetPath;
        this.moduleName = moduleName;
        this.modulePath = modulePath;
    }

    //FIXME: check result
    @Test
    public void testModuleAnalysis() throws Exception {

        System.out.println("Starting Analysis of Module " + modulePath);
        AbstractModuleAnalysis doopModulesEscapeAnalysis = new ModuleAnalysisBuilder(moduleName, modulePath).setIgnoreArrayCollection(ignoreArrayColl).setLogPath(targetPath).appendToClassPath("/home/adann/IdeaProjects/jdk_escape_analysis/examples_module_conf_int/myannotation_module").setBoxing(false).useReflectionExtension(true).buildDoopOnly();
        //collection || counter == 9 || counter == 12 ||
        //false positive 43,44
        // array counter == 13
       // counter == 6 || counter ==8 || counter == 9 || counter == 12 || counter == 13
        //counter == 6 || counter == 9 || counter == 12 || counter == 13
        //  AbstractModuleAnalysis doopModulesEscapeAnalysis = new ModuleAnalysisBuilder(moduleName, modulePath).setLogPath(targetPath).appendToClassPath("/home/adann/IdeaProjects/jdk_escape_analysis/examples_module_conf_int/myannotation_module").buildDoop();
        if ( counter == 15 || counter == 16){
                //counter == 10   || counter == 34 || counter == 36 || counter == 8 || counter == 14){

            counter++;
            doopModulesEscapeAnalysis.doAnalysis();

            Collection<AnalysisReport> results = doopModulesEscapeAnalysis.getIdentifiedViolations();
            assertFalse(results.isEmpty());
        } else {
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