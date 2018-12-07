#!/bin/bash 
set -x #echo on 
find . -name "*.class" -exec rm "{}" \; 
javac --module-path /home/adann/IdeaProjects/jdk_escape_analysis/examples_module_conf_int/myannotation_module -d ./modules --module-source-path ./modules $(find . -name "*.java")