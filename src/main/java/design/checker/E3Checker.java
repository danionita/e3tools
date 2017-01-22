package design.checker;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.bag.SynchronizedSortedBag;

import design.E3Graph;
import design.Utils;
import design.checker.checks.EndStimuliCheck;
import design.checker.checks.FlowChecker;
import design.checker.checks.LoopCheck;
import design.checker.checks.StartStimuliCheck;
import design.checker.checks.UnusedPortCheck;
import design.info.StartSignal;

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
			new UnusedPortCheck()
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
