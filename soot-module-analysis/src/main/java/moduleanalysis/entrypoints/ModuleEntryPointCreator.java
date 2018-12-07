
package moduleanalysis.entrypoints;

import moduleanalysis.utils.ModuleAnalysisUtils;
import ppg.spec.Spec;
import soot.jimple.infoflow.entryPointCreators.BaseEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.SequentialEntryPointCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.*;
import soot.jimple.internal.JEqExpr;
import soot.jimple.internal.JGotoStmt;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JNopStmt;
import soot.jimple.toolkits.scalar.NopEliminator;
import soot.tagkit.LineNumberTag;
import com.google.common.base.Optional;

import java.util.*;
import java.util.Map.Entry;

/**
 * Entry point creator that performs Java method invocations. The invocation
 * order is simulated to be arbitrary. If you only need a sequential list,
 * use the {@link SequentialEntryPointCreator} instead.
 *
 * @author adann
 */
public class ModuleEntryPointCreator extends BaseEntryPointCreator {

    private static final Logger logger = LoggerFactory.getLogger(ModuleEntryPointCreator.class);

    private HashSet<SootMethod> methodsToCall;
    private final HashSet<SootMethod> doneMethodsToCall;

    private int conditionCounter;
    private Value intCounter;

    private SootMethod generatedDummyMain;

    private boolean appendMode;

    private final Collection<SootClass> dummyClasses;
    private JGotoStmt gotoStart;
    private Stmt returnStmt;
    // private final Set<String> allowedExports;

    private final ModuleAnalysisUtils utils;

    private SootModuleInfo dummyModuleInfo;

    /**
     * Creates a new instanceof the {@link ModuleEntryPointCreator} class
     *
     * @param methodsToCall A collection containing the methods to be called
     *                      in the dummy main method. Note that the order of the method calls is
     *                      simulated to be arbitrary. Entries must be valid Soot method signatures.
     */
    public ModuleEntryPointCreator(HashSet<SootMethod> methodsToCall, ModuleAnalysisUtils utils) {
        this.methodsToCall = methodsToCall;
        dummyModuleInfo = new SootModuleInfo(SootModuleInfo.MODULE_INFO, "DummyModule", true);
        dummyModuleInfo.setResolvingLevel(SootClass.BODIES);
        Scene.v().addClass(dummyModuleInfo);
        // this.allowedExports = utils.getAllowedExportedPackages();
        this.utils = utils;
        this.appendMode = false;
        dummyClasses = new HashSet<>();
        doneMethodsToCall = new HashSet<>();
        generatedDummyMain = null;
    }


    public Collection<SootClass> getDummyClasses() {
        return dummyClasses;
    }


    @Override
    public SootMethod createDummyMain() {
        //reinitialize localVarsMap
        this.localVarsForClasses.clear();
        // Load the substitution classes
        if (substituteCallParams)
            for (String className : substituteClasses)
                Scene.v().forceResolve(className, SootClass.BODIES).setApplicationClass();
        generatedDummyMain = this.createDummyMainInternal();

        this.doneMethodsToCall.addAll(this.methodsToCall);
        this.methodsToCall.clear();

        return generatedDummyMain;
    }

    public SootMethod createDummyMain(HashSet<SootMethod> entrypoints) {
        //reinitialize localVarsMap
        // Load the substitution classes
        this.methodsToCall = entrypoints;
        return createDummyMain();
    }

    @Override
    public SootMethod createDummyMain(SootMethod dummyMainMethod) {
        //reinitialize localVarsMap
        this.localVarsForClasses.clear();
        // Load the substitution classes
        if (substituteCallParams)
            for (String className : substituteClasses)
                Scene.v().forceResolve(className, SootClass.BODIES).setApplicationClass();

        generatedDummyMain = this.createDummyMainInternal(dummyMainMethod);

        this.doneMethodsToCall.addAll(this.methodsToCall);
        this.methodsToCall.clear();

        return generatedDummyMain;
    }


    /**
     * Appends the methods to the already generated dummy main
     * if no dummy main is generated a new is returned same behavior as calling
     * createDummyMain()
     *
     * @return the generated or appended dummy main method
     */
    public SootMethod appendToDummyMain(HashSet<SootMethod> methodsToCall) {
        if (generatedDummyMain == null) {
            this.methodsToCall = methodsToCall;
            generatedDummyMain = this.createDummyMain();
            doneMethodsToCall.addAll(methodsToCall);
            this.methodsToCall.clear();
            this.appendMode = true;
        } else {
            this.appendMode = true;
            //compute the new methods by u
            HashSet<SootMethod> newMethodsToCall = new HashSet<>(methodsToCall);
            newMethodsToCall.removeAll(doneMethodsToCall);

            this.methodsToCall = newMethodsToCall;
            this.createDummyMainInternal(generatedDummyMain);
            doneMethodsToCall.addAll(newMethodsToCall);
            this.methodsToCall.clear();

        }
        return generatedDummyMain;


    }


