package callgraph.adapter.jgrapht;

import soot.Body;
import soot.jimple.JimpleBody;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LocalUses;
import soot.toolkits.scalar.SimpleLocalDefs;

/**
 * Created by adann on 21.02.17.
 */
public class UnitGraphContainer {


    private final LocalUses locals;
    private final UnitGraph unitGraph;
    private final SimpleLocalDefs localDefs;
    private Body jb;

    public UnitGraphContainer(LocalUses locals, UnitGraph unitGraph, SimpleLocalDefs localDefs, Body jb){

        this.locals = locals;
        this.unitGraph = unitGraph;
        this.localDefs = localDefs;
        this.jb = jb;
    }

    public UnitGraph getUnitGraph() {
        return unitGraph;
    }

    public LocalUses getLocals() {
        return locals;
    }

    public SimpleLocalDefs getLocalDefs() {
        return localDefs;
    }

    public Body getJb() {
        return jb;
    }
}
