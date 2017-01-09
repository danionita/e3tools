package design.checker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

import design.E3Graph;
import design.Utils;
import design.info.ConnectionElement;
import design.info.EndSignal;
import design.info.LogicBase;
import design.info.LogicDot;
import design.info.SignalDot;
import design.info.StartSignal;
import design.info.ValueExchange;
import design.info.ValueInterface;
import design.info.ValuePort;

/**
 * @author Bobe
 *
 */
public class E3Walker {
	private Set<Object> visited = new HashSet<>();
	private	Stack<Object> history = new Stack<>();

	public void markVisited(Object obj) {
		visited.add(obj);
	}
	
	public boolean isVisited(Object obj) {
		return visited.contains(obj);
	}
	
	public void checkPath(E3Graph graph, Object startSignal) {
		visited.clear();
		history.clear();
		
		history.push(null); // Sentinel value

		visitStartSignal(startSignal);
		markVisited(startSignal);
		history.push(startSignal);
		
		while(history.size() > 1) {
			// Get descendants of object on top of stack
			// Check if any is not visited
			// If they're all visited, pop item off stack
			// Otherwise pick an unvisited descendant,
			// visit it, mark it as viisted, and put it
			// on the stack.
			
			Object subject = history.peek();
			Object subjectInfo = graph.getModel().getValue(subject);
			Object ancestor = history.get(history.size() - 2);
			Object ancestorInfo = graph.getModel().getValue(ancestor);

			if (subjectInfo instanceof StartSignal) {
				Object dot = graph.getModel().getChildAt(subject, 0);

				if (!isVisited(dot)) {
					visitSignalDot(dot, false);
					markVisited(dot);
					history.push(dot);
				} else {
					history.pop();
				}
			} else if (subjectInfo instanceof SignalDot) {
				if (ancestorInfo instanceof ConnectionElement) {
					Object parent = graph.getModel().getParent(subject);
					Object parentInfo = graph.getModel().getValue(parent);
					
					if (!isVisited(parent)) {
						if (parentInfo instanceof ValueInterface) {
							visitValueInterface(parent, false);
						} else if (parentInfo instanceof EndSignal) {
							visitEndSignal(parent);
						}  else if (parentInfo instanceof StartSignal) {
							visitBadStartSignal(parent);
						} else {
							throw new RuntimeException("Unexpected parent of SignalDot "
									+ "\"" + parentInfo.getClass().getSimpleName() + "\"");
						}

						markVisited(parent);
						history.push(parent);
					} else {
						history.pop();
					}
				} else if (ancestorInfo instanceof ValueInterface
						|| ancestorInfo instanceof StartSignal) {
					if (graph.getModel().getEdgeCount(subject) > 0) {
						Object nextCE = graph.getModel().getEdgeAt(subject, 0);
						if (!isVisited(nextCE)) {
							// Make sure we get the OTHER end of the connection element
							Object other = null;
							Object src = graph.getModel().getTerminal(nextCE, true);
							Object dst = graph.getModel().getTerminal(nextCE, false);
							if (src == subject) {
								other = dst;
							} else {
								other = src;
							}

							visitConnectionElement(subject, nextCE, other);
							
							markVisited(nextCE);
							history.push(nextCE);
						} else {
							history.pop();
						}
					} else {
						history.pop();
					}
				} else {
					throw new RuntimeException("Unexpected ancestor of SignalDot "
							+ "\"" + ancestorInfo.getClass().getSimpleName() + "\"");
				}
			} else if (subjectInfo instanceof ConnectionElement) {
				// Make sure we get the OTHER end of the connection element
				Object descendant = null;
				Object src = graph.getModel().getTerminal(subject, true);
				Object dst = graph.getModel().getTerminal(subject, false);
				if (src == ancestor) {
					descendant = dst;
				} else {
					descendant = src;
				}
				
				if (!isVisited(descendant)) {
					visitSignalDot(descendant, true);
					markVisited(descendant);
					history.push(descendant);
				} else {
					history.pop();
				}
			} else if (subjectInfo instanceof ValueExchange) {
				// Make sure we get the OTHER end of the connection element
				Object descendant = null;
				Object src = graph.getModel().getTerminal(subject, true);
				Object dst = graph.getModel().getTerminal(subject, false);
				if (src == ancestor) {
					descendant = dst;
				} else {
					descendant = src;
				}
				
				if (!isVisited(descendant)) {
					visitValuePort(descendant, true);
					markVisited(descendant);
					history.push(descendant);
				} else {
					history.pop();
				}
			} else if (subjectInfo instanceof LogicDot) {
				if (ancestorInfo instanceof ConnectionElement) {
					Object descendant = graph.getModel().getParent(subject);
					if (!isVisited(descendant)) {
						LogicDot ldInfo = (LogicDot) subjectInfo;
						// If the logic dot is not a unit dot
						// (not a dot on the side to which the user cannot add dots)
						// then the logic base is "narrowing", that is upstream is on the side
						// where the user can add dots.
						visitLogicBase(descendant, !ldInfo.isUnit);
						markVisited(descendant);
						history.push(descendant);
					} else {
						history.pop();
					}
				} else if (ancestorInfo instanceof LogicBase) {
					if (graph.getModel().getEdgeCount(subject) > 0) {
						Object nextCE = graph.getModel().getEdgeAt(subject, 0);
						
						if (!isVisited(nextCE)) {
							Object other = null;
							Object src = graph.getModel().getTerminal(nextCE, true);
							Object dst = graph.getModel().getTerminal(nextCE, false);
							if (src == subject) {
								other = dst;
							} else {
								other = src;
							}
							
							visitConnectionElement(subject, nextCE, other);
							markVisited(nextCE);
							history.push(nextCE);
						} else {
							history.pop();
						}
					} else {
						history.pop();
					}
				} else {
					throw new RuntimeException("Unexpected ancestor of LogicDot \""
							+ ancestorInfo.getClass().getSimpleName() + "\"");
				}
			} else if (subjectInfo instanceof EndSignal) {
				history.pop();
			} else if (subjectInfo instanceof LogicBase) {
				LogicDot ldInfo = (LogicDot) ancestorInfo;
				Optional<Object> descendantOpt = null;

				if (ldInfo.isUnit) {
					descendantOpt = Utils.getChildrenWithValue(graph, subject, LogicDot.class).stream()
							.filter(obj -> {
								LogicDot info = (LogicDot) graph.getModel().getValue(obj);
								return !info.isUnit;
							})
							.filter(obj -> !isVisited(obj))
							.findFirst();
				} else {
					descendantOpt = Utils.getChildrenWithValue(graph, subject, LogicDot.class).stream()
							.filter(obj -> {
								LogicDot info = (LogicDot) graph.getModel().getValue(obj);
								
								return info.isUnit;
							})
							.filter(obj -> !isVisited(obj))
							.findAny();
				}

				if (descendantOpt.isPresent()) {
					Object descendant = descendantOpt.get();
					visitLogicDot(descendant, false);
					markVisited(descendant);
					history.push(descendant);
				} else {
					history.pop();
				}
			} else if (subjectInfo instanceof ValueInterface) {
				Class<?> nextClass = null;

				if (ancestorInfo instanceof SignalDot) {
					nextClass = ValuePort.class;
				} else if (ancestorInfo instanceof ValuePort) {
					nextClass = SignalDot.class;
				} else {
					throw new RuntimeException("Unexpected ancestor of ValueInterface \"" + ancestorInfo.getClass().getSimpleName() + "\"");
				}

				Optional<Object> descendantOpt = Utils.getChildrenWithValue(graph, subject, nextClass).stream()
						.filter(obj -> !isVisited(obj))
						.findAny();
				
				if (descendantOpt.isPresent()) {
					Object descendant = descendantOpt.get();
					if (nextClass == SignalDot.class) {
						visitSignalDot(descendant, false);
					} else if (nextClass == ValuePort.class) {
						visitValuePort(descendant, false);
					}

					markVisited(descendant);
					history.push(descendant);
				} else {
					history.pop();
				}
			} else if (subjectInfo instanceof ValuePort) {
				if (ancestorInfo instanceof ValueExchange) {
					Object descendant = graph.getModel().getParent(subject);
					if (!isVisited(descendant)) {
						visitValueInterface(descendant, true);
						markVisited(descendant);
						history.push(descendant);
					} else {
						// Visit all the other value exchanges that are on the "other side" of the value exchanges
						// That is, if the ancestor VE does not come from a VI contained in the same container as this VP
						// We should look only at the VE's going to an actor contained in the same container as this VP
						
						Object vpContainer = graph.getContainerOfChild(subject);
						Object ancestorVP = Utils.getOpposite(graph, ancestor, subject);
						Object ancestorContainer = graph.getContainerOfChild(ancestorVP);
						
						boolean onlyNestedValueExchanges = true;
						if (graph.getModel().isAncestor(vpContainer, ancestorContainer)) {
							onlyNestedValueExchanges = false;
						}
						
						Object nextSubject = null;
						
						for (Object candidateSubject : graph.getEdges(subject)) {
							if (candidateSubject == ancestor) continue;

							Object candidateSubjectVP = Utils.getOpposite(graph, candidateSubject, subject);
							boolean isNestedValueExchange = graph.getModel().isAncestor(vpContainer, graph.getContainerOfChild(candidateSubjectVP));
							if (isNestedValueExchange != onlyNestedValueExchanges) continue;
							
							if (isVisited(candidateSubject)) continue;
							
							nextSubject = candidateSubject;
							break;
						}
						
						if (nextSubject != null) {
							visitValueExchange(subject, nextSubject, Utils.getOpposite(graph, nextSubject, subject));
							markVisited(nextSubject);
							history.push(nextSubject);
						} else {
							history.pop();
						}
					}
				} else if (ancestorInfo instanceof ValueInterface) {
					Object nextVE = null;
							
					for (Object candidateVE : graph.getEdges(subject)) {
						if (isVisited(candidateVE)) continue;
						
						nextVE = candidateVE;
						break;
					}

					if (nextVE != null) {
						Object otherVP = Utils.getOpposite(graph, nextVE, subject);

						visitValueExchange(subject, nextVE, otherVP);
						markVisited(nextVE);
						history.push(nextVE);
					} else {
						history.pop();
					}
				} else {
					throw new RuntimeException("Unexpected ancestor of ValuePort \"" 
							+ ancestorInfo.getClass().getSimpleName() + "\"");
				}
			} else {
				System.out.println(subject);
				System.out.println(subjectInfo);
				throw new RuntimeException("Unexpected subject \"" + subjectInfo.getClass().getSimpleName() + "\"");
			}
		}
	}
	
