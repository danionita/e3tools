package design.checker;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import design.E3Graph;
import design.checker.checks.CorrectFormulaCheck;
import design.checker.checks.EndStimuliCheck;
import design.checker.checks.FlowChecker;
import design.checker.checks.IdenticalValueObjectCheck;
import design.checker.checks.LoopCheck;
import design.checker.checks.OcurrenceCheck;
import design.checker.checks.StartStimuliCheck;
import design.checker.checks.UnusedPortCheck;

/*
 * Checks:
 * - For an and gate, if checkable, incoming ce's must have the same occurrence rates
 * - Check wrong formulas
 * - Loops
 * - End stimuli check
 * - Conflicting flow check
 * - Start stimuli check
 * - Unused ports
 * - Connected ports must have the same value objects (Dan? doubly ports or ports on either side of a VE)
 *   - But we cannot assign value objects to ports, so only doubly edges?
 *   - Jup!
 */
public class E3Checker {
	
	/**
	 * @return A list of all available checks
	 */
	public static List<E3ModelCheck> allChecks() {
		return Arrays.asList(
			new EndStimuliCheck(),
			new FlowChecker(),
			new LoopCheck(),
			new StartStimuliCheck(),
			new UnusedPortCheck(),
			new IdenticalValueObjectCheck(),
			new CorrectFormulaCheck(),
			new OcurrenceCheck()
			);
	}
	
	/**
	 * Checks graph for all possible error checks.
	 * @param graph
	 * @return A list of model errors.
	 */
	public static List<ModelError> checkForErrors(E3Graph graph) {
		List<E3ModelCheck> allChecks = allChecks();
		
		return allChecks.stream()
			.map(c -> c.check(graph))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList())
			;
	}
}
