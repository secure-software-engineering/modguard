import argparse
import sys
import os
import shutil
import git
import datetime

import evalUI

#directories
DIR_HOME = os.path.expanduser('~')
DIR_DOOP_HOME = os.path.join(DIR_HOME,"doop-4.0.0")
DIR_TOMCAT = os.path.join(DIR_HOME, "tomcat_modularization_2017_09_19", "TOMCAT_8_5_21")
DIR_MIC9BENCH = os.path.join(DIR_HOME, "mic9bench")
DIR_SUSI = os.path.join(DIR_HOME, "SuSi")

DIR_MODGUARD = DIR_HOME

#result directories
DIR_RESULTS_MIC9BENCH = os.path.join(DIR_HOME, "MIC9Bench_RESULTS")
DIR_RESULTS_TOMCAT = os.path.join(DIR_HOME, "Tomcat_RESULTS")
DIR_RESULTS_TOMCAT_NAIVE = os.path.join(DIR_RESULTS_TOMCAT, "naiveTomcat")
DIR_RESULTS_TOMCAT_STRICT = os.path.join(DIR_RESULTS_TOMCAT, "strictTomcat")

DIR_RESULTS_SUSI = os.path.join(DIR_HOME, "SuSi_RESULTS")



#mic9Bench Repo and Revision
MIC9BENCH_REPO = "https://github.com/secure-software-engineering/mic9bench.git"



#invoke command
#java -cp lib/weka.jar:soot-trunk.jar:soot-infoflow.jar:soot-infoflow-android.jar:SuSi.jar de.ecspride.sourcesinkfinder.SourceSinkFinder android.jar permissionMethodWithLabel.pscout out.pscout 
CONST_SUSI_INVOKE = "java -cp lib/weka.jar:lib/soot-trunk.jar:lib/soot-infoflow.jar:lib/soot-infoflow-android.jar:SuSi.jar de.ecspride.sourcesinkfinder.SourceSinkFinder {0} {1} {2}"


CONST_MODGUARD_MIC9BENCH_INVOKE = "java -cp /opt/jdk-9/lib/jrt-fs.jar:soot-module-analysis-1.0-SNAPSHOT.jar DoopModuleAnalysis -moduleName {0} -modulePath {1} -logPath {2} -appendCP {3}"
CONST_MODGUARD_TOMCAT_INVOKE = "java -cp /opt/jdk-9/lib/jrt-fs.jar:soot-module-analysis-1.0-SNAPSHOT.jar DoopModuleAnalysis -moduleName {0} -modulePath {1} -logPath {2} -appendCP {3} --criticalEntitiesFile {4}  -onlyAppClass -ignoreArrayAndCollection"


CONST_SOOT_JAR = "soot-module-analysis.jar"



#java/soot options

LOG_LEVEL = "-Dorg.slf4j.simpleLogger.defaultLogLevel=error"








# Clean up/delete the generated results

def cleanTomcat():
    print("Clean up Tomcat results:")
    if os.path.exists(DIR_RESULTS_TOMCAT):
        for the_file in os.listdir(DIR_RESULTS_TOMCAT):
            file_path = os.path.join(DIR_RESULTS_TOMCAT, the_file)
            try:
                if os.path.isfile(file_path):
                    os.unlink(file_path)
                elif os.path.isdir(file_path): 
                    shutil.rmtree(file_path)
            except Exception as e:
                print(e)


def cleanMIC9Bench():
    print("Clean up MIC9Bench results:")
    if os.path.exists(DIR_RESULTS_MIC9BENCH):
        for the_file in os.listdir(DIR_RESULTS_MIC9BENCH):
            file_path = os.path.join(DIR_RESULTS_MIC9BENCH, the_file)
            try:
                if os.path.isfile(file_path):
                    os.unlink(file_path)
                elif os.path.isdir(file_path): 
                    shutil.rmtree(file_path)
            except Exception as e:
                print(e)


def cleanMIC9BenchGit():
    print("Clean up MIC9Bench Repo:")
    if os.path.exists(DIR_MIC9BENCH):
        shutil.rmtree(DIR_MIC9BENCH)


def cleanSuSi():
    print("Clean up SuSi results:")
    if os.path.exists(DIR_RESULTS_SUSI):
        for the_file in os.listdir(DIR_RESULTS_SUSI):
            file_path = os.path.join(DIR_RESULTS_SUSI, the_file)
            try:
                if os.path.isfile(file_path):
                    os.unlink(file_path)
                elif os.path.isdir(file_path): 
                    shutil.rmtree(file_path)
            except Exception as e:
                print(e)  

def clean_all():
    cleanMIC9Bench()
    cleanMIC9BenchGit()
    cleanTomcat()
    cleanSuSi()



