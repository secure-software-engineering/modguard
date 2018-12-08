#!/bin/bash
#current dir
DIR=$(pwd)

# path to project
PROJECT_PATH="$DIR/.."


#PATH to DOOP
DOOP_PATH="${PROJECT_PATH}/doop"
DOOP_BUILD="${PROJECT_PATH}/doop/build/distributions/doop-4.0.0.tar"

# path to lib and project files
SOOT_PATH="${PROJECT_PATH}/soot"
SOOT_BUILD="${SOOT_PATH}/target/sootclasses_j9-trunk.jar"
MODULE_ANALYSIS_BUILD="${PROJECT_PATH}/soot-module-analysis/target/soot-module-analysis-1.0-SNAPSHOT.jar"
MY_SOOT_INFOFLOW="${PROJECT_PATH}/soot-infoflow/target/soot-infoflow-2.0.0-SNAPSHOT.jar"


# build and install soot first
cd "$SOOT_PATH"
git checkout modguard1.0
mvn clean compile install -DskipTests

cd "$DIR"

# build  my doop version
cd "$DOOP_PATH"
git checkout ModGuard 
./gradlew clean
./gradlew distTar
./gradlew publishToMavenLocal

# copy doop dist to files
cp "$DOOP_BUILD" "$DIR"


# execute mvn build of jdk9 analysis
cd "$PROJECT_PATH"
mvn -DskipTests clean compile package install
#build analysis jar with dependencies
mvn -DskipTests clean compile assembly:single -pl soot-module-analysis
#copy artifact
cp "$MODULE_ANALYSIS_BUILD" "$DIR/files/"






# build the documentation
cd "$DIR"
pandoc -s -S --toc -c -H pandoc.css README.md -o README.html
cp "README.md" "./files/"




#build the docker images

docker build -t andann/modguard:latest .


#remove the artifacts
cd "$DIR"
rm "$DIR/doop-4.0.0.tar"
rm "$DIR/files/README.md"
rm "$DIR/README.html"
rm "$DIR/files/soot-module-analysis-1.0-SNAPSHOT.jar"
#rm "$DIR/files/OUR_RESULTS.tar.gz"
