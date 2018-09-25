package moduleanalysis.utils;

import com.google.common.base.Optional;
import moduleanalysis.utils.accesschecker.AbstractModuleAccessChecker;
import soot.*;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * This class provides utility functions for Module Analyses
 * Created by adann on 08.04.17.
 */
public class ModuleAnalysisUtils {

    //the name of the includes file
    private static final String includeFileName = "includes.txt";
    private static final String excludeFileName = "excludes.txt";


    private List<String> includedClassAndPackages;
    private List<String> excludedClassAndPackages;

    public Set<String> getAllowedExportedPackages() {
        return allowedExportedPackages;
    }

    public HashSet<SootClass> getAllowedExportedClasses() {
        return allowedExportedClasses;
    }

    private final Set<String> allowedExportedPackages;

    private final HashSet<SootClass> allowedExportedClasses;

    private final AbstractModuleAccessChecker accessChecker;


    public ModuleAnalysisUtils(String moduleName, int moduleAccessCheckMode) {
        readExcludeFile();
        readIncludeFile();
        allowedExportedPackages = new HashSet<>();
        allowedExportedClasses = new HashSet<>();
        this.computeAllowedExportedClasses(moduleName);
        accessChecker = AbstractModuleAccessChecker.getAccessChecker(moduleAccessCheckMode, this.allowedExportedClasses);
    }


    public boolean isClassAccessible(SootClass sootClass) {
        return accessChecker.isClassAccessible(sootClass);

    }

    public boolean isMethodAccessible(SootMethod sootMethod) {
        return accessChecker.isMethodAccessible(sootMethod);

    }


    public boolean isFieldAccessible(SootField sootField) {
        return accessChecker.isFieldAccessible(sootField);
    }

