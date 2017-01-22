package design.checker.checks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import design.E3Graph;
import design.Utils;
import design.checker.E3ModelCheck;
import design.checker.ModelError;
import design.info.LogicDot;
import design.info.SignalDot;
import design.info.ValueInterface;
import design.info.ValuePort;

public class UnusedPortCheck implements E3ModelCheck {

	@Override
	public Optional<ModelError> check(E3Graph graph) {
		List<Object> unusedPorts = new ArrayList<Object>();
		
		unusedPorts.addAll(
				Utils.getAllCells(graph).stream()
				.filter(obj -> graph.getModel().getValue(obj) instanceof ValuePort)
				.filter(obj -> graph.getModel().getEdgeCount(obj) < 1)
				.collect(Collectors.toList())
				);
		
		unusedPorts.addAll(
				Utils.getAllCells(graph).stream()
				.filter(obj -> graph.getModel().getValue(obj) instanceof LogicDot)
				.filter(obj -> graph.getModel().getEdgeCount(obj) < 1)
				.collect(Collectors.toList())
				);
		
		unusedPorts.addAll(
				Utils.getAllCells(graph).stream()
				// Finds all the signal dots in the graph
				.filter(obj -> graph.getModel().getValue(obj) instanceof SignalDot)
				// Checks if a given signal dot needs to be connected or not
				// - If a signal dot is on anything besides a value interface, it should be connected
				// - If a signal dot is on a value interface, it should be connected if all the value interface's
				//   value ports do not have any doubly edges or are not connected.
				// - If a value interface has ports with doubly edges, the signal dot does not have to be connected
				.filter(obj -> {
					Object parent = graph.getModel().getParent(obj);
					
					if (graph.getModel().getValue(parent) instanceof ValueInterface) {
						boolean noDoublyEdges = Utils.getChildrenWithValue(graph, parent, ValuePort.class).stream()
							.map(graph.getModel()::getEdgeCount)
							.allMatch(n -> n == 1 || n == 0);
						
						if (noDoublyEdges) {
							return graph.getModel().getEdgeCount(obj) < 1;
						} else {
							return false;
						}
					} else {
						return graph.getModel().getEdgeCount(obj) < 1;
					}
				})
				.collect(Collectors.toList())
				);
		
		if (unusedPorts.size() > 0) {
			return Optional.of(new ModelError("Unconnected ports detected.", unusedPorts));
		} else {
			return Optional.empty();
		}
	}

}
