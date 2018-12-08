# ModGuard:  Identifying Integrity & Confidentiality Violations in Java Modules

## Docker Image
To execute ModGuard and reporduce our results, we provide the docker image `andann/modguard:latest`

The instructions to use the docker image are in the folder `adann-ubuntu1604java9/README.md`.


## Building ModGuard

For building Modguard the following software needs to be installed: Java1.8, Maven3, bash, git.


Install PA-Datalog by following the instructions on http://snf-705535.vm.okeanos.grnet.gr/agreement.html.


To build Modguard cd into the project's root directory and execute `sh build.sh`.
The compiled jar files are stored in the folder `build`.


Install Doop by unpacking `doop-4.0.0.tar` into your home-directory and export `DOOP_HOME` to Doop's install folder.



## Running ModGuard

To execute ModGuard with your own module run

```
java -cp /opt/jdk-9/lib/jrt-fs.jar:soot-module-analysis-1.0-SNAPSHOT.jar DoopModuleAnalysis -moduleName {0} -modulePath {1} -logPath {2} -appendCP {3} --criticalEntitiesFile {4}  -onlyAppClass -ignoreArrayAndCollection

```
with

{0} - the name of the module to check

{1} - the module-path ModGuard should use to locate the module with the given name

{2} - where to store generated log files

{3} - (optionally) further jar files that should also be loaded into ModGuard

{4} - a simple *.txt file containing the fully-qualified names of the sensitive entities that should not escape a module; one per line, e.g., `<org.apache.tomcat.util.http.fileupload.MultipartStream: void setBoundary(byte[])>`
This are the sensitve entities that are checked by ModGuard.

