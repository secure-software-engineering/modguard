package exception.analysis;

import soot.SootClass;
import soot.SootMethod;

import java.util.HashMap;
import java.util.Map;


public class ClassExceptionAnalysis {

    private final Map<SootClass, ClassExceptionAnalysis> class_map;
    public final Map<SootMethod, MethodExceptionAnalysis> method_analysis;
    private final SootClass soot_class;


    public ClassExceptionAnalysis(SootClass c, Map<SootClass, ClassExceptionAnalysis> cm) {
        soot_class = c;
        class_map = cm;
        method_analysis = new HashMap<>();

        for (SootMethod method : soot_class.getMethods()) {
            method_analysis.putIfAbsent(method, new MethodExceptionAnalysis(soot_class, method, class_map));
        }
    }

    public boolean DoAnalysis() {
        boolean return_value = false;
        for (Map.Entry<SootMethod, MethodExceptionAnalysis> entry : method_analysis.entrySet()) {
            ExceptionAnalysis.verboseOutput("Doing Analysis for Method: " + entry.getKey());

            if (entry.getValue().DoAnalysis())
                return_value = true;
        }

        return return_value;
    }


}