    @Override
    protected SootMethod createEmptyMainMethod(Body body) {
        // If we already have a main class, we need to make sure to use a fresh
        // method name

        String methodName = dummyMethodName;
        SootClass mainClass;
        if (Scene.v().containsClass(dummyClassName)) {
            int methodIndex = 0;
            mainClass = Scene.v().getSootClass(dummyClassName);
            while (mainClass.declaresMethodByName(methodName))
                methodName = dummyMethodName + "_" + methodIndex++;
        } else {
            mainClass = new SootClass(dummyClassName, this.dummyModuleInfo.getModuleName());
            Scene.v().addClass(mainClass);
        }

        Type stringArrayType = ArrayType.v(ModuleRefType.v("java.lang.String", Optional.of("java.base")), 1);
        SootMethod mainMethod = new SootMethod(methodName,
                Collections.singletonList(stringArrayType), VoidType.v());
        body.setMethod(mainMethod);
        mainMethod.setActiveBody(body);
        mainClass.addMethod(mainMethod);

        // Add a parameter reference to the body
        LocalGenerator lg = new LocalGenerator(body);
        Local paramLocal = lg.generateLocal(stringArrayType);
        body.getUnits().addFirst(Jimple.v().newIdentityStmt(paramLocal,
                Jimple.v().newParameterRef(stringArrayType, 0)));

        // First add class to scene, then make it an application class
        // as addClass contains a call to "setLibraryClass"
        mainClass.setApplicationClass();
        mainMethod.setModifiers(java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.STATIC);
        return mainMethod;

    }

    @Override
    protected SootMethod createDummyMainInternal(SootMethod mainMethod) {
        Map<SootClass, Set<SootMethod>> classMap = new HashMap<>();
        for (SootMethod method : methodsToCall) {
            if (!classMap.containsKey(method.getDeclaringClass())) {
                classMap.put(method.getDeclaringClass(), new HashSet<>());
            }

            classMap.get(method.getDeclaringClass()).add(method);
        }


        // create new class:
        Body body = mainMethod.getActiveBody();
        LocalGenerator generator = new LocalGenerator(body);

        // create constructors:
        for (SootClass className : classMap.keySet()) {
            //already resolved
            /*Scene.v().forceResolve(className, SootClass.BODIES);
            className.setApplicationClass();
			*/

            ModuleScene.v().forceResolve(className.toString(), SootClass.BODIES, Optional.fromNullable(className.moduleName));
            className.setApplicationClass();
            Local localVal = generateClassConstructor(className, body);
            if (localVal == null) {
                logger.warn("Cannot generate constructor for class: {}", className);
                continue;
            }
            //get the actucal local class a dummy class my have been created

            localVarsForClasses.put(localVal.getType().getEscapedName(), localVal);
            new LineNumberTag(1);
        }

        // add entrypoints calls
        JNopStmt startStmt = null;
        if (!appendMode) {
            conditionCounter = 0;
            intCounter = generator.generateLocal(IntType.v());
            startStmt = new JNopStmt();
            body.getUnits().add(startStmt);
        }


        for (Entry<SootClass, Set<SootMethod>> entry : classMap.entrySet()) {
            Local classLocal = localVarsForClasses.get(entry.getKey().getName());
            for (SootMethod currentMethod : entry.getValue()) {

                if (currentMethod == null) {
                    logger.warn("Entry point not found: {}", currentMethod);
                    continue;
                }

                JEqExpr cond = new JEqExpr(intCounter, IntConstant.v(conditionCounter));
                conditionCounter++;
                JNopStmt thenStmt = new JNopStmt();
                JIfStmt ifStmt = new JIfStmt(cond, thenStmt);
                body.getUnits().add(ifStmt);
                Stmt stmt = buildMethodCall(currentMethod, body, classLocal, generator);

                //if we have a factory and get a a return of a exported interface than call these interface methods
                if (!currentMethod.isConstructor() && !(currentMethod.getReturnType() instanceof VoidType || currentMethod.getReturnType() instanceof PrimType) && (stmt instanceof AssignStmt)) {
                    if (currentMethod.getReturnType() instanceof RefType) {
                        SootClass retClass = ((RefType) currentMethod.getReturnType()).getSootClass();
                        if (retClass.isExportedByModule() && (retClass.isAbstract() || retClass.isInterface())) {

                            // maintain pseudo randomness
                            for (SootMethod m : retClass.getMethods()) {
                                // added private/protected methods here
                                if (!m.isConstructor() && utils.isMethodAccessible(m)) {
                                    buildMethodCall(m, body, (Local) ((AssignStmt) stmt).getLeftOp(), generator);

                                }

                            }


                        }

                    }

                }

                body.getUnits().add(thenStmt);
            }
        }
        // in appendMode remove this, and add this at the end again
        if (!appendMode) {
            JNopStmt endStmt = new JNopStmt();
            body.getUnits().add(endStmt);
            gotoStart = new JGotoStmt(startStmt);
            body.getUnits().add(gotoStart);
            returnStmt = Jimple.v().newReturnVoidStmt();
            body.getUnits().add(returnStmt);
        } else {
            //remove the statements at their current position
            // that is in the middle of the method
            body.getUnits().remove(gotoStart);
            body.getUnits().remove(returnStmt);

            //add them again at the end of the method
            body.getUnits().add(gotoStart);
            body.getUnits().add(returnStmt);
        }

        NopEliminator.v().transform(body);
        eliminateSelfLoops(body);
        return mainMethod;
    }

