import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import soot.Body;
import soot.PatchingChain;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.Trap;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.ThrowStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JimpleLocal;


public class MethodExceptionAnalysis {
	
	SootMethod method;
	Body body;
	SootClass soot_class;
	
	Map<SootClass, ClassExceptionAnalysis> class_map;
	
	public class TwoValuePair< V, W > {
		public final V v;
		public final W w;
		
		public TwoValuePair( V v, W w ) {
			this.v = v;
			this.w = w;
		}
	}
	Set<SootClass> method_exception_set;
	
	Map<Unit, List<TwoValuePair<Trap, Set<SootClass>>>> trap_begin_exception_sets;
	Map<Unit, List<TwoValuePair<Trap, Set<SootClass>>>> trap_end_exception_sets;
	
	public MethodExceptionAnalysis(SootClass c, SootMethod m,  Map<SootClass, ClassExceptionAnalysis> cm ) {
		method = m;
		soot_class = c;
		class_map = cm;
		body = m.retrieveActiveBody();
		trap_begin_exception_sets = new HashMap<Unit, List<TwoValuePair<Trap, Set<SootClass>>>>();
		trap_end_exception_sets = new HashMap<Unit, List<TwoValuePair<Trap, Set<SootClass>>>>();
		method_exception_set = new HashSet<SootClass>();
		
		for( Trap t : body.getTraps() ) {
			List<TwoValuePair<Trap, Set<SootClass>>> trap_pairs;
			TwoValuePair<Trap, Set<SootClass>> tvp;
			
			if( trap_begin_exception_sets.containsKey(t.getBeginUnit())) { // we assume that if traps have the same begin unit, they must have the same end unit and the same exception set as well
				trap_pairs = trap_begin_exception_sets.get(t.getBeginUnit());
				tvp = new TwoValuePair<Trap, Set<SootClass>>(t, trap_pairs.get(0).w); //use the same set
			}
			else {
				trap_pairs = new ArrayList<TwoValuePair<Trap, Set<SootClass>>>();
				tvp = new TwoValuePair<Trap, Set<SootClass>>(t, new HashSet<SootClass>());
			}
			
			trap_pairs.add(tvp);
			
			trap_begin_exception_sets.put(t.getBeginUnit(), trap_pairs);
			trap_end_exception_sets.put(t.getEndUnit(), trap_pairs); //May overwrite different list if we don't make the assumption above
		}
	}
	
