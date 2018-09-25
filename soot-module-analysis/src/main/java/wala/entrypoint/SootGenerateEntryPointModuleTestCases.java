package wala.entrypoint;

import wala.entrypoint.generator.GenerateModule2ThreadEntryPoint;

import java.io.IOException;

/**
 * Created by ralle on 24.08.16.
 */
public class SootGenerateEntryPointModuleTestCases {


    //Test Case A: return_stmt
    // private static String modulePath = "/home/adann/test_cases/A_return_stmt/modules";
    //works


    //Test Case B: public_fields
    // private static String modulePath = "/home/adann/test_cases/B_public_fields/modules";
    //works

    //Test Case B 2: Public Field Array
    //private static String modulePath = "/home/adann/test_cases/B2_public_fields_array/modules";
    //works

    //Test Case B 3: Public Field Collection
    //private static String modulePath = "/home/adann/test_cases/B3_public_fields_collection/modules";
     /*   works
     *workaround to deal with collection objects for furhter info, see
     * {@link #ptsForCollection(SootClass, PointsToSet, PointsToAnalysis, CallGraph)}
     *
     */

    //Test Case C: field_assignments on parameter objects
    //  private static String modulePath = "/home/adann/test_cases/C_field_assignment/modules";
    //works


    //Test Case C 2: field_assignments on parameter object array
    // private static String modulePath = "/home/adann/test_cases/C2_field_assignment_array/modules";
    //works


    //Test Case C 3: field_assignments on parameter object collection
    //  private static String modulePath = "/home/adann/test_cases/C3_field_assignment_collection/modules";
    /*   works
     *workaround to deal with collection objects for furhter info, see
     * {@link #ptsForCollection(SootClass, PointsToSet, PointsToAnalysis, CallGraph)}
     *
     */


    //Test Case D: callback on parameter objects
    //private static String modulePath = "/home/adann/test_cases/D_callback/modules";
    //works
    /*Note: for Interfaces / Abstract Classes as parameter the ModuleEntryPoint generator does the following
     *1. Looks if existing class implements / extends the abstract class
     * if exported class is found an instance of the found class is used as an argument
     *
     * 2. Else a dummy class implementing the interface/extending the abstract class is generated
     *   The created dummy class is returned by the Module Entry Point generator
     *   the locals of the dummy classes are added to the pts set to deal with callbacks in the module
     */


    //Test Case E: array as parameter
    //private static String modulePath = "/home/adann/test_cases/E_parameter_array/modules";
    //works


    //Test Case E 2: collection as parameter
  //  private static String modulePath = "/home/adann/test_cases/E2_parameter_collection/modules";
    /*   works
     *workaround to deal with collection objects for furhter info, see
     * {@link #ptsForCollection(SootClass, PointsToSet, PointsToAnalysis, CallGraph)}
     *
     */


    //Test Case F: Exceptions
    //  private static String modulePath = "/home/adann/test_cases/F_exceptions/modules";
    //works


    private static String testModules[] = {
            "/home/adann/test_cases/A_return_stmt/modules", //works 0
            "/home/adann/test_cases/B_public_fields/modules", //works 1
            "/home/adann/test_cases/B2_public_fields_array/modules", //works 2
            "/home/adann/test_cases/B3_public_fields_collection/modules", //works 3
            "/home/adann/test_cases/C_field_assignment/modules", //works 4
            "/home/adann/test_cases/C2_field_assignment_array/modules", //works 5
            "/home/adann/test_cases/C3_field_assignment_collection/modules",//works 6
            "/home/adann/test_cases/D_callback/modules", // works 7
            "/home/adann/test_cases/E_parameter_array/modules", // works 8
            "/home/adann/test_cases/E2_parameter_collection/modules", // works 9
            "/home/adann/test_cases/F_exceptions/modules",
//            "/home/adann/test_cases/G/modules"

    };

    public static void main(String[] args) throws IOException {
        int i = 0;
        for (String modulePath : testModules) {
            String logPath = "/home/adann/new_module_test_cases/module_"+i+"_result.log";



            System.out.println("Starting Analysis of Module " + modulePath);

            GenerateModule2ThreadEntryPoint entryPoint=  new GenerateModule2ThreadEntryPoint("de.upb.mod2",modulePath,"/home/adann/wala_module_testcases");
            entryPoint.generateEntryPoint();


            i++;
        }

    }


}