    @Override
    public Collection<String> getRequiredClasses() {
        return Collections.emptyList();
    }


    @Override
    protected Value getValueForType(Body body, LocalGenerator gen,
                                    Type tp, Set<SootClass> constructionStack, Set<SootClass> parentClasses) {


        // Depending on the parameter type, we try to find a suitable
        // concrete substitution
        if (isSimpleType(tp.toString()))
            return getSimpleDefaultValue(tp.toString());
        else if (tp instanceof RefType) {
            SootClass classToType = ((RefType) tp).getSootClass();

            if (classToType != null) {
                SootClass originalClassToType = classToType;

                { //FIXME: hack for the stack
                    Local classLocal = localVarsForClasses.get(classToType.getName());
                    if (classLocal != null) {
                        return classLocal;
                    }
                }

                //TODO: check if substituted class is in allowed exported classes

                // If we have a parent class compatible with this type and the class is exported, we use
                // it before we check any other option
                for (SootClass parent : parentClasses) {
                    if (isCompatible(parent, classToType) && parent.isExportedByModule()) {
                        Value val = this.localVarsForClasses.get(parent.getName());
                        if (val != null)
                            return val;
                    }

                }
                if (classToType.isAbstract() || classToType.isInterface()) {
                    //check if a public exported class implementing this interface or extending the abstract class exists
                    if (classToType.isInterface()) {

                        for (SootClass implementorOfSootClass : Scene.v().getActiveHierarchy().getImplementersOf(classToType)) {
                            if (!implementorOfSootClass.isAbstract() && implementorOfSootClass.isExportedByModule())
                                classToType = implementorOfSootClass;
                        }

                    } else {
                        for (SootClass sootSubClass : Scene.v().getActiveHierarchy().getSubclassesOf(classToType)) {
                            if (isCompatible(sootSubClass, classToType) && !sootSubClass.isAbstract() && sootSubClass.isExportedByModule())
                                classToType = sootSubClass;
                        }
                    }


                    SootClass classToCheck = classToType;
                    // always prefer non dummy classes, but look for methods that return appropriate type
                    if (getDummyClasses().contains(classToType)) {
                        classToCheck = originalClassToType;
                        Value val = this.localVarsForClasses.get(classToCheck.getName());
                        if (val != null)
                            return val;
                    }

                    //FIXME: always prefer methods
                    //if we did not find a concrete class look, check if a method has this as a return type
                    //((classToCheck.isInterface() || classToCheck.isAbstract()) && this.localVarsForClasses.get(classToCheck.getName()) == null)
                    if ((classToCheck.isInterface() || classToCheck.isAbstract())) {
                        SootMethod methodToCall = null;
                        //first look if a method may return the wanted class
                        Set<SootMethod> methodsToCheck = new HashSet<>();
                        methodsToCheck.addAll(methodsToCall);
                        methodsToCheck.addAll(doneMethodsToCall);

                        for (SootMethod method : methodsToCheck) {
                            Type returnType = method.getReturnType();
                            if (returnType instanceof RefType) {
                                if (isCompatible(((RefType) returnType).getSootClass(), classToCheck)) {
                                    Type baseRet = returnType instanceof ArrayType ? ((ArrayType) returnType).baseType : returnType;
                                    if (baseRet instanceof PrimType) {
                                        methodToCall = method;
                                        break;
                                    }

                                    //don't run into loops
                                    //happens if method return type are of the same type
                                    List<Type> methodsParameter = method.getParameterTypes();
                                    boolean methodNonRecursive = true;
                                    for (Type paraType : methodsParameter) {
                                        Type basePara = paraType instanceof ArrayType ? ((ArrayType) paraType).baseType : paraType;
                                        if(method == methodThatWouldBeCalled(basePara,constructionStack,parentClasses)){
                                            methodNonRecursive = false;
                                            methodToCall = null;
                                            break;
                                        }

                                        if (basePara == baseRet) {
                                            methodNonRecursive = false;
                                            methodToCall = null;
                                            break;
                                        }

                                    }
                                    if (methodNonRecursive) {
                                        methodToCall = method;
                                        break;
                                    }


                                }
                            }
                        }
                        if (methodToCall != null) {
                            Local classLocal = localVarsForClasses.get(classToCheck.getName());
                            LocalGenerator lg = new LocalGenerator(body);
                            if (classLocal == null) {
                                classLocal = lg.generateLocal(methodToCall.getDeclaringClass().getType());
                            }
                            Stmt stmt = buildMethodCall(methodToCall, body, classLocal, lg);
                            return ((AssignStmt) stmt).getLeftOp();
                        }
                        //create a dummy implementation of this Class or lookup if we already created one
                        // classToType = getDummyClass(classToCheck);

                    }


                    Value val = this.localVarsForClasses.get(classToType.getName());
                    if (val != null)
                        return val;

                }

                //if the class to instantiate is not exported replace by null
                if (!classToType.isExportedByModule()) {
                    return NullConstant.v();
                }
                // Create a new instance to plug in here
                Value val = generateClassConstructor(classToType, body, constructionStack, parentClasses);

                // If we cannot create a parameter, we try a null reference.
                // Better than not creating the whole invocation...
                if (val == null)
                    return NullConstant.v();

                return val;
            }
        } else if (tp instanceof ArrayType) {
            Value arrVal = buildArrayOfType(body, gen, (ArrayType) tp, constructionStack, parentClasses);
            if (arrVal == null) {
                logger.warn("Array parameter substituted by null");
                return NullConstant.v();
            }
            return arrVal;
        } else {
            logger.warn("Unsupported parameter type: {}", tp.toString());
            return null;
        }
        throw new RuntimeException("Should never see me");
    }


