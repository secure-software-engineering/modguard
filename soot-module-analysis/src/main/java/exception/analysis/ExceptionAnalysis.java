package exception.analysis;

import soot.SootClass;
import soot.SootMethod;
import soot.SootModuleInfo;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.util.Chain;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * basic classes from https://github.com/joseph2625/soot-exception-analysis/blob/master/src/ExceptionAnalysis.java
 */
public class ExceptionAnalysis {


    protected static CallGraph cgForExceptionAnalysis = null;


    public static boolean isVerbose = false;

    private static Map<SootClass, ClassExceptionAnalysis> class_map = new HashMap<>();


    public static void reset() {
        cgForExceptionAnalysis = null;
        class_map.clear();
    }

    public ExceptionAnalysis(Chain<SootClass> classes, CallGraph cg) {
        //class_map = new HashMap<>();
        cgForExceptionAnalysis = cg;


        for (SootClass next : classes) {
            if (next.getName().equals(SootModuleInfo.MODULE_INFO))
                continue;
            ClassExceptionAnalysis class_exception_analysis = new ClassExceptionAnalysis(next, class_map);
            class_map.putIfAbsent(next, class_exception_analysis);

        }

        boolean reached_fixed_point;
        do {

            reached_fixed_point = true;
            for (Map.Entry<SootClass, ClassExceptionAnalysis> entry : class_map.entrySet()) {

                ExceptionAnalysis.verboseOutput("Doing Analysis for Class: " + entry.getKey());

                if (entry.getValue().DoAnalysis())
                    reached_fixed_point = false;
            }
        } while (!reached_fixed_point);

        for (Map.Entry<SootClass, ClassExceptionAnalysis> entry : class_map.entrySet()) {
            entry.getValue().DoAnalysis();
        }
    }


    public static void verboseOutput(String message) {
        if (ExceptionAnalysis.isVerbose)
            System.out.println(message);

    }

    public Collection<SootClass> getExceptions(SootMethod method) {
        if (this.class_map.isEmpty()) {
            throw new RuntimeException("Exception Analysis is Empty");
        }
        ClassExceptionAnalysis classExceptionAnalysis = this.class_map.get(method.getDeclaringClass());
        if (classExceptionAnalysis == null) {
            throw new RuntimeException("The SootClass " + method.getDeclaringClass() + " has not been analyzed.");
        }
        MethodExceptionAnalysis methodExceptionAnalysis = classExceptionAnalysis.method_analysis.get(method);
        return methodExceptionAnalysis.getMethod_exception_set();
    }


}
