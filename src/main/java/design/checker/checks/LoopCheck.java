package design.checker.checks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import design.E3Graph;
import design.Utils;
import design.checker.E3ModelCheck;
import design.checker.E3Walker;
import design.checker.ModelError;
import design.info.Base;
import design.info.LogicDot;
import design.info.SignalDot;
import design.info.StartSignal;
import design.info.ValueInterface;
import design.info.ValuePort;

public class LoopCheck extends E3Walker implements E3ModelCheck {
	
	Set<Object> involvedObjects;
	
	private E3Graph graph;

	@Override
	public Optional<ModelError> check(E3Graph graph) {
		involvedObjects = new HashSet<>();
		this.graph = graph;
		
		Utils.getAllCells(graph).stream()
			.filter(obj -> graph.getModel().getValue(obj) instanceof StartSignal)
			.forEach(obj -> {
				checkPath(graph, obj);
			});
		
		if (involvedObjects.size() > 0) {
//			List<Object> history = new ArrayList<>(involvedObjects);
//			System.out.println("involved:");
//			for (Object obj : history) {
//				if (obj == null) continue;
//				System.out.println("\t" + graph.getModel().getValue(obj).getClass().getSimpleName());
//			}
			return Optional.of(new ModelError("The objects are involved in a loop.", new ArrayList<>(involvedObjects)));
		} else {
			return Optional.empty();
		}
	}
	
	public void visitStartSignal(Object ss) {
		// Nothing to do here, can't visit an SS twice.
	}
	
	/**
	 * Is called when a connection flow ENDS at a start signal
	 * @param ss
	 */
	public void visitBadStartSignal(Object ss) {
		// Also ok
	}
	
	/**
	 * @param dot
	 * @param in True if the connection element is upstream
	 */
	public void visitLogicDot(Object dot, boolean in) {
		if (!in) {
			// We need to check if the connectoin element is in the history 
			if (graph.getModel().getEdgeCount(dot) > 0) {
				// Dot has an outgoing edge
				Object connectionElement = graph.getModel().getEdgeAt(dot, 0);
				// If we already visited the edge, we're in a loop!
				if (isInHistory(connectionElement)) {
					involvedObjects.add(connectionElement);
				}
			}
		} else {
			// We need to check if the logic base is in the history
			Object logicBase = graph.getModel().getParent(dot);
			if (isInHistory(logicBase)) {
				involvedObjects.add(logicBase);
			}
		}
	}
	
	/**
	 * @param dot
	 * @param in True if the connection element is upstream
	 */
	public void visitSignalDot(Object dot, boolean in) {
		// The idea is the same, except at a signal dot we need to check for the value interface
		// and at a logic dot you need to check for the logic base. Both are the parent of the dot, so
		// this works out.
		visitLogicDot(dot, in);
	}
	
	/**
	 * @param vp
	 * @param prev The previous link in the chain. Can either be a value exchange or a value interface
	 */
	public void visitValuePort(Object vp, Object prev) {
		Base prevInfo = (Base) graph.getModel().getValue(prev);
		if (prevInfo instanceof ValueInterface) {
			// Check if we already visited any of the VE's
			for (int i = 0; i < graph.getModel().getEdgeCount(vp); i++) {
				Object ve = graph.getModel().getEdgeAt(vp, i);
				if (isInHistory(ve)) {
					involvedObjects.add(ve);
				}
			}
		} else {
			// prevInfo instanceof connection element
			// We need to check if we already saw the value interface
			Object valueInterface = graph.getModel().getParent(vp);
			if (isInHistory(valueInterface)) {
				involvedObjects.add(valueInterface);
				
				// Debug print
//				List<Object> history = getHistory();
//				System.out.println("History:");
//				for (Object obj : history) {
//					if (obj == null) continue;
//					System.out.println("\t" + graph.getModel().getValue(obj).getClass().getSimpleName());
//				}
			}
		}
	}

	public void visitValueExchange(Object vpUp, Object ve, Object vpDown) {
		// If we have already visited vpDown we will enter a loop
		if (isInHistory(vpDown)) {
			involvedObjects.add(vpDown);
		}
	}

	public void visitConnectionElement(Object dotUp, Object ce, Object dotDown) {
		// If we already visited dotDown, we will enter a loop.
		if (isInHistory(dotDown)) {
			involvedObjects.add(dotDown);
		}
		// But that will never really happen, because dotDown visited implies
		// ce Visited, which implies dotUp visited, which will be caught earlier.
		// Nevertheless, in case of a bug, it will be checked.
	}
	
	/**
	 * @param gate
	 * @param narrowing True if upstream is at the non-unit dot's
	 * side. This means that the side where the user can add or remove
	 * input dots is upstream.
	 */
	public void visitLogicBase(Object gate, boolean narrowing) {
		// Check all the logic dots on the side that narrowing dictates
		// whether or not they appear in the history
		Utils.getChildrenWithValue(graph, gate, LogicDot.class).stream()
			.filter(obj -> ((LogicDot) graph.getModel().getValue(obj)).isUnit == narrowing)
			.forEach(obj -> {
				if (isInHistory(obj)) {
					involvedObjects.add(obj);
				}
			});
	}
	
	/**
	 * @param vi
	 * @param in True if the direction of value exchanges is upstream
	 */
	public void visitValueInterface(Object vi, boolean in) {
		if (in) {
			// Need to check the signal dot
			// This will only fail if the value interfaces object layout ever changes
			Object signalDot = Utils.getChildrenWithValue(graph, vi, SignalDot.class).get(0);
			if (isInHistory(signalDot)) {
				involvedObjects.add(signalDot);
			}
		} else {
			// Need to check the value ports if they are visited already
			Utils.getChildrenWithValue(graph, vi, ValuePort.class).stream()
				.forEach(vp -> {
					if (isInHistory(vp)) {
						involvedObjects.add(vp);
					}
				});
		}
	}
	
	public void visitEndSignal(Object es) {
		// Can skip this one.
	}

}