    private SootMethod methodThatWouldBeCalled(Type tp, Set<SootClass> constructionStack, Set<SootClass> parentClasses) {


        // Depending on the parameter type, we try to find a suitable
        // concrete substitution
        if (isSimpleType(tp.toString()))
            return null;
        else if (tp instanceof RefType) {
            SootClass classToType = ((RefType) tp).getSootClass();

            if (classToType != null) {
                SootClass originalClassToType = classToType;

                { //FIXME: hack for the stack
                    Local classLocal = localVarsForClasses.get(classToType.getName());
                    if (classLocal != null) {
                        return null;
                    }
                }

                //TODO: check if substituted class is in allowed exported classes

                // If we have a parent class compatible with this type and the class is exported, we use
                // it before we check any other option
                for (SootClass parent : parentClasses) {
                    if (isCompatible(parent, classToType) && parent.isExportedByModule()) {
                        Value val = this.localVarsForClasses.get(parent.getName());
                        if (val != null)
                            return null;
                    }

                }
                if (classToType.isAbstract() || classToType.isInterface()) {
                    //check if a public exported class implementing this interface or extending the abstract class exists
                    if (classToType.isInterface()) {

                        for (SootClass implementorOfSootClass : Scene.v().getActiveHierarchy().getImplementersOf(classToType)) {
                            if (!implementorOfSootClass.isAbstract() && implementorOfSootClass.isExportedByModule())
                                classToType = implementorOfSootClass;
                        }

                    } else {
                        for (SootClass sootSubClass : Scene.v().getActiveHierarchy().getSubclassesOf(classToType)) {
                            if (isCompatible(sootSubClass, classToType) && !sootSubClass.isAbstract() && sootSubClass.isExportedByModule())
                                classToType = sootSubClass;
                        }
                    }


                    SootClass classToCheck = classToType;
                    // always prefer non dummy classes, but look for methods that return appropriate type
                    if (getDummyClasses().contains(classToType)) {
                        classToCheck = originalClassToType;
                        Value val = this.localVarsForClasses.get(classToCheck.getName());
                        if (val != null)
                            return null;
                    }

                    //FIXME: always prefer methods
                    //if we did not find a concrete class look, check if a method has this as a return type
                    //((classToCheck.isInterface() || classToCheck.isAbstract()) && this.localVarsForClasses.get(classToCheck.getName()) == null)
                    if ((classToCheck.isInterface() || classToCheck.isAbstract())) {
                        SootMethod methodToCall = null;
                        //first look if a method may return the wanted class
                        Set<SootMethod> methodsToCheck = new HashSet<>();
                        methodsToCheck.addAll(methodsToCall);
                        methodsToCheck.addAll(doneMethodsToCall);

                        for (SootMethod method : methodsToCheck) {
                            Type returnType = method.getReturnType();
                            if (returnType instanceof RefType) {
                                if (isCompatible(((RefType) returnType).getSootClass(), classToCheck)) {
                                    Type baseRet = returnType instanceof ArrayType ? ((ArrayType) returnType).baseType : returnType;
                                    if (baseRet instanceof PrimType) {
                                        methodToCall = method;
                                        break;
                                    }

                                    //don't run into loops
                                    //happens if method return type are of the same type
                                    List<Type> methodsParameter = method.getParameterTypes();
                                    boolean methodNonRecursive = true;
                                    for (Type paraType : methodsParameter) {
                                        Type basePara = paraType instanceof ArrayType ? ((ArrayType) paraType).baseType : paraType;
                                        if (basePara == baseRet) {
                                            methodNonRecursive = false;
                                            methodToCall = null;
                                            break;
                                        }

                                    }
                                    if (methodNonRecursive) {
                                        methodToCall = method;
                                        break;
                                    }


                                }
                            }
                        }
                        if (methodToCall != null) {
                            Local classLocal = localVarsForClasses.get(classToCheck.getName());
                            // LocalGenerator lg = new LocalGenerator(body);
                            if (classLocal != null) {
                                return null;
                            }
                            // Stmt stmt = buildMethodCall(methodToCall, body, classLocal, lg);
                            return methodToCall;
                        }
                        //create a dummy implementation of this Class or lookup if we already created one
                        // classToType = getDummyClass(classToCheck);

                    }


                    Value val = this.localVarsForClasses.get(classToType.getName());
                    if (val != null)
                        return null;

                }

                //if the class to instantiate is not exported replace by null
                if (!classToType.isExportedByModule()) {
                    return null;
                }
                // Create a new instance to plug in here
                // Value val = generateClassConstructor(classToType, body, constructionStack, parentClasses);


            }
        } else if (tp instanceof ArrayType) {
            return methodThatWouldBeCalled(((ArrayType) tp).baseType, constructionStack, parentClasses);
        } else {
            logger.warn("Unsupported parameter type: {}", tp.toString());
            return null;
        }
        //throw new RuntimeException("Should never see me");
        //generate class constructor is invoked
        //this is in general no problem, since then we do not run into a loop
        return null;
    }