# run susi of the wala analysis
def run_susi():
    os.chdir(DIR_SUSI)
    print("Build SuSi")
    cmdLineCall = 'ant jar'
    shutil.copy(os.path.join(DIR_SUSI,"build","jar","SuSi.jar"), DIR_SUSI )
    cmdLineCall = CONST_SUSI_INVOKE.format("jdk_tomcat_input.jar", "SuSi_Input_methodsThrowingSecurityExcepetionsJDK8.pscout", os.path.join(DIR_RESULTS_SUSI,"results.out"))
    print(cmdLineCall)
    os.system(cmdLineCall)





def run_tomcat(naive=True):
    tomcatPath = ""
    logPath = ""
    if naive:
        tomcatPath = os.path.join(DIR_TOMCAT, "output", "classes")
        logPath = DIR_RESULTS_TOMCAT_NAIVE
    else:
        tomcatPath = os.path.join(DIR_TOMCAT, "outputMin", "classes")
        logPath = DIR_RESULTS_TOMCAT_STRICT
    #find all modules
    dirs = os.listdir(tomcatPath)
    print("Found modules: ")
    print(*dirs, sep='\n')

    for dir in sorted(dirs):
        if os.path.isfile(os.path.join(tomcatPath, dir)):
            continue
        #cmd for invokation
        #logPath = os.path.join(DIR_RESULTS_TOMCAT)
        if not os.path.exists(logPath):
            os.makedirs(logPath)


        modulePath = os.path.join(tomcatPath, dir)
        moduleName = dir
        #FIXME: append path correct
        appendCP = "{0}:/root/ecj-4.5.1.jar".format(tomcatPath)
        criticalEntitiesFile = os.path.join(DIR_HOME,"OUR_RESULTS","SuSi","Tomcat_critical_Methods_Classes_Fields.txt")
        #execute the analysis/modguard
        os.chdir(DIR_MODGUARD)


        #log statics
        logStatics = os.path.join(logPath,"stats_{}.txt".format(moduleName))
        cmdLineCall = CONST_MODGUARD_TOMCAT_INVOKE.format(moduleName, modulePath, logPath, appendCP,criticalEntitiesFile)
        cmdLineCall = "/usr/bin/time -v -o {0} {1}".format(logStatics, cmdLineCall)

        print(cmdLineCall)
        os.system(cmdLineCall)


def run_tomcatNaive():
    run_tomcat(True)

def run_tomcatStrict():
    run_tomcat(False)

def run_all():
    run_susi()
    run_tomcat(True)
    run_tomcat(False)
    run_testcases()


def run_testcases():
    if not os.path.exists(DIR_MIC9BENCH):
        #clone the repo
        cloned_repo = git.Repo.clone_from(MIC9BENCH_REPO,os.path.join(DIR_HOME, "mic9bench"))   
        if cloned_repo.bare:
            print("[Error] Cloning {0} failed.".format(MIC9BENCH_REPO), file=sys.stderr)
            if os.path.exists(repoPath):
                shutil.rmtree(repoPath)
            sys.exit(2)
    else:
        repo = git.Repo(os.path.join(DIR_HOME, "mic9bench"))
        origin = repo.remotes.origin
        origin.pull()

    ## code in java
    #   for (File f : files) {
     #       String targetPath = "/home/adann/module_analysis_int_conf_RESULTS" + File.separator + f.getName();
    #        String modulePath = f.getAbsolutePath() + File.separator + "modules";
    #        String moduleName = "de.upb.mod2";
    #        parameters[i] = new Object[]{targetPath, modulePath, moduleName};
     #       i++;
    #    }
    ret = 0
    #switch to java 9 for compilation
    print(DIR_HOME)
    os.chdir(DIR_HOME)
    if ret != 0:
        print("[Error] Could not cd to Home-Dir. Exit.", file=sys.stderr)
        sys.exit(1)
    cmdLineCall = "bash switch_java 9"
    print(cmdLineCall)
    ret = os.system(cmdLineCall)
    if ret != 0:
        print("[Error] Could not swith to Java 9. Exit.", file=sys.stderr)
        sys.exit(1)
    #compile the tests
    os.chdir(DIR_MIC9BENCH)
    cmdLineCall = 'ant compile'
    print(cmdLineCall)
    ret = os.system(cmdLineCall)
    if ret != 0:
        print("[Error] Could not compile Tomcat. Exit.", file=sys.stderr)
        sys.exit(1)



    #switch back to java 8 for execution and doop
    os.chdir(DIR_HOME)
    cmdLineCall = "bash switch_java 8"
    print(cmdLineCall)
    ret = os.system(cmdLineCall)
    if ret != 0:
        print("[Error] Could not switch to Java 8. Exit.", file=sys.stderr)
        sys.exit(1)
    buildDir = os.path.join(DIR_MIC9BENCH, "build")

    #find all test cases
    dirs = os.listdir(buildDir)
    # show the found tests
    print("Found tests: ")
    print(*dirs, sep='\n')


    #start sar
    #os.system("systemctl sar start")



    #execute the tests
    for dir in sorted(dirs):
        if os.path.isfile(os.path.join(buildDir, dir)):
            continue
        if "mic9bench.annotation" in dir:
            continue
        currentFolder = os.path.join(buildDir, dir)    
        subdirs = os.listdir(currentFolder) 
        counter = 0   
        for subdir in sorted(subdirs):
            counter+=1
            #create result dir
            logPath = os.path.join(DIR_RESULTS_MIC9BENCH, dir, subdir)
            if not os.path.exists(logPath):
                os.makedirs(logPath)
            modulePath = os.path.join(buildDir, dir, subdir)
            #fixme adapt module name
            moduleName = "mic9.mod"
            #FIXME: append path correct
            appendCP = os.path.join(buildDir,"mic9bench.annotation")
            #execute the analysis/modguard
            os.chdir(DIR_MODGUARD)
            cmdLineCall = CONST_MODGUARD_MIC9BENCH_INVOKE.format(moduleName, modulePath, logPath, appendCP)
            # start time
        #    ts = datetime.datetime.now()
            logStatics = os.path.join(logPath,"stats_{0}.txt".format(counter))
            cmdLineCall = "/usr/bin/time -v -o {0} {1}".format(logStatics, cmdLineCall)

            print(cmdLineCall)

            os.system(cmdLineCall)
            #end time
         #   tf = datetime.datetime.now()
            # difference
         #   te = tf - ts



    #stop sar
    #os.system("systemctl sar stop")






