package design.checker.checks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import design.E3Graph;
import design.Utils;
import design.checker.E3ModelCheck;
import design.checker.E3Walker;
import design.checker.ModelError;
import design.info.StartSignal;

public class FlowChecker extends E3Walker implements E3ModelCheck {

	private enum Flow {
		SEND,
		RECEIVE
	}
	
	private Map<Object, Flow> flow = new HashMap<>();
	private Set<Object> conflictingDots = new HashSet<>();
	
	@Override
	public Optional<ModelError> check(E3Graph graph) {
		Utils.getAllCells(graph).stream()
			.filter(obj -> graph.getModel().getValue(obj) instanceof StartSignal)
			.forEach(obj -> {
				checkPath(graph, obj);
			});
		
		Set<Object> dots = getConflictingDots();
		
		if (dots.size() > 0) {
			return Optional.of(new ModelError("Conflicting flow directions.", new ArrayList<>(dots)));
		} else {
			return Optional.empty();
		}
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