    public SootClass getDummyClass(SootClass toImplement){
        return getDummyClass(toImplement, this.dummyModuleInfo);

    }

    public SootClass getDummyClass(SootClass toImplement,SootModuleInfo moduleInfoToUse) {
        String packageName = toImplement.getJavaPackageName();

        String clzName = toImplement.getJavaStyleName();


        String dummyClassName = packageName + ".Dummy" + clzName;
        if (ModuleScene.v().containsClass(dummyClassName, Optional.of(moduleInfoToUse.getModuleName())))
            return ModuleScene.v().getSootClass(dummyClassName, Optional.of(moduleInfoToUse.getModuleName()));

        SootClass dummyClass = new SootClass(dummyClassName, moduleInfoToUse.getModuleName());
        //dummyClass.setModifiers(toImplement.getModifiers() ^ Modifier.ABSTRACT);
        dummyClass.setModifiers(Modifier.PUBLIC);

        //create the constructor
        SootMethod constructor = new SootMethod("<init>", Collections.emptyList(), VoidType.v());
        dummyClass.addMethod(constructor);
        JimpleBody body = Jimple.v().newBody(constructor);


        //for convenience we add this class to the existing module we are currently analyzing
        dummyClass.setModuleInformation(toImplement.getModuleInformation());


        // Add this reference
        body.insertIdentityStmts();
        //special invoke Object Initi
        //
        SootMethod method = Scene.v().getSootClass("java.lang.Object").getMethod("void <init>()");
        SpecialInvokeExpr expr = Jimple.v().newSpecialInvokeExpr(body.getThisLocal(),method.makeRef());
        Stmt invokeStmt = Jimple.v().newInvokeStmt(expr);
        body.getUnits().add(invokeStmt);
        Stmt ret = Jimple.v().newReturnStmt(body.getThisLocal());
        body.getUnits().add(ret);

        constructor.setActiveBody(body);




        /* handle multiple interfaces (or interface extends other)
         * if a class is an interface or is abstract
         */
        if (toImplement.isAbstract()) {

            //if class is an interface it might extend another interface, then we have to implement
            // the superclass methods as well

            //if class is abstract
            // a) it might implements several interfaces, that needs to be implemented

            // b) extends a superclass which is also abstract
            //b.2) and so on....
            // c) these superclasses might implements several interfaces

            HashSet<SootMethod> methodsToImplement = new HashSet<>();

            HashSet<SootClass> classesWhoseMethodsMustBeImplemented = new HashSet<>();
            SootClass classToVisit = toImplement;

            while (classToVisit.isAbstract()) {
                classesWhoseMethodsMustBeImplemented.add(classToVisit);
                classToVisit = classToVisit.getSuperclass();

            }

            for (SootClass classWhichMethodsMustBeImplemented : classesWhoseMethodsMustBeImplemented) {
                methodsToImplement.addAll(classWhichMethodsMustBeImplemented.getMethods());
                for (SootClass interfaceToImplement : classWhichMethodsMustBeImplemented.getInterfaces()) {
                    methodsToImplement.addAll(interfaceToImplement.getMethods());
                }


            }
            //above we might added methods which are already implemented but we catch this latter in the for loop for actually generating the methods

            if (toImplement.isInterface()) {
                dummyClass.addInterface(toImplement);
                dummyClass.setSuperclass(ModuleScene.v().getSootClass("java.lang.Object", Optional.of("java.base")));

            } else {
                //we have an abstract class
                dummyClass.setSuperclass(toImplement);
            }
            for (SootMethod parentMethod : methodsToImplement) {

                if (parentMethod.isAbstract()) { //if we have added to much methods above, we only generate methods for the abstract ones here
                    //the next if statement deals with name clashes of methods of several interfaces
                    if (dummyClass.declaresMethod(parentMethod.getName(), parentMethod.getParameterTypes(), parentMethod.getReturnType())) {
                        //a corresponding method  is already contained in the dummyClass; thus we don't need to generate another one
                        continue;
                    }
                    SootMethod generatedMethod = generateMethodImplementation(parentMethod, dummyClass);
                    dummyClass.addMethod(generatedMethod);
                }
            }
        }


        // First add class to scene, then make it an application class
        // as addClass contains a call to "setLibraryClass"
        Scene.v().addClass(dummyClass);
        dummyClass.setApplicationClass();
        //add these classes to the dummyClass set to get the Parameter passed in at callbacks
        this.dummyClasses.add(dummyClass);
        return dummyClass;
    }