def setUpDirs():
    if not os.path.exists(DIR_RESULTS_SUSI):
        os.makedirs(DIR_RESULTS_SUSI)
    if not os.path.exists(DIR_RESULTS_TOMCAT):
        os.makedirs(DIR_RESULTS_TOMCAT)
    if not os.path.exists(DIR_RESULTS_MIC9BENCH):
        os.makedirs(DIR_RESULTS_MIC9BENCH)  

## the dictionary


cmdDict =  {}
cmdDict['all'] = (run_all, "Run SuSi, and  ModGuard on Tomcat, and mic9Bench")
cmdDict['tomcatNaive'] = (run_tomcatNaive, "Run ModGuard on naive Tomcat")
cmdDict['tomcatStrict'] = (run_tomcatStrict, "Run ModGuard on strict Tomcat")
cmdDict['susi'] = (run_susi, "Run SuSi...")
cmdDict['clean'] = (clean_all, "Clean up all generated results")
cmdDict['mic9Bench'] = (run_testcases, "Run ModGuard on MIC9Bench")



def check_arg(args=None):
    # generate the help text
    helpText = "Select the Analysis to run: \n"
    for key, value in sorted(cmdDict.items()):
        helpText += "\t{0}: {1} \n".format(str(key), str(value[1]))

#'Select the Analysis to run: \n all: run SuSi, ModGuard on tomcat, and mic9Bench \n susi: run SuSi to identify critical methods in Tomcat \n tomcatNaive: run ModGuard on naive Tomcat \n tomcatStrict: run ModGuard on strict Tomcat \n tomcat: run ModGuard on naive & strict Tomcat \n clean: clean up all generated results \n mic9Bench: run ModGuard on MIC9Bench'
    parser = argparse.ArgumentParser(description='Script to start ModGuard evaluation',  usage='use "%(prog)s --help" for more information',   formatter_class=argparse.RawTextHelpFormatter)
    parser.add_argument('cmd', action='store' , nargs='?',  choices=['all', 'tomcat', 'tomcatNaive', 'tomcatStrict', 'susi', 'clean', 'mic9Bench', 'gui'], 
                      help=helpText)
    parser.add_argument("--reflection", help="use Doop's Reflection Extension",  action="store_true")
    args = parser.parse_args()
    return args.cmd, parser, args






if __name__ == '__main__':
    cmd, parser, args = check_arg(sys.argv[1:])
    if args.reflection:
    	print("reflection turned on")
    	CONST_MODGUARD_MIC9BENCH_INVOKE = CONST_MODGUARD_MIC9BENCH_INVOKE + " -doopReflect"
    	CONST_MODGUARD_TOMCAT_INVOKE = CONST_MODGUARD_TOMCAT_INVOKE + " -doopReflect"
    print("Home-Dir: {}".format(DIR_HOME))
    print("Selected Command: {}".format(cmd))
    setUpDirs()
    myUI = evalUI.EvalUI(cmdDict,"MyEval")
    if cmd == None:
        parser.print_help()
    elif cmd == 'gui':
        #start the gui   
        print("Start ui")
        myUI.start()
        cmd = myUI.selectedCmd
        if cmd:
            cmdDict[cmd][0]()

    else:
        text = cmdDict[cmd][1]
        print(str(text))
        cmdDict[cmd][0]()
