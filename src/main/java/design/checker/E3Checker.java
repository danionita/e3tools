package design.checker;

import java.util.ArrayList;
import java.util.List;

import design.E3Graph;
import design.Utils;
import design.info.StartSignal;

public class E3Checker {
	List<String> errors = new ArrayList<>();

	private E3Graph graph;
	
	public E3Checker(E3Graph graph) {
		this.graph = graph;
		
		checkForStartStimuli();
	}
	
	public void checkForStartStimuli() {
		if (Utils.getAllCells(graph).stream()
				.map(graph.getModel()::getValue)
				.filter(obj -> obj instanceof StartSignal)
				.count() < 1) {
			errors.add("At least once start stimuli is required for fraud generation.");
		}
	}
}