    private SootMethod generateMethodImplementation(SootMethod methodToImplement, final SootClass generatedDummyClass) {
        SootMethod generatedMethod = new SootMethod(methodToImplement.getName(), methodToImplement.getParameterTypes(), methodToImplement.getReturnType());
        Body body = Jimple.v().newBody();
        body.setMethod(generatedMethod);
        generatedMethod.setActiveBody(body);

        // add locals for Parameter
        // Add a parameter reference to the body
        LocalGenerator lg = new LocalGenerator(body);

        //create a local for the this reference
        if (!methodToImplement.isStatic()) {
            Local thisLocal = lg.generateLocal(generatedDummyClass.getType());
            body.getUnits().addFirst(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(generatedDummyClass.getType())));
        }

        int i = 0;
        for (Type type : generatedMethod.getParameterTypes()) {
            Local paramLocal = lg.generateLocal(type);
            body.getUnits().add(Jimple.v().newIdentityStmt(paramLocal,
                    Jimple.v().newParameterRef(type, i)));
            i++;
        }

        JNopStmt startStmt = new JNopStmt();
        JNopStmt endStmt = new JNopStmt();

        body.getUnits().add(startStmt);


        //check if return type is void (check first, since next call includes void)
        if (methodToImplement.getReturnType() instanceof VoidType) {
            body.getUnits().add(Jimple.v().newReturnVoidStmt());
        }
        // if sootClass is simpleClass
        else if (isSimpleType(methodToImplement.getReturnType().toString())) {
            Local varLocal = lg.generateLocal(getSimpleTypeFromType(methodToImplement.getReturnType()));

            AssignStmt aStmt = Jimple.v().newAssignStmt(varLocal, getSimpleDefaultValue(methodToImplement.getReturnType().toString()));
            body.getUnits().add(aStmt);
            body.getUnits().add(Jimple.v().newReturnStmt(varLocal));
        } else {
            body.getUnits().add(Jimple.v().newReturnStmt(NullConstant.v()));

        }

        //remove the abstract Modifier from the new implemented method
        generatedMethod.setModifiers(methodToImplement.getModifiers() ^ Modifier.ABSTRACT);

