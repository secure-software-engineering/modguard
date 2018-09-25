package moduleanalysis.utils.accesschecker;

import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

import java.util.HashSet;

/**
 * Created by adann on 17.06.17.
 */
public abstract class AbstractModuleAccessChecker {

    public static final int DENY_ILLEGAL_ACCESS = 2; //0010
    public static final int PERMIT_ILLEGAL_ACCESS = 4; //0100
    public static final int CHECK_PRIVATE = 1; //0001

    private final HashSet<SootClass> allowedExportedClasses;

    private final boolean considerOnlyPublic;

    protected AbstractModuleAccessChecker(HashSet<SootClass> allowedExportedClasses, boolean considerOnlyPublic) {
        this.allowedExportedClasses = allowedExportedClasses;
        this.considerOnlyPublic = considerOnlyPublic;
    }

    public static AbstractModuleAccessChecker getAccessChecker(int moduleAccessCheckMode, HashSet<SootClass> allowedExportedClasses) {
        boolean considerOnlyPublic = !((moduleAccessCheckMode & 1) == CHECK_PRIVATE);
        if ((moduleAccessCheckMode & DENY_ILLEGAL_ACCESS) == DENY_ILLEGAL_ACCESS)
            return new DenyIllegalAccessModuleChecker(allowedExportedClasses, considerOnlyPublic);
        if ((moduleAccessCheckMode & PERMIT_ILLEGAL_ACCESS) == PERMIT_ILLEGAL_ACCESS)
            return new PermitIllegalAccessModuleChecker(allowedExportedClasses, considerOnlyPublic);

        return null;
    }

    protected HashSet<SootClass> getAllowedExportedClasses() {
        return allowedExportedClasses;
    }

    protected boolean isConsiderOnlyPublic() {
        return considerOnlyPublic;
    }


    public abstract boolean isFieldAccessible(SootField sootField);


    public abstract boolean isClassAccessible(SootClass sootClass);


    public abstract boolean isMethodAccessible(SootMethod sootMethod);
}
