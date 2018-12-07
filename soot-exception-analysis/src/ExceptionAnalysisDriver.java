
import java.util.Map;

import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.Transform;
import soot.options.Options;
import soot.util.Chain;

public class ExceptionAnalysisDriver {
	public static void main(String[] args) {

		PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTransform", new SceneTransformer(){

			@Override
			protected void internalTransform(String phaseName, Map options) {
				// TODO Auto-generated method stub
				Chain<SootClass> classes = Scene.v().getApplicationClasses();
				System.out.println("Application classes analyzed: " + classes.toString());
				ExceptionAnalysis analysis = new ExceptionAnalysis(classes);
			}
			
		}));
		
		Options.v().set_whole_program(true);
		Options.v().setPhaseOption("wjpp", "enabled:false");
		Options.v().setPhaseOption("wspp", "enabled:false");
		Options.v().setPhaseOption("cg", "enabled:false");
		Options.v().setPhaseOption("wjap", "enabled:false");
		
		Options.v().setPhaseOption("bb", "enabled:false");
		Options.v().setPhaseOption("gb", "enabled:false");
		Options.v().setPhaseOption("tag", "enabled:false");
		Options.v().setPhaseOption("jap", "enabled:false");
		Options.v().setPhaseOption("jb", "use-original-names:true");
		Options.v().set_keep_line_number(true);
		Options.v().set_output_format(Options.output_format_jimple);
		
		//args is a list of all the application classes analyzed
		soot.Main.main(args);
	}

}
