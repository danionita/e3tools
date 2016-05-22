/*******************************************************************************
 * Copyright (C) 2016 Bob Rubbens
 *  
 *  
 * This file is part of e3tool.
 *  
 * e3tool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * e3tool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with e3tool.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package design.main;

import java.io.StringWriter;
import java.util.function.Function;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.mxgraph.view.mxGraph;

import design.main.Info.Actor;
import design.main.Info.Base;
import design.main.Info.MarketSegment;
import design.main.Info.ValueActivity;
import design.main.Info.ValueInterface;
import design.vocabulary.E3value;

public class RDFExport {
	private final mxGraph graph;
	private String result;
	
	public RDFExport(mxGraph graph) {
		this.graph = graph;
		
		convertToRdf();
	}
	
	private void convertToRdf() {
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefix("a", E3value.getURI());
		String base = "http://www.cs.vu.nl/~gordijn/TestModel#";
		
		// Create model resource
		long modelSUID = Info.getSUID();
		Resource modelRes = model.createResource(base + modelSUID, E3value.model);
		modelRes.addProperty(E3value.e3_has_name, "model" + modelSUID);
		modelRes.addProperty(E3value.e3_has_uid, "" + modelSUID);
		
		// Create diagram resource
		long diagramSUID = Info.getSUID();
		Resource diagramRes = model.createResource(base + diagramSUID, E3value.diagram);
		diagramRes.addProperty(E3value.e3_has_name, "diagram" + diagramSUID);
		diagramRes.addProperty(E3value.e3_has_uid, "" + diagramSUID);
		
		Function<Long, Resource> getResource = suid -> {
			Resource res = model.createResource(base + suid);
			
			res.addProperty(E3value.e3_has_uid, "" + suid);
			res.addProperty(E3value.mc_in_mo, modelRes);
			res.addProperty(E3value.mc_in_di, diagramRes);
			modelRes.addProperty(E3value.mo_has_mc, res);
			diagramRes.addProperty(E3value.di_has_mc, res);
			
			return res;
		};
		
		for (Object cell : Utils.getAllCells(graph)) {
			Base value = (Base) graph.getModel().getValue(cell);
			
			if (value == null) continue;
			
			Resource res = getResource.apply(value.getSUID());
			res.addProperty(E3value.e3_has_name, value.name);
			
			// TODO: What if the value part of the formula is empty? Put zero there or just leave it empty?
			for (String key : value.formulas.keySet()) {
				res.addProperty(E3value.e3_has_formula, key + "=" + value.formulas.get(key));
			}
			
			if (value instanceof Actor) {
				res.addProperty(RDF.type, E3value.elementary_actor);
				
				for (Object child : Utils.getAllCells(graph, cell)) {
					Base childValue = (Base) graph.getModel().getValue(child);
					
					if (childValue instanceof ValueInterface) {
						res.addProperty(E3value.ac_has_vi, getResource.apply(childValue.getSUID()));
					}
				}
			} else if (value instanceof MarketSegment) {
				res.addProperty(RDF.type, E3value.market_segment);
			} else if (value instanceof ValueInterface) {
				res.addProperty(RDF.type, E3value.value_interface);
				
				Base parentValue = (Base) graph.getModel().getValue(graph.getModel().getParent(cell));
				Resource parentRes = getResource.apply(parentValue.getSUID());

				if (parentValue instanceof Actor) {
					res.addProperty(E3value.vi_assigned_to_ac, parentRes);
				} else if (parentValue instanceof MarketSegment) {
					res.addProperty(E3value.vi_assigned_to_ms, parentRes);
				} else if (parentValue instanceof ValueActivity) {
					res.addProperty(E3value.vi_assigned_to_va, parentRes);
				}
			}
		}
		
		StringWriter out = new StringWriter();
		model.write(out, "RDF/XML");
		result = out.toString();
		
		
		// TODO
	}
	
	@Override
	public String toString() {
		return result;
	}
}
