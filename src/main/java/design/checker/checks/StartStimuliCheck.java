package design.checker.checks;

import java.util.Optional;

import design.E3Graph;
import design.Utils;
import design.checker.E3ModelCheck;
import design.checker.ModelError;
import design.info.StartSignal;

public class StartStimuliCheck implements E3ModelCheck {

	@Override
	public Optional<ModelError> check(E3Graph graph) {
		if (Utils.getAllCells(graph).stream()
				.map(graph.getModel()::getValue)
				.filter(obj -> obj instanceof StartSignal)
				.count() < 1) {
			return Optional.of(new ModelError("At least once start stimuli is required for fraud generation."));
		}

		return Optional.empty();
	}

}
