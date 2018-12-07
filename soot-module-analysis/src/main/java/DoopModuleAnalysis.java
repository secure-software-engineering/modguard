import moduleanalysis.AbstractModuleAnalysis;
import moduleanalysis.ModuleAnalysisBuilder;
import moduleanalysis.criticalfinder.ReadFileCriticalFinder;
import moduleanalysis.utils.AnalysisReport;
import org.apache.commons.cli.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

/**
 * Created by adann on 29.12.17.
 */
public class DoopModuleAnalysis {

    final static String modulePathOpt = "modulePath";
    final static String moduleNameOpt = "moduleName";
    final static String logPathOpt = "logPath";
    final static String appendCPOpt = "appendCP";
    final static String ignoreArrayAndCollectionOpt = "ignoreArrayAndCollection";
    final static String appClassOpt = "onlyAppClass";
    final static String criticalEntitiesFileOpt = "criticalEntitiesFile";
    final static String doopReflectionOpt = "doopReflect";


    private static Options createOptions() {
        Options options = new Options();

        Option modulePath = OptionBuilder.withArgName("path")
                .hasArg()
                .withDescription("use given module path for analysis")
                .isRequired()
                .create(modulePathOpt);

        Option moduleName = OptionBuilder.withArgName("name")
                .hasArg()
                .withDescription("analyze given module")
                .isRequired()
                .create(moduleNameOpt);

        Option logfile = OptionBuilder.withArgName("logpath")
                .hasArg()
                .withDescription("use given path for log")
                .isRequired()
                .create(logPathOpt);

        Option appendCP = OptionBuilder.withArgName("appendCP")
                .hasArg()
                .withDescription("append to classpath")
                .create(appendCPOpt);


        Option critFile = OptionBuilder.withArgName("critFile")
                .hasArg()
                .withDescription("file specifying the critical entities")
                .create(criticalEntitiesFileOpt);

        Option doopReflection = new Option(doopReflectionOpt, false, "use Doop's Reflection Extension");


        Option ignoreArrayAndCollection = new Option(ignoreArrayAndCollectionOpt, false, "ignore arrays and collections");
        Option appClassOnly = new Option(appClassOpt, false, "analyze app classes only");


        //required options
        options.addOption(modulePath);
        options.addOption(moduleName);
        options.addOption(logfile);


        //optional
        options.addOption(appendCP);
        options.addOption(ignoreArrayAndCollection);
        options.addOption(appClassOnly);
        options.addOption(critFile);
        options.addOption(doopReflection);


        Option help = OptionBuilder.withLongOpt("help").withDescription("print this message").create("h");
        options.addOption(help);
        return options;
    }


