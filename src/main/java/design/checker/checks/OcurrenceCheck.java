package design.checker.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.hp.hpl.jena.rdf.model.Model;

import design.E3Graph;
import design.Utils;
import design.checker.E3ModelCheck;
import design.checker.E3Walker;
import design.checker.ModelError;
import design.export.RDFExport;
import design.export.RDFExport.VTMode;
import design.info.LogicBase;
import design.info.LogicDot;
import design.info.StartSignal;
import e3fraud.model.ExpressionEvaluator;
import e3fraud.model.ExpressionEvaluator.ModelOrError;

public class OcurrenceCheck extends E3Walker implements E3ModelCheck{
	
	Optional<ExpressionEvaluator> getEvaluatedModel(E3Graph graph) {
		RDFExport rdfExporter = new RDFExport(graph, true, VTMode.DERIVE_ORPHANED, true);

		if (!rdfExporter.getModel().isPresent()) {
			Optional<String> error = rdfExporter.getError();

			String errorString = "An error occurred while converting to an internal format. Please make sure the model contains no errors.";
			if (error.isPresent()) {
				errorString += " The error: \n" + error.get();
			}

			System.out.println("Error while checking for correct formulas from RDF: " + errorString);

			return Optional.empty();
		}

		Model model = rdfExporter.getModel().get();
		
		ModelOrError moe = ExpressionEvaluator.evaluateModelOrError(model);
		
		if (moe.optionalModel.isPresent()) {
			return moe.optionalModel;
		}
		
		return Optional.empty();
	}
	
	ExpressionEvaluator em;
	E3Graph graph;
	Optional<Double> currentOccurrences = Optional.empty();
	Map<Object, Double> occurrenceMap;
	
	@Override
	public Optional<ModelError> check(E3Graph graph) {
		Optional<ExpressionEvaluator> emOpt = getEvaluatedModel(graph);
		
		if (!emOpt.isPresent()) {
			return Optional.empty();
		}
		
		em = emOpt.get();
		this.graph = graph;
		
		occurrenceMap = new HashMap<>();
		
		Utils.getAllCells(graph).stream()
			.filter(obj -> graph.getModel().getValue(obj) instanceof StartSignal)
			.forEach(obj -> checkPath(graph, obj));
		
		List<Object> unmatchingCells = Utils.getAllCells(graph).stream()
			.filter(obj -> graph.getModel().getValue(obj) instanceof LogicBase)
			.map(obj -> {
				long distinctOccurrenceValues = Utils.getChildrenWithValue(graph, obj, LogicDot.class).stream()
					// If a port does not have an occurrence rate we put it at 1.0, s.t. it will be different from all other
					// possible occurrences.
					.map(ld -> occurrenceMap.getOrDefault(ld, -1.0))
					.distinct()
					.count();
				
				if (distinctOccurrenceValues == 1) {
					// Only 1 distinct value, so they are all the same, so no bad dots here!
					return new ArrayList<>();
				} else {
					// They are different, so they are all wrong! Light them all up!
					return Utils.getChildrenWithValue(graph, obj, LogicDot.class);
				}
			})
			.flatMap(List::stream)
			.collect(Collectors.toList());
		
		if (unmatchingCells.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(new ModelError("All dots on an AND or OR gate must have equal occurrence rates.", unmatchingCells));
		}
	}
	
	@Override
	public void visitStartSignal(Object ss) {
		StartSignal ssInfo = (StartSignal) Utils.base(graph, ss);
		
		currentOccurrences = em.valueOf("#" + ssInfo.SUID + ".OCCURRENCES");
		
//		if (currentOccurrences.isPresent()) {
//			System.out.println("Occurrences for " + ssInfo.SUID + " = " + currentOccurrences.get());
//		}
	}
	
	@Override
	public void visitConnectionElement(Object dotUp, Object ce, Object dotDown) {
		if (currentOccurrences.isPresent()) {
			occurrenceMap.put(dotUp, currentOccurrences.get());
			occurrenceMap.put(dotDown, currentOccurrences.get());
		}
	}

	public void visitLogicBase(Object gate, boolean narrowing) {
//		System.out.println("Visiting logic base");
	}
}