	//finds the most restrictive form of exception that is also a superclass of exception_class
	SootClass GetMostRestrictiveFormOfException( List<TwoValuePair<Trap, Set<SootClass>>> tvps, SootClass exception_class ) {
		List<TwoValuePair<SootClass, Integer>> superclass_counters = new ArrayList<TwoValuePair<SootClass, Integer>>();
		for( TwoValuePair<Trap, Set<SootClass>> tvp : tvps ) {
			SootClass super_exception_class = exception_class;
			int i = 0;
			while(super_exception_class != null ) {
				
				if( tvp.v.getException() == super_exception_class) {
					superclass_counters.add(new TwoValuePair<SootClass, Integer>(tvp.v.getException(), new Integer(i)));
				}
				
				i++;
				try {
					super_exception_class = super_exception_class.getSuperclass();
				} catch ( RuntimeException e ) {//unfortunately soot doesn't have a specific exception class for "no superclass for java.lang.Object" exception
					super_exception_class = null;
				}
			}
		}
		
		assert superclass_counters.size() != 0;
		
		TwoValuePair<SootClass, Integer> most_restrictive_form = superclass_counters.get(0);
		for( TwoValuePair<SootClass, Integer> tvp : superclass_counters ) {
			if( tvp.w < most_restrictive_form.w ) {
				most_restrictive_form = tvp;
			}
		}
		
		return most_restrictive_form.v;
	}
	public boolean DoAnalysis( boolean verbose ){
		Stack<List<Set<SootClass>>> current_exception_sets = new Stack<List<Set<SootClass>>>();
		{
			List<Set<SootClass>> exception_sets = new ArrayList<Set<SootClass>>();
			exception_sets.add(method_exception_set);
			current_exception_sets.push(exception_sets);
		}
		
		PatchingChain<Unit> chain = body.getUnits();
		Unit next = chain.getFirst();
		
		int previous_class_count = method_exception_set.size();
		for( Map.Entry<Unit, List<TwoValuePair<Trap, Set<SootClass>>>> entry : trap_begin_exception_sets.entrySet() ) {
			for( TwoValuePair<Trap, Set<SootClass>> tvp : entry.getValue() ) {
				previous_class_count += tvp.w.size();
			}
		}
		
		do {
			
			if( trap_begin_exception_sets.containsKey(next)) {
				List<TwoValuePair<Trap, Set<SootClass>>> tvps = trap_begin_exception_sets.get(next);
				List<Set<SootClass>> sets_to_add = new ArrayList<Set<SootClass>>();
				for( TwoValuePair<Trap, Set<SootClass>> tvp : tvps ) {
					sets_to_add.add(tvp.w);
				}
				current_exception_sets.push(sets_to_add);
			}
			if( next instanceof InvokeStmt || next instanceof AssignStmt ) {
				SootMethod callee = null;
				
				if( next instanceof AssignStmt ) {
					if ( ((AssignStmt)next).getRightOp() instanceof InvokeExpr) {
						callee = ((InvokeExpr)((AssignStmt)next).getRightOp()).getMethod();
					}
				} else {
					callee = ((InvokeStmt)next).getInvokeExpr().getMethod();
				}
				
				if( callee != null ) { // invocation only
					
					Collection<SootClass> exception_classes;
					if( class_map.containsKey(callee.getDeclaringClass()) ) {
						assert class_map.get(callee.getDeclaringClass()).method_analysis.containsKey(callee);
						
						exception_classes = class_map.get(callee.getDeclaringClass()).method_analysis.get(callee).method_exception_set;
					}
					else {
						exception_classes = callee.getExceptions();
					}
					for( SootClass exception_class : exception_classes ) {
						for( Set<SootClass> current_exception_set : current_exception_sets.peek() ) {
							current_exception_set.add(exception_class);
						}
					}
				}
				
			}
			else if( next instanceof ThrowStmt ) {
				
				assert chain.getPredOf(next) instanceof JInvokeStmt;
				assert ((JInvokeStmt) chain.getPredOf(next)).getInvokeExpr() instanceof SpecialInvokeExpr;
				assert ((SpecialInvokeExpr)((JInvokeStmt) chain.getPredOf(next)).getInvokeExpr()).getBase() instanceof JimpleLocal;				
				assert ((JimpleLocal)((SpecialInvokeExpr)((JInvokeStmt) chain.getPredOf(next)).getInvokeExpr()).getBase()).getType() instanceof RefType;
				
				SootClass exception_class = ((RefType)((JimpleLocal)((SpecialInvokeExpr)((JInvokeStmt) chain.getPredOf(next)).getInvokeExpr()).getBase()).getType()).getSootClass();
				
				for( Set<SootClass> current_exception_set : current_exception_sets.peek() ) {
					current_exception_set.add(exception_class);
				}
			}
			
			if( trap_end_exception_sets.containsKey(next)) {
				
				List<TwoValuePair<Trap, Set<SootClass>>> tvps = trap_end_exception_sets.get(next);
				
				if( verbose ) {
					for( Set<SootClass> current_exception_set : current_exception_sets.peek() ) { 
						if( current_exception_set.size() == 0 ) {
							for( TwoValuePair<Trap, Set<SootClass>> tvp : tvps ) {
								if( !tvp.v.getException().getName().equals("java.lang.RuntimeException"))
									System.out.println("Warning: In " + method.toString() + ": Unncessary exception handler for catching " + tvp.v.getException().getName() );
							}
						}
					}
				}
				
				for( TwoValuePair<Trap, Set<SootClass>> tvp : tvps ) {
					
					Iterator<SootClass> i = tvp.w.iterator();
					while( i.hasNext() ) {
						SootClass exception_class = i.next();
						
						if( tvp.v.getException() == exception_class ) {//getting properly caught by the handler.
							i.remove();
						} else {
							SootClass super_exception_class = exception_class;
							while(super_exception_class != null ) {
								
								if( tvp.v.getException() == super_exception_class) {
									i.remove();
									if( verbose && super_exception_class != exception_class && GetMostRestrictiveFormOfException(tvps, exception_class) == tvp.v.getException() && !super_exception_class.getName().equals("java.lang.RuntimeException")) {
										System.out.println("Warning: In " + method.toString() + ": attempting to catch " + exception_class.getName() + " with " + super_exception_class.getName() + " Consider catching the most restrictive exception class");
									}
									break;
								}
								try {
									super_exception_class = super_exception_class.getSuperclass();
								} catch ( RuntimeException e ) {//unfortunately soot doesn't have a specific exception class for "no superclass for java.lang.Object" exception
									super_exception_class = null;
								}
							}
						}
					}
				}

				current_exception_sets.pop();
				
				for( Set<SootClass> current_exception_set : current_exception_sets.peek() ) { //This list iteration is not necessary if we are still making the assumption that there's only one unique set that contains exception classes if the range for traps are the same 
					for( TwoValuePair<Trap, Set<SootClass>> tvp : tvps ) {
						for( SootClass exception_class : tvp.w ) {
							current_exception_set.add(exception_class);
						}
					}
				}
			}
			next = chain.getSuccOf(next);
		} while( next != null );
		
		int new_class_count = method_exception_set.size();
		for( Map.Entry<Unit, List<TwoValuePair<Trap, Set<SootClass>>>> entry : trap_begin_exception_sets.entrySet() ) {
			for( TwoValuePair<Trap, Set<SootClass>> tvp : entry.getValue() ) {
				new_class_count += tvp.w.size();
			}
		}
		
		return new_class_count != previous_class_count;
	}

}
