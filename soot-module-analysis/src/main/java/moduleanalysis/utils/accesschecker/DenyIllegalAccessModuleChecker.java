package moduleanalysis.utils.accesschecker;

import beaver.Parser;
import soot.*;

import java.util.HashSet;

/**
 * Created by adann on 17.06.17.
 */
public class DenyIllegalAccessModuleChecker extends  AbstractModuleAccessChecker{


    protected DenyIllegalAccessModuleChecker(HashSet<SootClass> allowedExportedClasses, boolean considerOnlyPublic) {
        super(allowedExportedClasses, considerOnlyPublic);
    }

    public boolean isClassAccessible(SootClass sootClass) {
        if (sootClass == null) {
            throw new RuntimeException("[Error] Given class is null!");
        }
        if (sootClass.isExportedByModule() && sootClass.isPublic())
            return true;
        return (sootClass.isOpenedByModule() && !isConsiderOnlyPublic());

    }

    public boolean isMethodAccessible(SootMethod sootMethod) {
        //test if class is accessible
        if (this.isClassAccessible(sootMethod.getDeclaringClass()) && sootMethod.isPublic())
            return true;
        if (this.isClassAccessible(sootMethod.getDeclaringClass()) && sootMethod.isStatic() && sootMethod.isProtected())
            return true;
        if(this.isClassAccessible(sootMethod.getDeclaringClass()) && sootMethod.getDeclaringClass().isInterface())
            return true;
        // check if method is overridden from super/or extended class
        Hierarchy hierarchy = Scene.v().getActiveHierarchy();
        try {
            for (SootClass c : hierarchy.getSuperclassesOfIncluding(sootMethod.getDeclaringClass())) {
                SootMethod sm = c.getMethodUnsafe(sootMethod.getSubSignature());
                if (sm != null && this.getAllowedExportedClasses().contains(c)) {
                    return true;
                }
                for (SootClass cInterface : c.getInterfaces()) {
                    SootMethod smInter = cInterface.getMethodUnsafe(sootMethod.getSubSignature());
                    if (smInter != null && this.getAllowedExportedClasses().contains(cInterface)) {
                        return true;
                    }
                }
            }


        }
        catch (Exception e){
            return false;
        }
        return (sootMethod.getDeclaringClass().isOpenedByModule() && !isConsiderOnlyPublic());

    }

    public boolean isFieldAccessible(SootField sootField) {
        if (this.isClassAccessible(sootField.getDeclaringClass()) && sootField.isPublic())
            return true;
        if (this.isClassAccessible(sootField.getDeclaringClass()) && sootField.isStatic() && sootField.isProtected())
            return true;
        return (sootField.getDeclaringClass().isOpenedByModule() && !isConsiderOnlyPublic());
    }
}
