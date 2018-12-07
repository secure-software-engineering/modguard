package callgraph.adapter.jgrapht;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import soot.MethodOrMethodContext;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.*;

/**
 * Created by adann on 25.10.16.
 */
public class SootCallGraphAdapter implements DirectedGraph<Object, Edge> {

    private CallGraph callGraph;

    private Set<Object> methodVertexSet;


    public SootCallGraphAdapter(CallGraph callGraph) {

        this.callGraph = callGraph;
        this.vertexSet();
    }

    @Override
    public int inDegreeOf(Object obj) {

        if(obj instanceof SootMethod) {
            SootMethod method =(SootMethod) obj;
            int i = 0;
            Iterator<Edge> it = callGraph.edgesInto(method);
            while (it.hasNext()) {
                i++;
                it.next();
            }

            return i;
        }
        else{
            throw  new RuntimeException("Not a method");
        }
    }

    @Override
    public Set<Edge> incomingEdgesOf(Object obj) {
        if(obj instanceof SootMethod){
            SootMethod method = (SootMethod) obj;
            Set<Edge> edges = new HashSet<>();
            Iterator<Edge> it = callGraph.edgesInto(method);
            while (it.hasNext()) {
                edges.add(it.next());
            }
            return edges;
        }
        else if(obj instanceof Unit){
            throw new RuntimeException("Eroor");
        }

        else{
            throw new RuntimeException("Eroor");
        }


    }

    @Override
    public int outDegreeOf(Object obj) {

        if(obj instanceof  SootMethod) {
            SootMethod method = (SootMethod) obj;
            int i = 0;
            Iterator<Edge> it = callGraph.edgesOutOf(method);
            while (it.hasNext()) {
                i++;
                it.next();
            }

            return i;
        }
        else if(obj instanceof Unit){

            Unit unit = (Unit) obj;
            int i = 0;
            Iterator<Edge> it = callGraph.edgesOutOf(unit);
            while (it.hasNext()) {
                i++;
                it.next();
            }

            return i;

        }
        else{
            throw new RuntimeException("eroeo");
        }
    }

    @Override
    public Set<Edge> outgoingEdgesOf(Object obj) {

        if(obj instanceof SootMethod){
            SootMethod method = (SootMethod) obj;
            Set<Edge> edges = new HashSet<>();
            Iterator<Edge> it = callGraph.edgesOutOf(method);
            while (it.hasNext()) {
                edges.add(it.next());
            }
            return edges;
        }
        else if(obj instanceof Unit){
            Unit unit = (Unit) obj;
            Set<Edge> edges = new HashSet<>();
            Iterator<Edge> it = callGraph.edgesOutOf(unit);
            while (it.hasNext()) {
                edges.add(it.next());
            }
            return edges;
        }
        else{
            throw new RuntimeException("awdwa");
        }
    }

    @Override
    public Set<Edge> getAllEdges(Object method, Object v1) {
        throw new RuntimeException("Unsupported Operation");

    }

    @Override
    public Edge getEdge(Object method, Object v1) {
        throw new RuntimeException("Unsupported Operation");

    }

    @Override
    public EdgeFactory<Object, Edge> getEdgeFactory() {
        throw new RuntimeException("Unsupported Operation");

    }

    @Override
    public Edge addEdge(Object method, Object v1) {
        throw new RuntimeException("Unsupported Operation");
    }

    @Override
    public boolean addEdge(Object method, Object v1, Edge edge) {
        throw new RuntimeException("Unsupported Operation");

    }

    @Override
    public boolean addVertex(Object method) {
        throw new RuntimeException("Unsupported Operation");

    }

    @Override
    public boolean containsEdge(Object method, Object v1) {
        throw new RuntimeException("Unsupported Operation");

    }

    @Override
    public boolean containsEdge(Edge edge) {
        throw new RuntimeException("Unsupported Operation");

    }

    @Override
    public boolean containsVertex(Object method) {
        return methodVertexSet.contains(method);
    }

    @Override
    public Set<Edge> edgeSet() {
        return null;
              //  callGraph.getEdges();
    }

    @Override
    public Set<Edge> edgesOf(Object method) {
        throw new RuntimeException("Unsupported Operation");

    }

    @Override
    public boolean removeAllEdges(Collection<? extends Edge> collection) {
        throw new RuntimeException("Unsupported Operation");

    }

    @Override
    public Set<Edge> removeAllEdges(Object method, Object v1) {
        throw new RuntimeException("Unsupported Operation");

    }

    @Override
    public boolean removeAllVertices(Collection<? extends Object> collection) {
        throw new RuntimeException("Unsupported Operation");

    }

    @Override
    public Edge removeEdge(Object method, Object v1) {
        throw new RuntimeException("Unsupported Operation");

    }

    @Override
    public boolean removeEdge(Edge edge) {
        throw new RuntimeException("Unsupported Operation");

    }

    @Override
    public boolean removeVertex(Object method) {
        throw new RuntimeException("Unsupported Operation");

    }

    @Override
    public Set<Object> vertexSet() {
        if (methodVertexSet != null) {
            return methodVertexSet;
        }
        methodVertexSet = new HashSet<>();
        Iterator<MethodOrMethodContext> it = callGraph.sourceMethods();
        while (it.hasNext()) {
            MethodOrMethodContext m = it.next();
            methodVertexSet.add(m.method());
            methodVertexSet.add(m.context());
        }

      /*  it = callGraph.targetMethods();
        while (it.hasNext()) {
            MethodOrMethodContext m = it.next();
            methodVertexSet.add(m.method());
            methodVertexSet.add(m.context());
        }
*/

        return methodVertexSet;

    }

    @Override
    public SootMethod getEdgeSource(Edge edge) {
        return edge.src();
    }

    @Override
    public SootMethod getEdgeTarget(Edge edge) {
        return edge.tgt();
    }

    @Override
    public double getEdgeWeight(Edge edge) {
        return 0;
    }
}
