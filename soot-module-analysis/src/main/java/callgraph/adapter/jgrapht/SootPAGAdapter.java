package callgraph.adapter.jgrapht;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import soot.SootMethod;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.VarNode;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.Collection;
import java.util.Set;

/**
 * Created by adann on 25.10.16.
 */
public class SootPAGAdapter implements DirectedGraph<Node, Edge> {

    private PAG pag;

    private Set<SootMethod> methodVertexSet;


    public SootPAGAdapter(PAG pag) {

        this.pag = pag;

    }


    @Override
    public int inDegreeOf(Node node) {
        return 0;
    }

    @Override
    public Set<Edge> incomingEdgesOf(Node node) {
        return null;
    }

    @Override
    public int outDegreeOf(Node node) {
        return 0;
    }

    @Override
    public Set<Edge> outgoingEdgesOf(Node node) {
        return null;
    }

    @Override
    public Set<Edge> getAllEdges(Node node, Node v1) {
        return null;
    }

    @Override
    public Edge getEdge(Node node, Node v1) {
        return null;
    }

    @Override
    public EdgeFactory<Node, Edge> getEdgeFactory() {
        return null;
    }

    @Override
    public Edge addEdge(Node node, Node v1) {
        return null;
    }

    @Override
    public boolean addEdge(Node node, Node v1, Edge edge) {
        return false;
    }

    @Override
    public boolean addVertex(Node node) {
        return false;
    }

    @Override
    public boolean containsEdge(Node node, Node v1) {
        return false;
    }

    @Override
    public boolean containsEdge(Edge edge) {
        return false;
    }

    @Override
    public boolean containsVertex(Node node) {
        return false;
    }

    @Override
    public Set<Edge> edgeSet() {
        return null;
    }

    @Override
    public Set<Edge> edgesOf(Node node) {
        return null;
    }

    @Override
    public boolean removeAllEdges(Collection<? extends Edge> collection) {
        return false;
    }

    @Override
    public Set<Edge> removeAllEdges(Node node, Node v1) {
        return null;
    }

    @Override
    public boolean removeAllVertices(Collection<? extends Node> collection) {
        return false;
    }

    @Override
    public Edge removeEdge(Node node, Node v1) {
        return null;
    }

    @Override
    public boolean removeEdge(Edge edge) {
        return false;
    }

    @Override
    public boolean removeVertex(Node node) {
        return false;
    }

    @Override
    public Set<Node> vertexSet() {
        return null;
    }

    @Override
    public Node getEdgeSource(Edge edge) {
        return null;
    }

    @Override
    public Node getEdgeTarget(Edge edge) {
        return null;
    }

    @Override
    public double getEdgeWeight(Edge edge) {
        return 0;
    }
}
