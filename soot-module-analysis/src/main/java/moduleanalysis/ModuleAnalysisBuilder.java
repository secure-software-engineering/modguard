package moduleanalysis;

import moduleanalysis.criticalfinder.ICriticalFinder;
import moduleanalysis.criticalfinder.TagBasedCriticalFinder;
import moduleanalysis.utils.accesschecker.AbstractModuleAccessChecker;

import java.util.List;

/**
 * Builder Pattern for the ModuleEscapeAnalysis
 */
public class ModuleAnalysisBuilder {
    //required
    protected final String modulePath;
    protected final String moduleName;

    protected ICriticalFinder finder = new TagBasedCriticalFinder();

    //optional
    protected boolean verbose = false;
    protected boolean considerOnlyPublic = false;
    protected int moduleAccessMode = AbstractModuleAccessChecker.DENY_ILLEGAL_ACCESS | AbstractModuleAccessChecker.CHECK_PRIVATE;
    protected String logPath = null;
    protected String classPath = "";
    protected List<String> excludes = null;

    protected boolean boxingEnabled = true;

    protected boolean onlyApplicationClasses = false;

    protected boolean ignoreCollectionArrays = true;

    protected boolean useDoopReflection = false;


    /**
     * Initializes the builder with required parameters
     *
     * @param moduleName the name of the module to analyse
     * @param modulePath the path where the module is stored
     */
    public ModuleAnalysisBuilder(String moduleName, String modulePath) {
        this.modulePath = modulePath;
        this.moduleName = moduleName;
    }


    public AbstractModuleAnalysis build() {
        return new SootModuleAnalysis(this);
    }


    public AbstractModuleAnalysis buildDoop() {
        return new DoopModuleAnalysis(this);
    }


    public AbstractModuleAnalysis buildDoopOnly() {
        return new DoopOnlyModuleAnalysis(this);
    }

    /**
     * Whether the analysis should print out debug information
     *
     * @param verbose false/true
     * @return the builder
     */
    public ModuleAnalysisBuilder setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }



    public ModuleAnalysisBuilder useReflectionExtension(boolean useDoopReflection) {
        this.useDoopReflection = useDoopReflection;
        return this;
    }

    public ModuleAnalysisBuilder setIgnoreArrayCollection(boolean ignore) {
        this.ignoreCollectionArrays = ignore;
        return this;
    }
    /**
     * Should the analysis also consider non public fields/methods/classes
     *
     * @param considerOnlyPublic if only public fields should be considered
     * @return the builder
     */
    public ModuleAnalysisBuilder setConsiderOnlyPublic(boolean considerOnlyPublic) {
        this.considerOnlyPublic = considerOnlyPublic;
        return this;
    }


    /**
     * Which AccessMode Shall be used for the Analysis
     *
     * @param moduleAccessMode whether only public classes and methods should be considers
     * @return the builder
     */
    public ModuleAnalysisBuilder setModuleAccessMode(int moduleAccessMode) {
        this.moduleAccessMode = moduleAccessMode;
        return this;
    }

    public ModuleAnalysisBuilder setCriticalFinder(ICriticalFinder finder) {
        this.finder = finder;
        return this;
    }

    public ModuleAnalysisBuilder setOnlyApplicationClasses(boolean onlyApplicationClasses) {
        this.onlyApplicationClasses = onlyApplicationClasses;
        return this;
    }


    /**
     * @param logPath the path where to store the results in form of a log file
     * @return the builder
     */
    public ModuleAnalysisBuilder setLogPath(String logPath) {
        this.logPath = logPath;
        return this;
    }

    public ModuleAnalysisBuilder appendToClassPath(String classPath) {
        this.classPath = classPath;
        return this;
    }

    public ModuleAnalysisBuilder excludes(List<String> excludes) {
        this.excludes = excludes;
        return this;
    }

    public ModuleAnalysisBuilder setBoxing(boolean enable) {
        this.boxingEnabled = enable;
        return this;
    }


}
