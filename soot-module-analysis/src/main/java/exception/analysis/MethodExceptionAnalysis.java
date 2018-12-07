package exception.analysis;

import soot.*;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.exceptions.ThrowableSet;
import soot.toolkits.exceptions.UnitThrowAnalysis;

import java.util.*;


public class MethodExceptionAnalysis {

    private final SootMethod method;
    private final SootClass soot_class;


    private final Map<SootClass, ClassExceptionAnalysis> class_map;


    public Set<SootClass> getMethod_exception_set() {
        return method_exception_set;
    }

    private final Set<SootClass> method_exception_set;


    public MethodExceptionAnalysis(SootClass c, SootMethod m, Map<SootClass, ClassExceptionAnalysis> cm) {
        method = m;
        soot_class = c;
        class_map = cm;
        method_exception_set = new HashSet<>();
    }


    /**
     * inspired by
     *
     * @return if fixPoint is reached
     * @see UnitThrowAnalysis#mightThrow(SootMethod, Set)
     */

    public boolean DoAnalysis() {

        HashSet<SootClass> current_exception_sets = new HashSet<>();
        current_exception_sets.addAll(method_exception_set);


        if (!method.hasActiveBody()) {
            method_exception_set.addAll(this.method.getExceptions());
            ExceptionAnalysis.verboseOutput("No active Body: " + method.getName());
            return false;
        }
        ExceptionAnalysis.verboseOutput("Active Body: " + method.getName());


        // mapping between unit and exception
        // add all units between traps
        final PatchingChain<Unit> units = method.getActiveBody().getUnits();
        Map<Unit, Collection<Trap>> unitToTraps = method.getActiveBody().getTraps().isEmpty()
                ? null : new HashMap<>();
        for (Trap t : method.getActiveBody().getTraps()) {
            for (Iterator<Unit> unitIt = units.iterator(t.getBeginUnit(),
                    units.getPredOf(t.getEndUnit())); unitIt.hasNext(); ) {
                Unit unit = unitIt.next();

                Collection<Trap> unitsForTrap = unitToTraps.get(unit);
                if (unitsForTrap == null) {
                    unitsForTrap = new ArrayList<>();
                    unitToTraps.put(unit, unitsForTrap);
                }
                unitsForTrap.add(t);
            }
        }


        for (Unit u : this.method.getActiveBody().getUnits()) {
            if (u instanceof Stmt) {
                Collection<SootClass> exception_classes = new HashSet<>();


                Stmt stmt = (Stmt) u;
                ThrowableSet curStmtExceptionSet = ThrowableSet.Manager.v().EMPTY;
                if (stmt.containsInvokeExpr()) {


                    HashSet<SootMethod> methodsToCheck = new HashSet<>();

                    //get target methods of this call
                    Iterator<Edge> it = ExceptionAnalysis.cgForExceptionAnalysis.edgesOutOf(stmt);
                    while (it.hasNext()) {
                        Edge edge = it.next();
                        SootMethod m = edge.getTgt().method();
                        methodsToCheck.add(m);
                    }


                    //hold/collects the new found exceptions

                    for (SootMethod callee : methodsToCheck) {

                        if (class_map.containsKey(callee.getDeclaringClass()) && class_map.get(callee.getDeclaringClass()).method_analysis.containsKey(callee)) {

                            //FIXME: hack
                          //  assert class_map.get(callee.getDeclaringClass()).method_analysis.containsKey(callee);
                            exception_classes.addAll(class_map.get(callee.getDeclaringClass()).method_analysis.get(callee).method_exception_set);
                        } else {
                            exception_classes.addAll(callee.getExceptions());
                        }


                    }
                    //add the new found exceptions
                    // to the current Statement Throwable Set
                    for (SootClass exception_class : exception_classes) {
                        curStmtExceptionSet = curStmtExceptionSet.add(exception_class.getType());
                    }


                } else {
                    curStmtExceptionSet = UnitThrowAnalysis.v().mightThrow(u);
                    /* FIXME: iMHO: in the above code throw statements are missing
                     * FIXME: check if they are handed here
                     */
                }

                // The exception might be caught along the way
                if (unitToTraps != null) {
                    Collection<Trap> trapsForUnit = unitToTraps.get(stmt);
                    if (trapsForUnit != null)
                        for (Trap t : trapsForUnit) {
                            ThrowableSet.Pair p = curStmtExceptionSet.whichCatchableAs(t.getException().getType());
                            curStmtExceptionSet = curStmtExceptionSet.remove(p.getCaught());
                        }
                }

                //only add those exceptions to the method set that are not caught via a trap in the above code
                for (RefLikeType t : curStmtExceptionSet.typesIncluded()) {
                    if (t instanceof RefType) {
                        RefType type = (RefType) t;
                        current_exception_sets.add(type.getSootClass());

                    }
                }
            }
        }

        //add the current method exception to the method's exception
        // if we found new exceptions, we have not reached a fixpoint
        // thus re-iterate until fixpoint is reached
        //FIXME: check if modification is correct
      /*  boolean done = true;
        if (this.method_exception_set.size() != current_exception_sets.size()) {
            method_exception_set.addAll(current_exception_sets);
            done = false;
        }

        return done;*/

        method_exception_set.addAll(current_exception_sets);
        return this.method_exception_set.size() != current_exception_sets.size();
    }

}
