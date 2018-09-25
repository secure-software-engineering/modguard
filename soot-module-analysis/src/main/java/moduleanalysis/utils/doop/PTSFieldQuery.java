package moduleanalysis.utils.doop;

import soot.PointsToSet;
import soot.SootField;
import soot.Type;
import soot.jimple.spark.sets.EmptyPointsToSet;

import java.util.ArrayList;

/**
 * Created by adann on 16.10.17.
 */
public class PTSFieldQuery extends PTSQueryObject {


    private static final String queryBodyFormat = " <- InstanceFieldPointsTo(%s,?heap,\"%s\",%s,%s), Value:Type[?heap]=?type, Value:Id[?heap]=?id.";
    private static final String headNameFormat = "_ptsField%d(?type,?heap,?id)";
    private ArrayList<PTSQueryAdapter.DoopNode> nodes = new ArrayList<>();

    public PTSFieldQuery(SootField field) {
        if (field.isStatic()) {
            throw new RuntimeException("Field is static");
        }
        String context = "_";
        String basecontext = "_";
        String basevalue = "_";
        String var = field.toString();
        ruleBody = String.format(queryBodyFormat, context, var, basecontext, basevalue);
        ruleHead = headNameFormat;
        sootObject = field;
    }

    public PointsToSet getPts() {
        if (pts == null) {
            if (nodes.isEmpty()) {
                pts = EmptyPointsToSet.v();
            } else {
                Type type = nodes.get(0).getType();
                PTSQueryAdapter.DoopPTS pts = new PTSQueryAdapter.DoopPTS(type);
                for (PTSQueryAdapter.DoopNode n : nodes) {
                    pts.add(n);
                }
                this.pts = pts;
            }
        }
        return pts;
    }

    public void parseOutput(String line) {
        String split[] = line.split(", ");
        if (split.length != 3)
            return;
        String type = split[0].trim();
        String heap = split[1].trim();
        String id = split[2].trim();

        PTSQueryAdapter.DoopNode node = PTSQueryAdapter.DoopNode.makeDoopNode(type, heap);
        node.setID(id);
        this.nodes.add(node);
    }

}
