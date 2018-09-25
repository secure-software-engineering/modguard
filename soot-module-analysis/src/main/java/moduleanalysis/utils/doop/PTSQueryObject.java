package moduleanalysis.utils.doop;

import groovy.lang.Closure;
import org.clyze.analysis.Analysis;
import soot.PointsToSet;
import soot.util.Numberable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by adann on 16.10.17.
 */
public abstract class PTSQueryObject {
    private String query;
    protected String ruleBody;
    protected String ruleHead;
    protected PointsToSet pts;

    protected Numberable sootObject;

    public String getQuery() {
        if (query == null || query.isEmpty())
            query = ruleHead + ruleBody;
        return query;
    }

    public Numberable getSootObject() {
        return sootObject;
    }


    public abstract PointsToSet getPts();

    private static final String writeQueriesIntoFile(Collection<? extends PTSQueryObject> queries, String fileName, Analysis doopAnalysis) {
        File outDir = doopAnalysis.getOutDir();

        File logicFile = new File(outDir + File.separator + fileName);
        if (logicFile.exists()) {

            logicFile.delete();

        }
        try {
            logicFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (BufferedWriter writer = Files.newBufferedWriter(logicFile.toPath())) {
            String line = "";
            for (PTSQueryObject queryObject : queries) {
                line = queryObject.getQuery();
                writer.write(line);
                writer.write("\n");
            }
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }


        return logicFile.getAbsolutePath();
    }

    public abstract void parseOutput(String line);


    public static void execute(Collection<PTSQueryObject> queries, Analysis doopAnalysis) {
        if (queries == null || queries.isEmpty()) {
            return;
        }
        if (queries.size() == 1) {
            PTSQueryObject query = (PTSQueryObject) queries.toArray()[0];
            execute(query, doopAnalysis);
            return;
        }

        HashMap<String, PTSQueryObject> headNameMap = new HashMap<>();


        int i = 0;
        for (PTSQueryObject queryObject : queries) {
            String headName = String.format(queryObject.ruleHead, i);
            queryObject.ruleHead = headName;
            i++;
            //FIXME: this is not so nice but it works
            String headNameForMap = headName.substring(0, headName.indexOf("("));
            headNameMap.put(headNameForMap, queryObject);
        }

        //write to file
        String filename = writeQueriesIntoFile(queries, "ptsQuery.logic", doopAnalysis);

        Closure myCl = new Closure(null) {

            PTSQueryObject queryObject;

            @Override
            public Object call(Object... args) {
                String line = (String) args[0];
                //get the QueryObject with the corresponding rule name
                if (line.startsWith("+")) {
                    String ruleHeadName = line.replace("+", "").trim();
                    queryObject = headNameMap.get(ruleHeadName);
                    return null;
                }
                if (queryObject == null)
                    return null;

                //we get the output as !one! arg??? because it is read from STDOUT?
                //thus we split it here
                queryObject.parseOutput(line);

                return null;
            }
        };


        String queryToFile = "-file " + filename;
        doopAnalysis.processRelation(queryToFile, myCl);
    }


    public static void execute(PTSQueryObject queryObject, Analysis doopAnalysis) {


        String headName = String.format(queryObject.ruleHead, 0);
        queryObject.ruleHead = headName;


        Closure myCl = new Closure(null) {


            @Override
            public Object call(Object... args) {
                String line = (String) args[0];
                //we get the output as !one! arg??? because it is read from STDOUT?
                //thus we split it here
                queryObject.parseOutput(line);

                return null;
            }
        };


        String queryToExecute = "'"+queryObject.getQuery()+"'";
        doopAnalysis.processRelation(queryToExecute, myCl);
    }


}
