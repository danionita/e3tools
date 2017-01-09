package design.checker.checks;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import design.E3Graph;
import design.Utils;
import design.checker.E3ModelCheck;
import design.checker.E3Walker;
import design.checker.ModelError;
import design.info.StartSignal;

public class EndStimuliCheck extends E3Walker implements E3ModelCheck {
	
	@Override
	public Optional<ModelError> check(E3Graph graph) {
		
		List<Object> faultyStartStimuli = Utils.getAllCells(graph).stream()
				.filter(obj -> graph.getModel().getValue(obj) instanceof StartSignal)
				.filter(obj -> !startStimuliHasEndStimuli(graph, obj))
				.collect(Collectors.toList());
		
		if (faultyStartStimuli.size() > 0) {
			return Optional.of(new ModelError("No end stimuli found for start stimuli.", faultyStartStimuli));
		} else {
			return Optional.empty();
		}
	}
	
	private boolean foundEndStimuli;
	
	public boolean startStimuliHasEndStimuli(E3Graph graph, Object startSignal) {
		foundEndStimuli = false;
		
		checkPath(graph, startSignal);
		
		return foundEndStimuli;
	}
	
	@Override
	public void visitEndSignal(Object es) {
		foundEndStimuli = true;
	}

}
