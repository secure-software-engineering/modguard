import java.util.HashMap;
import java.util.Map;
import soot.SootClass;
import soot.util.Chain;


public class ExceptionAnalysis {
	
	public ExceptionAnalysis(Chain<SootClass> classes) {
		SootClass next = classes.getFirst();
		Map<SootClass, ClassExceptionAnalysis> class_map = new HashMap<SootClass, ClassExceptionAnalysis>();  
		
		do {
			ClassExceptionAnalysis class_exception_analysis = new ClassExceptionAnalysis( next, class_map );
			class_map.put(next, class_exception_analysis);
			
			next = classes.getSuccOf(next);
		}
		while( next != null );
		
		boolean reached_fixed_point;
		do{
			reached_fixed_point = true;
			for (Map.Entry<SootClass, ClassExceptionAnalysis> entry : class_map.entrySet())
			{
				if(entry.getValue().DoAnalysis(false))
					reached_fixed_point = false;
			}
		} while( !reached_fixed_point );
		
		for (Map.Entry<SootClass, ClassExceptionAnalysis> entry : class_map.entrySet())
		{
			entry.getValue().DoAnalysis(true);
		}
	}

}
