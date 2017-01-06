package design.checker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import design.E3Graph;
import design.Utils;
import design.info.StartSignal;

public class FlowChecker extends E3Walker {

	private enum Flow {
		SEND,
		RECEIVE
	}
	
	private Map<Object, Flow> flow = new HashMap<>();
	private Set<Object> conflictingDots = new HashSet<>();
	
	public FlowChecker(E3Graph graph) {
		super(graph);
		
		Utils.getAllCells(graph).stream()
			.filter(obj -> graph.getModel().getValue(obj) instanceof StartSignal)
			.forEach(obj -> {
				checkPath(obj);
			});
	}
	
	@Override
	public void visitConnectionElement(Object dotUp, Object ce, Object dotDown) {
		if (flow.containsKey(dotUp)) {
			if (flow.get(dotUp) != Flow.SEND) {
				conflictingDots.add(dotUp);
			}
		} else {
			flow.put(dotUp, Flow.SEND);
		}
		
		if (flow.containsKey(dotDown)) {
			if (flow.get(dotDown) != Flow.RECEIVE) {
				conflictingDots.add(dotDown);
			}
		} else {
			flow.put(dotDown, Flow.RECEIVE);
		}
	}
	
	public Set<Object> getConflictingDots() {
		return new HashSet<>(conflictingDots);
	}
}
