package moduleanalysis.criticalfinder;

import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.TagBuilderCallback;
import soot.jimple.spark.pag.SparkField;
import soot.options.Options;
import soot.tagkit.AnnotationTag;
import soot.tagkit.Host;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by adann on 26.09.17.
 */
public class TagBasedCriticalFinder implements TagBuilderCallback, ICriticalFinder {


    private HashSet<SootClass> criticalClasses = new HashSet<>();
    private HashSet<SparkField> criticalFields = new HashSet<>();
    private HashSet<SootMethod> criticalMethods = new HashSet<>();

    public TagBasedCriticalFinder() {

    }

    @Override
    public void tagBuild(Host host, AnnotationTag annotationTag) {
        if (!annotationTag.getType().toString().contains("Critical"))
            return;
        System.out.println("Host: " + host + " annotation: " + annotationTag.getName());


        SootEnum typeName = SootEnum.valueOf(host.getClass().getSimpleName());
        switch (typeName) {
            case SootClass:
                criticalClasses.add((SootClass) host);
                break;
            case SootField:
                criticalFields.add((SootField) host);
                break;
            case SootMethod:
                criticalMethods.add((SootMethod) host);
                break;
        }


    }

    @Override
    public void initialize() {
        Options.v().tgb = this;
    }

    @Override
    public Set<SparkField> getCriticalFields() {
        return this.criticalFields;
    }

    @Override
    public Set<SootMethod> getCriticalMethods() {
        return this.criticalMethods;
    }

    @Override
    public Set<SootClass> getCriticalClasses() {
        return this.criticalClasses;
    }

    private enum SootEnum {
        SootMethod, SootClass, SootField
    }
}
