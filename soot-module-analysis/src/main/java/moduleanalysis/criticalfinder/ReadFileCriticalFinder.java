package moduleanalysis.criticalfinder;

import soot.*;
import soot.jimple.spark.pag.SparkField;
import soot.options.Options;
import soot.tagkit.AnnotationTag;
import soot.tagkit.Host;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by adann on 26.09.17.
 */
public class ReadFileCriticalFinder implements ICriticalFinder {

    String[] files;


    private Set<String> methods = new HashSet<>();

    private Set<String> fields = new HashSet<>();

    private Set<String> classes = new HashSet<>();


    private HashSet<SootClass> criticalClasses = new HashSet<>();
    private HashSet<SparkField> criticalFields = new HashSet<>();
    private HashSet<SootMethod> criticalMethods = new HashSet<>();

    private boolean doneResolving;

    public ReadFileCriticalFinder(String[] files) {
        this.files = files;
        doneResolving = false;

    }


    @Override
    public void initialize() {

        for (String file : files) {
            Path fileToRead = Paths.get(file);


            try (InputStream in = Files.newInputStream(fileToRead);
                 BufferedReader reader =
                         new BufferedReader(new InputStreamReader(in))) {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    String readLine = line.trim();
                    if (readLine.startsWith("#")) {
                        //we have a comment
                        continue;
                    } else if (readLine.isEmpty()) {
                        continue;

                    } else if (readLine.contains("(")) {
                        //we have a method
                        methods.add(readLine);
                        continue;
                    } else if (readLine.contains(":")) {
                        //we have a field
                        fields.add(readLine);
                    } else {
                        //we have a class
                        classes.add(readLine);
                    }

                    System.out.println(line);
                }
            } catch (IOException x) {
                System.err.println(x);
            }


        }


    }

    @Override
    public Set<SparkField> getCriticalFields() {
        if (!doneResolving)
            resolve();
        return this.criticalFields;
    }

    @Override
    public Set<SootMethod> getCriticalMethods() {
        if (!doneResolving)
            resolve();
        return this.criticalMethods;
    }

    @Override
    public Set<SootClass> getCriticalClasses() {
        if (!doneResolving)
            resolve();
        return this.criticalClasses;
    }

    private void resolve() {
        if (doneResolving)
            return;
        if (Scene.v().getClasses().size() == 0)
            throw new RuntimeException("Scene is empty. Nothing to resolve");


        //resolve the classes
        for (String className : this.classes) {
            if (Scene.v().containsClass(className))
                this.criticalClasses.add(Scene.v().getSootClass(className));
        }

        //resolve the fields
        for (String fieldNameAndClass : this.fields) {
            String[] split = fieldNameAndClass.split(":");
            if (split.length != 2)
                throw new RuntimeException("Parsing error");
            String className = split[0].trim();
            String fieldName = split[1].trim();
            if (Scene.v().containsClass(className)) {
                SootClass sootClass = Scene.v().getSootClass(className);
                SootField field = sootClass.getFieldByName(fieldName);
                this.criticalFields.add(field);
            }

        }

        //resolve the methods
        for (String methodAndClassName : this.methods) {
            String[] split = methodAndClassName.split(":");
            if (split.length != 2)
                throw new RuntimeException("Parsing error");
            String className = split[0].trim().replace("<", "");
            String methodSig = split[1].trim().replace(">", "");
            if (Scene.v().containsClass(className)) {
                SootClass sootClass = Scene.v().getSootClass(className);
                SootMethod method = sootClass.getMethod(methodSig);
                this.criticalMethods.add(method);
            }

        }

        doneResolving = true;

    }
}
