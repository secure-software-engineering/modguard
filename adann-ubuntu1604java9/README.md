# Artifact: ModGuard: Identifying Integrity & Confidentiality Violations in Java 9 Modules

__MISSING SOURCES IN CONTAINER__

## Artifact Description
We provide the following artifacts:

* The results of ModGuard and SuSi in folder `OUR_RESULTS`:
	* gathered from running SuSi against Tomcat `OUR_RESULTS/SuSi`
	* gathered from running ModGuard against the naive modularization of Tomcat `OUR_RESULTS/Tomcat/doop_naive_tomcat_results.txt`
	* gathered from running ModGuard against the strict modularization of Tomcat `OUR_RESULTS/Tomcat/doop_strict_tomcat_results.txt`
* The source code of our implementation
* MIC9Bench - The benchmark for checking integrity and confidentiality of Java 9 modules
* A docker container which contains the compiled version of ModGuard's implementation, and a python script to re-run our evaluation



## Setup
* Install docker on your machine
* Pull our docker image using:

		docker pull andann/pldi2018:evaluation

* After pulling the image it can be run using:

		docker run --shm-size="6g" -it andann/pldi2018:evaluation /bin/bash

* You will find yourself in a bash session in the directory of the evaluation project



## Reproducing the Evaluation
* To reproduce the paper's evaluation you must use the following command:

		python3 evaluation.py CMD 


* The mandatory argument `CMD` can be one of the following:
	* __susi__ 
	: runs SuSi on Tomcat to identify critical methods, results are stored in the folder `SuSi_RESULTS`

	* __tomcatNaive__ 
	: runs ModGuard on the naive modularization of Tomcat stored in `tomcat_modularization_2017_09_19/TOMCAT_8_5_21/java9_modules` and compiled in `tomcat_modularization_2017_09_19/TOMCAT_8_5_21/output/` results are stored in `Tomcat_RESULTS/naiveTomcat/` 

	* __tomcatStrict__ 
	: runs ModGuard on the strict modularization of Tomcat stored in `tomcat_modularization_2017_09_19/TOMCAT_8_5_21/java9_modules_minExp` and compiled in `tomcat_modularization_2017_09_19/TOMCAT_8_5_21/outputMin/` results are stored in `Tomcat_RESULTS/strictTomcat/` 

	* __tomcat__ 
	: runs ModGuard on the naive and strict modularization of Tomcat, same as running `tomcatNaive` and `tomcatStrict`

	* __clean__ 
	: removes the generated results and entry-points

	* __test__
	:  runs ModGuard on the benchmark MIC9Bench

	* __all__
	:  runs susi, tomcat, and test

	* __--help__
	:  shows help and usage information


For instance, to execute ModGuard's analysis on the naive modularization of Tomcat use:
	
	python3 evaluation.py tomcatNaive


To execute the complete evaluation use:
	
	python3 evaluation.py all

__Note:__ The analysis may take >2 h and >16 GB memory each.


To copy the file to your host system for subsequent analyses use:

	docker <container-id>:/root/Tomcat_RESULTS <path-on-your-machine>
	docker <container-id>:/root/SuSi_RESULTS <path-on-your-machine>


## Folders Explained / Evaluation Environment
* `OUR_RESULTS`
:  the results of our experiments
	* `Tomcat`
	: the results of ModGuard's Tomcat analysis
		* `doop_naive_tomcat_results.txt` results of Tomcat's naive modularization (all packages are exported)
		* `doop_strict_tomcat_results.txt` results of Tomcat's strict modularization (only required packages are exported)

	* `SuSi`
	: the results of SuSi run on Tomcat




* `tomcat_modularization_2017_09_19`
:	the modularized version of apache Tomcat 8.5.21 <https://tomcat.apache.org/>
  	* the file `modularization.txt` describes our process for modularizing Tomcat 
			* `java9_modules`
			: the naive modularization of Tomcat

			* `output/classes`
			: the compiled naive modularization

			* `buildj9.xml`
			:  ant file to build the naive modularization

			* `java9_modules_minExp`
			: the strict modularization of Tomcat

			* `outputMin/classes`
			: the compiled strict modularization

			* `buildj9_minExp.xml`
			:  ant file to build the strict modularization


* `SuSi`
:  the machine learning framework SuSi to identify critical methods in Tomcat
	* the file `SuSi_Input_methodsThrowingSecurityExcepetionsJDK8.pscout` contains all method of the JDK8 that may throw SecurityExceptions
	* the file `SuSi_Input_methodsThrowingSecurityExcepetionsJDK8.pscout` is used as an input for SuSi

* `mic9bench`
:  the MIC9Bench - An Integrity and Confidentiality for Java 9 Modules Analysis Benchmark Suite, to compile it run `ant compile`, compiled `class` files are stored in the folder `build`


* `switch_java`
: bash script to swith between jdk8 and jdk9, to switch between jdks run `sh switch_java 8` or `sh swith_java 9`
	* jdk8 is used to run the evalution and Doop
	* jdk9 can be used to re-compile the modularized version of Tomcat
	