    private static void showHelpMessage(String[] args, Options options) {
        Options helpOptions = new Options();
        Option help = OptionBuilder.withLongOpt("help").withDescription("print this message").create("h");

        helpOptions.addOption(help);
        try {
            CommandLine helpLine = new BasicParser().parse(helpOptions, args, true);
            if (helpLine.hasOption("help") || args.length == 0) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("DoopModuleAnalysis", options);
                System.exit(0);
            }
        } catch (ParseException ex) {
            System.err.println("Parsing failed.  Reason: " + ex.getMessage());
            System.exit(1);
        }
    }

    public static void main(String[] args) {

        Options options = createOptions();

        showHelpMessage(args, options);

        CommandLineParser parser = new BasicParser();
        CommandLine cmdLine = null;
        HelpFormatter formatter = new HelpFormatter();
        try {
            // parse the command line arguments
            cmdLine = parser.parse(options, args);


        } catch (ParseException exp) {
            // oops, something went wrong
            System.err.println("Parsing failed.  Reason: " + exp.getMessage() + "\n");
            formatter.printHelp("DoopModuleAnalysis", options);
            System.exit(1);

        }


        final String modulePathValue = cmdLine.getOptionValue(modulePathOpt);
        final String moduleNameValue = cmdLine.getOptionValue(moduleNameOpt);
        final String logPathValue = cmdLine.getOptionValue(logPathOpt);

        ModuleAnalysisBuilder builder = new ModuleAnalysisBuilder(moduleNameValue, modulePathValue);
        builder.setLogPath(logPathValue);

        //always do not box, boxing is done in Doop using Unified P/T Analysis
        builder.setBoxing(false);

        //check optional values
        if (cmdLine.hasOption(ignoreArrayAndCollectionOpt)) {
            boolean ignoreArraysAndCollections = Boolean.parseBoolean(cmdLine.getOptionValue(ignoreArrayAndCollectionOpt));
            builder.setIgnoreArrayCollection(true);
            System.out.println("Ignoring Collection: True");
        } else {
            builder.setIgnoreArrayCollection(false);

            System.out.println("Ignoring Collection: False");

        }

        if (cmdLine.hasOption(doopReflectionOpt)) {
            builder.useReflectionExtension(true);
            System.out.println("Use Doop's Reflection Extension: True");

        } else {
            builder.useReflectionExtension(false);
            System.out.println("Use Doop's Reflection Extension: False");

        }

        if (cmdLine.hasOption(appendCPOpt)) {
            String appendCPValue = cmdLine.getOptionValue(appendCPOpt);
            builder.appendToClassPath(appendCPValue);
        }


        if (cmdLine.hasOption(appClassOpt)) {
            ArrayList<String> excludes = new ArrayList<>();
            excludes.add("org.apache.tools.*");
            excludes.add("org.eclipse.*");
            excludes.add("java.*");
            excludes.add("sun.*");
            excludes.add("javax.*");
            excludes.add("com.sun.*");
            excludes.add("com.ibm.*");
            excludes.add("org.xml.*");
            excludes.add("org.w3c.*");
            excludes.add("apple.awt.*");
            excludes.add("com.apple.*");
            builder.excludes(excludes);
            System.out.println("Only Analyze Application Classes");

        }

        if (cmdLine.hasOption(criticalEntitiesFileOpt)) {
            String criticalFileValue = cmdLine.getOptionValue(criticalEntitiesFileOpt);
            builder.setCriticalFinder(new ReadFileCriticalFinder(new String[]{criticalFileValue}));
            System.out.println("Critical Entities File: " + criticalFileValue);

        }


        //ignore for TomcatAnalysis
        //  builder.appendToClassPath("/home/adann/IdeaProjects/jdk_escape_analysis/examples_module_conf_int/myannotation_module");


        AbstractModuleAnalysis doopModulesEscapeAnalysis = builder.buildDoopOnly();
        System.out.println("Starting Analysis of Module " + moduleNameValue);
        System.out.println(String.format("Name: %s, Path: %s, Log: %s", moduleNameValue, modulePathValue, logPathValue));

        try {
            doopModulesEscapeAnalysis.doAnalysis();
        } catch (IOException e) {
            e.printStackTrace();
        }


        //write the output
        Path dummyMainDir = Paths.get(logPathValue);
        if (!Files.exists(dummyMainDir)) {
            try {
                Files.createDirectory(dummyMainDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String fileName = "results_log.txt";
        Charset charset = Charset.defaultCharset();
        Path dummyOutput = dummyMainDir.resolve(fileName);
        if (!Files.exists(dummyOutput)) {
            try {
                Files.createFile(dummyOutput);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (BufferedWriter dummyOutPutStream = Files.newBufferedWriter(dummyOutput, charset, StandardOpenOption.APPEND)) {


            dummyOutPutStream.write("################################\n");

            dummyOutPutStream.write("Results for " + moduleNameValue + "\n");

            dummyOutPutStream.write("################################\n");


            Collection<AnalysisReport> methods = doopModulesEscapeAnalysis.getIdentifiedViolationMethodsFiltered();
            Collection<AnalysisReport> classes = doopModulesEscapeAnalysis.getIdentifiedViolationClassesFiltered();
            Collection<AnalysisReport> fiedls = doopModulesEscapeAnalysis.getIdentifiedViolationFieldsFiltered();

            dummyOutPutStream.write("Methods: " + methods.size());
            dummyOutPutStream.write("\n");
            dummyOutPutStream.write("Fields: " + fiedls.size());
            dummyOutPutStream.write("\n");
            dummyOutPutStream.write("Classes: " + classes.size());
            dummyOutPutStream.write("\n");


            dummyOutPutStream.write("-- Methods -- \n");

            for (Object res : methods) {
                dummyOutPutStream.write(res.toString());
                dummyOutPutStream.write("\n");

            }

            dummyOutPutStream.write("-- Fields -- \n");

            for (Object res : fiedls) {
                dummyOutPutStream.write(res.toString());
                dummyOutPutStream.write("\n");

            }
            dummyOutPutStream.write("-- Classes -- \n");
            for (Object res : classes) {
                dummyOutPutStream.write(res.toString());
                dummyOutPutStream.write("\n");

            }

        } catch (
                IOException e)

        {
            e.printStackTrace();
        }
    }
}