        return generatedMethod;
    }

    protected Stmt buildMethodCall(SootMethod methodToCall, Body body,
                                   Local classLocal, LocalGenerator gen) {
        return buildMethodCall(methodToCall, body, classLocal, gen,
                Collections.<SootClass>emptySet());
    }


    @Override
    protected Stmt buildMethodCall(SootMethod methodToCall, Body body, Local classLocal, LocalGenerator gen, Set<SootClass> parentClasses) {
        assert methodToCall != null : "Current method was null";
        assert body != null : "Body was null";
        assert gen != null : "Local generator was null";

        if (classLocal == null && !methodToCall.isStatic()) {
            logger.warn("Cannot call method {}, because there is no local for base object: {}",
                    methodToCall, methodToCall.getDeclaringClass());
            getFailedMethods().add(methodToCall);
            return null;
        }

        final InvokeExpr invokeExpr;
        List<Value> args = new LinkedList<>();
        if (methodToCall.getParameterCount() > 0) {
            for (Type tp : methodToCall.getParameterTypes()) {
                Set<SootClass> constructionStack = new HashSet<>();
                if (!isAllowSelfReferences())
                    constructionStack.add(methodToCall.getDeclaringClass());

                args.add(getValueForType(body, gen, tp, constructionStack, parentClasses));
            }

            if (methodToCall.isStatic())
                invokeExpr = Jimple.v().newStaticInvokeExpr(methodToCall.makeRef(), args);
            else {
                assert classLocal != null : "Class local method was null for non-static method call";
                if (methodToCall.isConstructor()) {
                    invokeExpr = Jimple.v().newSpecialInvokeExpr(classLocal, methodToCall.makeRef(), args);
                }
                /*
                 *    adann: added Invoke Expression for interface methods (e.g., if a factory returns a reference
                 *    to an object implementing an interface or inheriting from an abstract class
                 */
                else if (methodToCall.getDeclaringClass().isInterface()) {
                    invokeExpr = Jimple.v().newInterfaceInvokeExpr(classLocal, methodToCall.makeRef(), args);
                } else {
                    invokeExpr = Jimple.v().newVirtualInvokeExpr(classLocal, methodToCall.makeRef(), args);
                }
            }
        } else {
            if (methodToCall.isStatic()) {
                invokeExpr = Jimple.v().newStaticInvokeExpr(methodToCall.makeRef());
            } else {
                assert classLocal != null : "Class local method was null for non-static method call";
                if (methodToCall.isConstructor()) {
                    invokeExpr = Jimple.v().newSpecialInvokeExpr(classLocal, methodToCall.makeRef());
                }
                /*
                 *    adann: added Invoke Expression for interface methods (e.g., if a factory returns a reference
                 *    to an object implementing an interface or inheriting from an abstract class
                 */
                else if (methodToCall.getDeclaringClass().isInterface()) {
                    invokeExpr = Jimple.v().newInterfaceInvokeExpr(classLocal, methodToCall.makeRef(), args);
                } else {
                    invokeExpr = Jimple.v().newVirtualInvokeExpr(classLocal, methodToCall.makeRef());
                }
            }
        }

        Stmt stmt;
        if (!(methodToCall.getReturnType() instanceof VoidType)) {
            Local returnLocal = gen.generateLocal(methodToCall.getReturnType());
            stmt = Jimple.v().newAssignStmt(returnLocal, invokeExpr);
            localVarsForClasses.put(returnLocal.getType().getEscapedName(), returnLocal);

        } else {
            stmt = Jimple.v().newInvokeStmt(invokeExpr);
        }
        body.getUnits().add(stmt);

        // Clean up
        for (Object val : args)
            if (val instanceof Local && ((Value) val).getType() instanceof RefType)
                body.getUnits().add(Jimple.v().newAssignStmt((Value) val, NullConstant.v()));

        return stmt;
    }


    protected Local generateClassConstructor(SootClass createdClass, Body body,
                                             Set<SootClass> constructionStack, Set<SootClass> parentClasses) {
        if (createdClass == null || this.failedClasses.contains(createdClass))
            return null;

        // We cannot create instances of phantom classes as we do not have any
        // constructor information for them
        if (createdClass.isPhantom() || createdClass.isPhantomClass()) {
            logger.warn("Cannot generate constructor for phantom class {}", createdClass.getName());
            failedClasses.add(createdClass);
            return null;
        }


        LocalGenerator generator = new LocalGenerator(body);

        // if sootClass is simpleClass:
        if (isSimpleType(createdClass.toString())) {
            Local varLocal = generator.generateLocal(getSimpleTypeFromType(createdClass.getType()));

            AssignStmt aStmt = Jimple.v().newAssignStmt(varLocal, getSimpleDefaultValue(createdClass.toString()));
            body.getUnits().add(aStmt);
            return varLocal;
        }

        { //FIXME: hack for the stack
            Local classLocal = localVarsForClasses.get(createdClass.getName());
            if (classLocal != null) {
                return classLocal;
            }
        }

        boolean isInnerClass = createdClass.getName().contains("$");
        String outerClass = isInnerClass ? createdClass.getName().substring
                (0, createdClass.getName().lastIndexOf("$")) : "";

        // Make sure that we don't run into loops
        if (!constructionStack.add(createdClass)) {
            //TODO: deactivated log to test speedup
//            logger.warn("Ran into a constructor generation loop for class " + createdClass
//                    + ", substituting with null...");
            Local tempLocal = generator.generateLocal(RefType.v(createdClass));
            AssignStmt assignStmt = Jimple.v().newAssignStmt(tempLocal, NullConstant.v());
            body.getUnits().add(assignStmt);
            return tempLocal;
        }
        if ((createdClass.isInterface() || createdClass.isAbstract()) && createdClass.isExportedByModule()) {

            // Find a matching implementor of the interface

            SootClass sootClassToGenerate = null;
            List<SootClass> classCandidates;
            if (createdClass.isInterface())
                classCandidates = Scene.v().getActiveHierarchy().getImplementersOf(createdClass);
            else
                classCandidates = Scene.v().getActiveHierarchy().getSubclassesOf(createdClass);

            // Generate an instance of the substitution class. If we fail,
            // try the next substitution. If we don't find any possible
            // substitution, we're in trouble
            outerloop:
            for (SootClass sClass : classCandidates) {
                if (isCompatible(sClass, createdClass) && !sClass.isAbstract() && sClass.isExportedByModule() && sClass.isExportedByModule()) {
                    for (SootMethod currentMethod : createdClass.getMethods()) {
                        if (!currentMethod.isConstructor()) {
                            continue;
                        }
                        sootClassToGenerate = sClass;
                        //TODO: check for non private constructor
                        break outerloop;
                    }
                }
            }

            if (sootClassToGenerate == null) {

                sootClassToGenerate = getDummyClass(createdClass);
            }
            Local cons = generateClassConstructor(sootClassToGenerate, body, constructionStack, parentClasses);
            if (cons == null) {

                logger.warn("Cannot create valid constructor for {}, because it is {} and cannot substitute with subclass", createdClass,
                        (createdClass.isInterface() ? "an interface" : (createdClass.isAbstract() ? "abstract" : "")));
                this.failedClasses.add(createdClass);
                //   throw new RuntimeException("No Class for " + createdClass);

            }
            return cons;
        } else {
            // Find a constructor we can invoke. We do this first as we don't want
            // to change anything in our method body if we cannot create a class
            // instance anyway.
            for (SootMethod currentMethod : createdClass.getMethods()) {
                if (!currentMethod.isConstructor())
                    continue;

                List<Value> params = new LinkedList<>();
                for (Type type : currentMethod.getParameterTypes()) {
                    // We need to check whether we have a reference to the
                    // outer class. In this case, we do not generate a new
                    // instance, but use the one we already have.
                    String typeName = type.toString().replaceAll("\\[\\]]", "");
                    if (type instanceof RefType
                            && isInnerClass && typeName.equals(outerClass)
                            && this.localVarsForClasses.containsKey(typeName))
                        params.add(this.localVarsForClasses.get(typeName));
                    else
                        params.add(getValueForType(body, generator, type, constructionStack, parentClasses));
                }

                // Build the "new" expression
                NewExpr newExpr = Jimple.v().newNewExpr(RefType.v(createdClass));
                Local tempLocal = generator.generateLocal(RefType.v(createdClass));
                AssignStmt assignStmt = Jimple.v().newAssignStmt(tempLocal, newExpr);
                body.getUnits().add(assignStmt);

                // Create the constructor invocation
                InvokeExpr vInvokeExpr;
                if (params.isEmpty() || params.contains(null))
                    vInvokeExpr = Jimple.v().newSpecialInvokeExpr(tempLocal, currentMethod.makeRef());
                else
                    vInvokeExpr = Jimple.v().newSpecialInvokeExpr(tempLocal, currentMethod.makeRef(), params);

                // Make sure to store return values
                if (!(currentMethod.getReturnType() instanceof VoidType)) {
                    Local possibleReturn = generator.generateLocal(currentMethod.getReturnType());
                    AssignStmt assignStmt2 = Jimple.v().newAssignStmt(possibleReturn, vInvokeExpr);
                    body.getUnits().add(assignStmt2);
                } else
                    body.getUnits().add(Jimple.v().newInvokeStmt(vInvokeExpr));

                return tempLocal;
            }

            logger.warn("Could not find a suitable constructor for class {}", createdClass.getName());
            super.failedClasses.add(createdClass);
            return null;
        }
    }

}
