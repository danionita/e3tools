package design.checker.checks;

import java.util.Arrays;
import java.util.Optional;

import javax.swing.JOptionPane;

import com.hp.hpl.jena.rdf.model.Model;

import design.E3Graph;
import design.Main;
import design.checker.E3ModelCheck;
import design.checker.ModelError;
import design.export.RDFExport;
import e3fraud.model.EvaluatedModel;
import e3fraud.model.EvaluatedModel.ModelOrError;

public class CorrectFormulaCheck implements E3ModelCheck {

	@Override
	public Optional<ModelError> check(E3Graph graph) {

		RDFExport rdfExporter = new RDFExport(graph, true, true, true);

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
				return Optional.of(new ModelError("Object #" + uid + " has an error in the expression for \"" + badFormulaName + "\"", Arrays.asList(uid)));
			} catch (NumberFormatException e) {
				System.out.println("Could not parse the badUIDStr: " + badUIDStr);
				return Optional.empty();
			}
		}

		return Optional.of(new ModelError("An unexpected error occurred while evaluating all the formula expressions."));
	}

}
