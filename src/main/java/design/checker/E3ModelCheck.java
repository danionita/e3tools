package design.checker;

import java.util.Optional;

import design.E3Graph;

public interface E3ModelCheck {
	public Optional<ModelError> check(E3Graph graph);
}
