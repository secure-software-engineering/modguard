#!/usr/bin/env bash
cd /home/adann/IdeaProjects/jdk_escape_analysis/examples_module_conf_int/case_01
echo $PWD
for folder in $(find . -type d -name 'modules'); do
    echo $folder
    java -cp /home/adann/IdeaProjects/jdk_escape_analysis/soot_jdk9_test/out/artifacts/soot_module_analysis_jar/soot-module-analysis.jar TestSootModuleConfIntAnalysis "/home/adann/module_analysis_int_conf_RESULTS" "de.upb.mod2" "/home/adann/IdeaProjects/jdk_escape_analysis/examples_module_conf_int/case_03/A2_return_stmt/modules"
done