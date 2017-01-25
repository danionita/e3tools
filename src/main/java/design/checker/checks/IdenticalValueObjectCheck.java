package design.checker.checks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import design.E3Graph;
import design.Utils;
import design.checker.E3ModelCheck;
import design.checker.ModelError;
import design.info.ValueExchange;
import design.info.ValuePort;

public class IdenticalValueObjectCheck implements E3ModelCheck {
	
	public List<Object> nonIdenticalValuePortAndValueExchanges(Object vp) {
		List<Object> involvedObjects = new ArrayList<>();
		
		List<Object> valueExchanges = new ArrayList<>();
		for (int i = 0; i < graph.getModel().getEdgeCount(vp); i++) {
			valueExchanges.add(graph.getModel().getEdgeAt(vp, i));
		}
		
		if (valueExchanges.size() <= 1) return new ArrayList<>();
		
		if (valueExchanges.stream()
			.map(ve -> (ValueExchange) graph.getModel().getValue(ve))
			.map(veInfo -> veInfo.valueObject)
			.distinct()
			.limit(2)
			.count()
			<= 1) {
			return new ArrayList<>();
		} else {
			valueExchanges.add(vp);
			return valueExchanges;
		}
	}
	
	private E3Graph graph;
	
	@Override
	public Optional<ModelError> check(E3Graph graph) {
		this.graph = graph;
		
		List<Object> involvedObjects = Utils.getAllCells(graph).stream()
			.filter(cell -> graph.getModel().getValue(cell) instanceof ValuePort)
			.filter(cell -> graph.getModel().getEdgeCount(cell) > 1)
			.map(this::nonIdenticalValuePortAndValueExchanges)
			.flatMap(l -> l.stream())
			.collect(Collectors.toList());
		
		if (involvedObjects.size() > 0) {
			return Optional.of(new ModelError("Value exchanges connected to identical value ports should have identical value objects.", involvedObjects));
		} else {
			return Optional.empty();
		}
	}
}
