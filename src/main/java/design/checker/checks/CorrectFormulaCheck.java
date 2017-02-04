package design.checker.checks;

import java.util.Arrays;
import java.util.Optional;

import com.hp.hpl.jena.rdf.model.Model;

import design.E3Graph;
import design.Utils;
import design.checker.E3ModelCheck;
import design.checker.ModelError;
import design.export.RDFExport;
import design.export.RDFExport.VTMode;
import design.info.Base;
import e3fraud.model.EvaluatedModel;
import e3fraud.model.EvaluatedModel.ModelOrError;

public class CorrectFormulaCheck implements E3ModelCheck {

	@Override
	public Optional<ModelError> check(E3Graph graph) {

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
		
		ModelOrError moe = EvaluatedModel.evaluateModelOrError(model);
		
		if (moe.optionalModel.isPresent()) {
			return Optional.empty();
		}
		
		if (moe.badUID.isPresent()) {
			String badUIDStr = moe.badUID.get();
			String badFormulaName = moe.badFormulaName.get();
			
			try {
				long uid = new Long(badUIDStr);
				
				Optional<Object> infoOpt = Utils.getAllCells(graph).stream()
						.filter(obj -> ((Base) graph.getModel().getValue(obj)).SUID == uid)
					.filter(obj -> ((Base) graph.getModel().getValue(obj)).SUID == uid)
					.findAny();
				
				if (infoOpt.isPresent()) {
					return Optional.of(new ModelError("Formula of object with UID #" + uid + " has an error in formula \"" + badFormulaName + "\"", Arrays.asList(infoOpt.get())));
				} else {
					System.out.println("Could not find cell with SUID " + uid);
					return Optional.empty();
				}
			} catch (NumberFormatException e) {
				System.out.println("Could not parse the badUIDStr: " + badUIDStr);
				return Optional.empty();
			}
		}

		return Optional.of(new ModelError("An unexpected error occurred while evaluating all the formula expressions."));
	}

}