	public List<Object> getHistory() {
		return new ArrayList<>(history);
	}
	
	public Set<Object> getVisited() {
		return new HashSet<>(visited);
	}
	
	public void visitStartSignal(Object ss) {
		
	}
	
	/**
	 * Is called when a connection flow ENDS at a start signal
	 * @param ss
	 */
	public void visitBadStartSignal(Object ss) {
		
	}
	
	/**
	 * @param dot
	 * @param in True if the connection element is upstream
	 */
	public void visitLogicDot(Object dot, boolean in) {
		
	}
	
	/**
	 * @param dot
	 * @param in True if the connection element is upstream
	 */
	public void visitSignalDot(Object dot, boolean in) {
		
	}
	
	/**
	 * @param vp
	 * @param prev The previous link in the chain. Can either be a value exchange or a value interface
	 */
	public void visitValuePort(Object vp, Object prev) {
		
	}

	public void visitValueExchange(Object vpUp, Object ve, Object vpDown) {
		
	}

	public void visitConnectionElement(Object dotUp, Object ce, Object dotDown) {
		
	}
	
	/**
	 * @param gate
	 * @param narrowing True if upstream is at the non-unit dot's
	 * side. This means that the side where the user can add or remove
	 * input dots is upstream.
	 */
	public void visitLogicBase(Object gate, boolean narrowing) {
		
	}
	
	/**
	 * @param vi
	 * @param in True if the direction of value exchanges is upstream
	 */
	public void visitValueInterface(Object vi, boolean in) {
		
	}
	
	public void visitEndSignal(Object es) {
		
	}
}