    /**
     * reads in the classes stored in the file excludes.txt
     * that should be ignored during the creation of entryPoint
     */
    private void readExcludeFile() {

        Path excludeFile = Paths.get(excludeFileName);
        InputStream in = null;
        Charset charset = Charset.defaultCharset();
        try {
            if (!Files.exists(excludeFile)) {
                //else take the one package

                in = getClass().getResourceAsStream(File.separator + excludeFileName);
            } else {
                in = Files.newInputStream(excludeFile);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        //read file into stream, try-with-resources
        try (
                Stream<String> stream = new BufferedReader(new InputStreamReader(in, charset)).lines())

        {
            excludedClassAndPackages = new LinkedList<>();
            stream.forEach(line -> excludedClassAndPackages.add(line));

        }

    }

    /**
     * reads in the classes stored in the file includes.txt
     * that contain regex for classes that should be also considered as escapes
     * creates based on the file a list of regex
     */
    private void readIncludeFile() {

        Path includeFile = Paths.get(includeFileName);

        InputStream in = null;
        Charset charset = Charset.defaultCharset();
        try {
            if (!Files.exists(includeFile)) {
                //else take the one package

                in = getClass().getResourceAsStream(File.separator + excludeFileName);
            } else {
                in = Files.newInputStream(includeFile);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        //read file into stream, try-with-resources
        try (Stream<String> stream = new BufferedReader(new InputStreamReader(in, charset)).lines()) {
            includedClassAndPackages = new LinkedList<>();
            stream.forEach(line -> includedClassAndPackages.add(line));

        }

    }


    private void computeAllowedExportedClasses(String moduleName) {
        //compute which packages are allowed to escape
        //add java.base exports
        SootModuleInfo javaBaseModule = null;
        SootModuleInfo moduleInfo = null;

        java.util.Optional javaBaseModuleInfo = Scene.v().getClasses().stream().filter((p) -> p.getName().equals(SootModuleInfo.MODULE_INFO) && p.moduleName.equals("java.base")).findFirst();
        if (javaBaseModuleInfo.isPresent())
            javaBaseModule = (SootModuleInfo) javaBaseModuleInfo.get();

        this.determineAllowedExportPackages(javaBaseModule);

        //add exports of the module under analysis
        java.util.Optional moduleInfoOpt = Scene.v().getApplicationClasses().stream().filter((p) -> p.getName().equals(SootModuleInfo.MODULE_INFO) && p.moduleName.equals(moduleName)).findFirst();
        if (moduleInfoOpt.isPresent())
            moduleInfo = (SootModuleInfo) moduleInfoOpt.get();

        if (moduleInfo == null) {
            throw new RuntimeException("No Module-Descriptor found for module: " + moduleName);
        }

        this.determineAllowedExportPackages(moduleInfo);

        //initialize allowed Exported Classes
        this.determineAllowedExportedSootClasses();
    }


    private void determineAllowedExportPackages(SootModuleInfo modInfo) {

        if (allowedExportedPackages.containsAll(modInfo.getPublicExportedPackages())) {
            return;
        }

        //first add the packages that are exported by the module-info
        allowedExportedPackages.addAll(modInfo.getPublicExportedPackages());


        //second add the requires public of required modules
        for (SootModuleInfo reqModInfo : modInfo.getRequiredPublicModules()) {
            determineAllowedExportPackages(reqModInfo);
        }


    }


    private void determineAllowedExportedSootClasses() {
        for (SootClass sootClass : Scene.v().getClasses()) {
            String javaPackageName = sootClass.getJavaPackageName();
            if (this.allowedExportedPackages.contains(javaPackageName)) {
                this.allowedExportedClasses.add(sootClass);
            }
        }

    }


    public static SootClass loadClass(String name, boolean main, String module) {
        SootClass c = ModuleScene.v().loadClassAndSupport(name, Optional.of(module));
        c.setApplicationClass();
        if (main)
            Scene.v().setMainClass(c);
        return c;
    }


    public static boolean arePrimitiveOrVoidTypes(Collection<Type> types) {
        boolean result = true;
        for (Type type : types) {
            result = result & ModuleAnalysisUtils.isPrimitiveOrVoidType(type);
        }
        return result;
    }

    //we skip fields of primitive type, since they do not carry references to classes
    public static boolean isPrimitiveOrVoidType(Type type) {
        if (type instanceof ArrayType) {
            type = ((ArrayType) type).baseType;
        }
        if (type instanceof PrimType) {
            return true;
        }
        if (type instanceof VoidType) {
            return true;
        }
        return false;
    }


    /**
     * We check if a Type is escaping: should not leave this module
     *
     * @param type to check
     * @return whether the type escapes a module
     */
    public boolean isEscapingType(Type type) {

        if (type instanceof ArrayType) {
            type = ((ArrayType) type).baseType;
        }

        if (isPrimitiveOrVoidType(type)) {
            return false;
        }


        if (type instanceof RefType) {
            Queue<SootClass> sootClassQueue = new ArrayDeque<>();
            sootClassQueue.add(((RefType) type).getSootClass());


            // here we check if the class is mentioned in the special includes
            //NOTE: THIS DOES NOT APPLY TO SUBCLASSES ONLY THE CLASS ITSELF DIRECTLY
            // for applying it to subclasses/implementing classes this check have to be moved in the while loop below
            if (isIncluded(((RefType) type).getSootClass().getName())) {
                return true;
            }

            while (!sootClassQueue.isEmpty()) {
                SootClass element = sootClassQueue.remove();

                //TODO: check if these cases ever fail: 1 part true, but second part NOT ???
                if (this.allowedExportedClasses.contains(element) && (element.isPublic() || this.allowedExportedPackages.contains(element.getJavaPackageName()))) {
                    return false;
                }

                //check if the class implements an exported interfaces
                sootClassQueue.addAll(element.getInterfaces().getElementsUnsorted());

                //check if the class implements an exported super-class
                try {
                    SootClass sootSuperClass = element.getSuperclass();
                    //since if there is no superclass an exception is thrown, thus the class does not implement one
                    if (sootSuperClass != ModuleScene.v().getSootClass("java.lang.Object", Optional.of("java.base"))) {
                        sootClassQueue.add(element.getSuperclass());
                    }
                } catch (RuntimeException e) {
                    //the superClass call fails and until now, no public superclass/interface has been found, thus this is an internal class
                    return true;
                }

            }


        }
        return true;
    }

    /**
     * checks if the class is in the included list
     *
     * @param clazzName String of the class' name
     * @return if the class is mentioned in the included list
     */
    public boolean isIncluded(String clazzName) {
        return includedClassAndPackages.stream().anyMatch(pkg -> (clazzName.equals(pkg) || ((pkg.endsWith("*") || pkg.endsWith(".*") || pkg.endsWith("$*")) && clazzName.startsWith(pkg.substring(0, pkg.length() - 1)))));
    }


    public boolean isExcluded(String clazzName) {
        return excludedClassAndPackages.stream().anyMatch(pkg -> (clazzName.equals(pkg) || ((pkg.endsWith("*") || pkg.endsWith(".*") || pkg.endsWith("$*")) && clazzName.startsWith(pkg.substring(0, pkg.length() - 1)))));
    }


    /**
     * Here we are only interested in fields that are accessible in the module system (public fields)
     * including superclasses' fields
     *
     * @param sootClass the SootClass whose fields should be returned
     * @return all fields (including superclasses' fields) that are accessible in the module system
     */
    public Collection<SootField> getAccessibleFieldsIncludingSuperClass(SootClass sootClass) {
        HashSet<SootField> fields = new HashSet<>();

        SootClass sootClassToVisit = sootClass;
        while (sootClassToVisit != null) {
            if (this.isClassAccessible(sootClassToVisit)) {

                for (SootField field : sootClassToVisit.getFields()) {
                    if (this.isFieldAccessible(field)) {
                        fields.add(field);
                    }
                }
            }
            try {
                sootClassToVisit = sootClassToVisit.getSuperclass();
            } catch (RuntimeException e) {
                sootClassToVisit = null;
            }
        }
        return fields;
    }

    /**
     * Here we are only interested in methods that are accessible in the module system (public fields)
     *
     * @param sootClass the SootClass whose methods should be checked
     * @return all accessible/invokable methods including superclasses' methods
     */
    public Collection<SootMethod> getAccessibleMethodsIncludingSuperClass(SootClass sootClass) {
        HashSet<SootMethod> methods = new HashSet<>();

        SootClass sootClassToVisit = sootClass;
        while (sootClassToVisit != null) {
            if (this.isClassAccessible(sootClassToVisit)) {

                for (SootMethod method : sootClassToVisit.getMethods()) {
                    if (this.isMethodAccessible(method)) {
                        methods.add(method);
                    }
                }
            }
            try {
                sootClassToVisit = sootClassToVisit.getSuperclass();
            } catch (RuntimeException e) {
                sootClassToVisit = null;
            }
        }
        return methods;
    }
}
