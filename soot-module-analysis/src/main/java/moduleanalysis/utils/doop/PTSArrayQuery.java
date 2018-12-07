package moduleanalysis.utils.doop;

import soot.PointsToSet;
import soot.Type;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.sets.EmptyPointsToSet;
import soot.jimple.spark.sets.P2SetVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by adann on 16.10.17.
 */
public class PTSArrayQuery extends PTSQueryObject {
    private PTSQueryAdapter.DoopPTS arrayPTS;
    private List<Integer> hctxs;
    private List<Type> types;
    private List<String> ids;

    private String baseArrayValue;
    private ArrayList<PTSQueryAdapter.DoopNode> nodes = new ArrayList<>();


    private static final String queryBodyFormat = "  <- ArrayIndexPointsTo(?ctx, ?heap, _, ?basearray), Value:Type[?heap]=?type, Value:Id[?basearray]=\"%s\".";
    private static final String headNameFormat = "_ptsArray%d(?ctx,?type,?heap,?basearray)";


    public PTSArrayQuery(PTSQueryAdapter.DoopPTS arrayPTS) {
        this.arrayPTS = arrayPTS;
        hctxs = new ArrayList<>();
        types = new ArrayList<>();
        ids = new ArrayList<>();

        arrayPTS.forall(new P2SetVisitor() {
            @Override
            public void visit(Node n) {
                if (n instanceof PTSQueryAdapter.DoopNode) {
                    hctxs.add(((PTSQueryAdapter.DoopNode) n).getHeap());
                    ids.add(((PTSQueryAdapter.DoopNode) n).getID());
                    types.add(n.getType());
                }
            }
        });
        String id = ids.get(0);
        ruleBody = String.format(queryBodyFormat, id);
        // ruleBody = queryBodyFormat;
        ruleHead = headNameFormat;
        baseArrayValue = "*" + hctxs.get(0);
        sootObject = null;
    }


    @Override
    public PointsToSet getPts() {
        if (pts == null) {
            if (nodes.isEmpty()) {
                pts = EmptyPointsToSet.v();
            } else {
                //get the type for the new created pts
                // must be done per local ...
                //FIXME: for now a dirty little hack
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

    @Override
    public void parseOutput(String line) {
        String split[] = line.split(", ");
        if (split.length != 4)
            return;
        String ctx = split[0].trim();
        String type = split[1].trim();
        String heap = split[2].trim();
        String basearray = split[3].trim();
        if (!basearray.equals(baseArrayValue)) {
            return;
        }

        PTSQueryAdapter.DoopNode node = PTSQueryAdapter.DoopNode.makeDoopNode(type, heap);
        this.nodes.add(node);

    }
}
