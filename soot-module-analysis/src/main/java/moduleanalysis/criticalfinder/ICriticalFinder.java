package moduleanalysis.criticalfinder;

import soot.SootClass;
import soot.SootMethod;
import soot.jimple.spark.pag.SparkField;

import java.util.Set;

/**
 * Created by adann on 26.09.17.
 */
public interface ICriticalFinder {

    public void initialize();

    public Set<SparkField> getCriticalFields();

    public Set<SootMethod> getCriticalMethods();

    public Set<SootClass> getCriticalClasses();
}
