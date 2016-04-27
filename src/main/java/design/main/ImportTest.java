package design.main;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.mxOrganicLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;

import design.main.Info.Actor;
import design.main.Info.Base;
import design.main.Info.MarketSegment;
import design.main.Info.ValueActivity;
import design.main.Info.ValueExchange;
import design.main.Info.ValuePort;
import design.vocabulary.E3value;

public class ImportTest {

	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		Main t = new Main();
		
		t.graphComponent.setEnabled(false);
		
		// Import test
        //Load file
        InputStream inputStream = null;
        String file = "All.rdf";
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException ex) {
        	System.out.println("Whoops, file " + file + "  not found");
        	return;
        }

        //First, replace undeline (_) with dashes(-)
        //This is because e3valuetoolkit does a bad job at exporting RDF and outputs _ instead of -
        SearchAndReplaceInputStream fixedInputStream = new SearchAndReplaceInputStream(inputStream, "_", "-");

        //creating THE JENA MODEL
        Model model = ModelFactory.createDefaultModel();
        model.read(fixedInputStream, null);

        t.graph.getModel().beginUpdate();
        try {
			// Import ValueObjects
			{
				Main.valueObjects.clear();
				
				ResIterator it = model.listSubjectsWithProperty(RDF.type, E3value.value_object);
				while (it.hasNext()) {
					Resource res = it.next();
					String valueObject = res.getProperty(E3value.e3_has_name).getString();
					if (!Main.valueObjects.contains(valueObject)) Main.valueObjects.add(valueObject);
				}
			}
			
			Map<Resource, Object> resourceToCell = new HashMap<>();
			
			// Import Actors
			{
				ResIterator it = model.listSubjectsWithProperty(RDF.type, E3value.elementary_actor);
				while (it.hasNext()) {
					Resource res = it.next();
					String name = res.getProperty(E3value.e3_has_name).getString();
					int UID = res.getProperty(E3value.e3_has_uid).getInt();
					StmtIterator formulas = res.listProperties(E3value.e3_has_formula);
					
					mxCell actor = (mxCell) Main.tools.clone(Main.tools.actor);
					Actor actorValue = (Actor) actor.getValue();
					actorValue.formulas.clear();

					actorValue.setSUID(UID);
					actorValue.name = name;

					while(formulas.hasNext()) {
						String formula = formulas.next().getString();
						String formulaName = formula.split("=")[0];
						String value = Utils.concatTail(formula.split("="));

						actorValue.formulas.put(formulaName, value);
					}
					
					t.graph.addCell(actor);
					
					resourceToCell.put(res, actor);
				}
			}
			
			// Import MarketSegments
			{
				ResIterator it = model.listSubjectsWithProperty(RDF.type, E3value.market_segment);
				while (it.hasNext()) {
					Resource res = it.next();
					String name = res.getProperty(E3value.e3_has_name).getString();
					int UID = res.getProperty(E3value.e3_has_uid).getInt();
					StmtIterator formulas = res.listProperties(E3value.e3_has_formula);
					
					mxCell marketSegment = (mxCell) Main.tools.clone(Main.tools.marketSegment);
					MarketSegment marketSegmentValue = (MarketSegment) marketSegment.getValue();
					marketSegmentValue.formulas.clear();

					marketSegmentValue.setSUID(UID);
					marketSegmentValue.name = name;

					while(formulas.hasNext()) {
						String formula = formulas.next().getString();
						String formulaName = formula.split("=")[0];
						String value = Utils.concatTail(formula.split("="));

						marketSegmentValue.formulas.put(formulaName, value);
						
						System.out.println("Formula: " + formula);
					}
					
					t.graph.addCell(marketSegment);

					resourceToCell.put(res, marketSegment);
				}
			}
			
			// Import ValueActivities
			{
				ResIterator it = model.listSubjectsWithProperty(RDF.type, E3value.value_activity);
				while (it.hasNext()) {
					Resource res = it.next();
					String name = res.getProperty(E3value.e3_has_name).getString();
					int UID = res.getProperty(E3value.e3_has_uid).getInt();
					StmtIterator formulas = res.listProperties(E3value.e3_has_formula);
					
					mxCell valueActivity = (mxCell) Main.tools.clone(Main.tools.valueActivity);
					ValueActivity valueActivityValue = (ValueActivity) valueActivity.getValue();
					valueActivityValue.formulas.clear();

					valueActivityValue.setSUID(UID);
					valueActivityValue.name = name;

					while(formulas.hasNext()) {
						String formula = formulas.next().getString();
						String formulaName = formula.split("=")[0];
						String value = Utils.concatTail(formula.split("="));

						valueActivityValue.formulas.put(formulaName, value);
					}
					
					t.graph.addCell(valueActivity);
					
					resourceToCell.put(res, valueActivity);
				}
			}
			
			// Import ValueInterfaces
			{
				ResIterator it = model.listSubjectsWithProperty(RDF.type, E3value.value_interface);
				while (it.hasNext()) {
					Resource res = it.next();

					Consumer<Statement> printCellName = p -> {
						Object parent = resourceToCell.get(p.getResource());
						mxCell valueInterface = (mxCell) t.tools.clone(t.tools.valueInterface);
						
						String name = res.getProperty(E3value.e3_has_name).getString();
						int UID = res.getProperty(E3value.e3_has_uid).getInt();
						
						Base value = (Base) valueInterface.getValue();
						value.name = name;
						value.setSUID(UID);
						
						valueInterface.getGeometry().setX(0);
						valueInterface.getGeometry().setY(0);
						
						t.graph.addCell(valueInterface, parent);
						
						List<Object> toRemove = new ArrayList<Object>();
						for (int i = 0; i < valueInterface.getChildCount(); i++) {
							Object child = valueInterface.getChildAt(i);
							Base childValue = (Base) t.graph.getModel().getValue(child);
							if (childValue instanceof ValuePort) toRemove.add(child);
						}
						toRemove.forEach(o -> t.graph.getModel().remove(o));
						
						E3Graph.straightenValueInterface(t.graph, valueInterface);
						
						resourceToCell.put(res, valueInterface);
					};

					if (res.hasProperty(E3value.vi_assigned_to_ac)) {
						printCellName.accept(res.getProperty(E3value.vi_assigned_to_ac));
					}
					if (res.hasProperty(E3value.vi_assigned_to_ms)) {
						printCellName.accept(res.getProperty(E3value.vi_assigned_to_ms));
					}
					if (res.hasProperty(E3value.vi_assigned_to_va)) {
						printCellName.accept(res.getProperty(E3value.vi_assigned_to_va));
					}
				}
			}
			
			// Import valueOfferings (wrapper objects of valuePorts, because the "name" property was already taken
			// by something else so they needed a wrapper?)
			{
				ResIterator it = model.listSubjectsWithProperty(RDF.type, E3value.value_offering);
				while (it.hasNext()) {
					Resource res = it.next();
					
					Object parent = resourceToCell.get(res.getProperty(E3value.vo_in_vi).getResource());
					boolean incoming = res.getProperty(E3value.e3_has_name).getString().equals("in");
					
					mxCell valuePort = (mxCell) E3Graph.addValuePort(t.graph, (mxICell) parent, incoming);
					
					Resource valuePortRes = res.getProperty(E3value.vo_consists_of_vp).getResource();
					String name = valuePortRes.getProperty(E3value.e3_has_name).getString();
					int UID = valuePortRes.getProperty(E3value.e3_has_uid).getInt();
					
					Base valuePortValue = Utils.base(t.graph, valuePort);
					valuePortValue.name = name;
					valuePortValue.setSUID(UID);
					t.graph.getModel().setValue(valuePort, valuePortValue);
					
					resourceToCell.put(res, valuePort);
					resourceToCell.put(valuePortRes, valuePort);
				}
			}
			
			// Import valueExchanges
			{
				ResIterator it = model.listResourcesWithProperty(RDF.type, E3value.value_exchange);
				
				while (it.hasNext()) {
					Resource res = it.next();
		
					String name = res.getProperty(E3value.e3_has_name).getString();
					int UID = res.getProperty(E3value.e3_has_uid).getInt();
					
					System.out.println(name + UID);
					
					ValueExchange veValue = new ValueExchange();
					veValue.formulas.clear();
					veValue.name = name;
					veValue.setSUID(UID);
					
					StmtIterator formulas = res.listProperties(E3value.e3_has_formula);
					
					while(formulas.hasNext()) {
						String formula = formulas.next().getString();
						String formulaName = formula.split("=")[0];
						String value = Utils.concatTail(formula.split("="));

						veValue.formulas.put(formulaName, value);
					}
					
					Object left = resourceToCell.get(res.getProperty(E3value.ve_has_in_po).getResource());
					Object right = resourceToCell.get(res.getProperty(E3value.ve_has_out_po).getResource());
					
					Object valueExchange = t.graph.insertEdge(t.graph.getDefaultParent(), null, null, left, right);
					// Have to set value here manually, the value parameter o the insertEdge method doesn't seem to work
					t.graph.getModel().setValue(valueExchange, veValue);
					
					resourceToCell.put(res, valueExchange);
				}
			}
			
			// This was to enable editing again, so the state can't be altered while
			// a model is loaded. I'm not sure if it works though
			t.graphComponent.setEnabled(true);

			// Layout the graph nicely
			mxIGraphLayout layout = new mxOrganicLayout(t.graph);
//			mxIGraphLayout layout = new mxHierarchicalLayout(t.graph);
//			mxIGraphLayout layout = new mxOrthogonalLayout(t.graph);
//			mxIGraphLayout layout = new mxPartitionLayout(t.graph);
//			mxIGraphLayout layout = new mxStackLayout(t.graph);
//			mxIGraphLayout layout = new mxCompactTreeLayout(t.graph);
			
			layout.execute(t.graph.getDefaultParent());
			
			// TODO: Recursively layout all actors/marketsegments/valueactivities
        } finally {
        	t.graph.getModel().endUpdate();
        }
	}
}
