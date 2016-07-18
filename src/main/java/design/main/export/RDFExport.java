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
package design.main.export;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.mxgraph.view.mxGraph;

import design.main.E3Graph;
import design.main.Info;
import design.main.Utils;
import design.main.Info.Actor;
import design.main.Info.Base;
import design.main.Info.ConnectionElement;
import design.main.Info.EndSignal;
import design.main.Info.LogicBase;
import design.main.Info.MarketSegment;
import design.main.Info.StartSignal;
import design.main.Info.ValueActivity;
import design.main.Info.ValueExchange;
import design.main.Info.ValueInterface;
import design.main.Info.ValuePort;
import design.main.Utils.EdgeAndSides;
import design.vocabulary.E3value;

public class RDFExport {
	
	private final E3Graph graph;
	private String result;
	
	Map<Long, Resource> offeringIn = new HashMap<>();
	Map<Long, Resource> offeringOut = new HashMap<>();
	Map<String, Resource> valueObject = new HashMap<>();
	public Model model;
	
	public RDFExport(mxGraph graph) {
		this.graph = (E3Graph) graph;
		
		convertToRdf();
	}
	
	private void convertToRdf() {
		model = ModelFactory.createDefaultModel();
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
		
		Function<Long, Resource> getOfferingIn = suid -> {
			if (!offeringIn.containsKey(suid)) {
				offeringIn.put(suid, getResource.apply(Info.getSUID()));
				Resource of = offeringIn.get(suid);
				of.addProperty(E3value.e3_has_name, "in");
				of.addProperty(RDF.type, E3value.value_offering);

				of.addProperty(E3value.vo_in_vi, getResource.apply(suid));
				getResource.apply(suid).addProperty(E3value.vi_consists_of_of, of);
			}
			
			return offeringIn.get(suid);
		};
		
		Function<Long, Resource> getOfferingOut = suid -> {
			if (!offeringOut.containsKey(suid)) {
				offeringOut.put(suid, getResource.apply(Info.getSUID()));
				Resource of = offeringOut.get(suid);
				of.addProperty(E3value.e3_has_name, "out");
				of.addProperty(RDF.type, E3value.value_offering);

				of.addProperty(E3value.vo_in_vi, getResource.apply(suid));
				getResource.apply(suid).addProperty(E3value.vi_consists_of_of, of);
			}
			
			return offeringOut.get(suid);
		};
		
		Function<String, Resource> getValueObject = obj -> {
			if (!valueObject.containsKey(obj)) {
				valueObject.put(obj, getResource.apply(Info.getSUID()));
				Resource reObj = valueObject.get(obj);
				reObj.addProperty(E3value.e3_has_name, obj);
				reObj.addProperty(RDF.type, E3value.value_object);
			}
			
			return valueObject.get(obj);
		};
		
		Map<Object, Flow> flowMap = new HashMap<>();
		
		for (String valueObject : graph.valueObjects) {
			getValueObject.apply(valueObject);
		}
		
		for (Object cell : Utils.getAllCells(graph)) {
			Object cellValue = graph.getModel().getValue(cell);
			
			if (!(
					cellValue instanceof Actor
					|| cellValue instanceof MarketSegment
					|| cellValue instanceof ValueInterface
					|| cellValue instanceof ValueExchange
					|| cellValue instanceof StartSignal
					|| cellValue instanceof EndSignal
					|| cellValue instanceof ConnectionElement
					|| cellValue instanceof ValueActivity
					|| cellValue instanceof ValuePort
			)) continue;
			
			Base value = (Base) cellValue;
			
			System.out.println("Considering: \"" + value.name + "\"");
			
			Resource res = getResource.apply(value.getSUID());

			// Add name
			if (value.name != null) {
				res.addProperty(E3value.e3_has_name, value.name);
			}
			
			// Add formulas
			// TODO: What if the value part of the formula is empty? Put zero there or just leave it empty?
			// I guess for now we'll just leave it "empty", the parser on the other side can deal with it
			for (String key : value.formulas.keySet()) {
				res.addProperty(E3value.e3_has_formula, key + "=" + value.formulas.get(key));
			}
			
			if (value instanceof Actor) {
				res.addProperty(RDF.type, E3value.elementary_actor);
				
				for (Object child : Utils.getChildrenWithValue(graph, cell, ValueInterface.class)) {
					Base childInfo = (Base) graph.getModel().getValue(child);
					res.addProperty(E3value.ac_has_vi, getResource.apply(childInfo.getSUID()));
				}
				
				// TODO: We need an extra RDF thing here, right? Like ac_consist_of_ms or smth
//				for (Object child : Utils.getChildrenWithValue(graph, cell, MarketSegment.class)) {
//					Base childInfo = (Base) graph.getModel().getValue(child);
//					res.addProperty(E3value.consis, o)
//				}
				
				for (Object child : Utils.getChildrenWithValue(graph, cell, ValueActivity.class)) {
					ValueActivity vaInfo = (ValueActivity) graph.getModel().getValue(child);
					Resource vaRes = getResource.apply(vaInfo.getSUID());
					res.addProperty(E3value.el_performs_va, vaRes);
					vaRes.addProperty(E3value.va_performed_by_el, res);
				}
			} else if (value instanceof MarketSegment) {
				res.addProperty(RDF.type, E3value.market_segment);

				for (Object child : Utils.getChildrenWithValue(graph, cell, ValueInterface.class)) {
					Base childInfo = (Base) graph.getModel().getValue(child);
					res.addProperty(E3value.ms_has_vi, getResource.apply(childInfo.getSUID()));
				}

				// TODO: Can't implement this because Dan's E3value class does not have the
				// ms_performs_va and such. The original exporter does export this though?
//				for (Object child : Utils.getChildrenWithValue(graph, cell, ValueActivity.class)) {
//					ValueActivity vaInfo = (ValueActivity) graph.getModel().getValue(child);
//					Resource vaRes = getResource.apply(vaInfo.getSUID());
//					res.addProperty(E3value.ms_performs_va, vaRes);
//					vaRes.addProperty(E3value.va_perormed_by_ms, res);
//				}
			} else if (value instanceof ValueActivity) {
				res.addProperty(RDF.type, E3value.value_activity);

				for (Object child : Utils.getChildrenWithValue(graph, cell, ValueInterface.class)) {
					Base childInfo = (Base) graph.getModel().getValue(child);
					res.addProperty(E3value.va_has_vi, getResource.apply(childInfo.getSUID()));
				}
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
			} else if (value instanceof ValuePort) {
				res.addProperty(RDF.type, E3value.value_port);
				ValuePort vpInfo = (ValuePort) value;
				
				Object viCell = graph.getModel().getParent(cell);
				ValueInterface viInfo = (ValueInterface) graph.getModel().getValue(viCell);

				if (((ValuePort) value).incoming) {
					Resource offIn = getOfferingIn.apply(viInfo.getSUID());
					offIn.addProperty(E3value.vo_consists_of_vp, res);					
					res.addProperty(E3value.vp_in_vo, offIn);
				} else {
					Resource offOut = getOfferingOut.apply(viInfo.getSUID());
					offOut.addProperty(E3value.vo_consists_of_vp, res);					
					res.addProperty(E3value.vp_in_vo, offOut);
				}
				
				// False = in
				// True = out
				res.addProperty(E3value.vp_has_dir, (!vpInfo.incoming) + "");
				
				assert(graph.getModel().getEdgeCount(cell) < 2);
				if (graph.getModel().getEdgeCount(cell) == 1) {
					System.out.println("One!");
					Object valueExchange = graph.getModel().getEdgeAt(cell, 0);
					ValueExchange veInfo = (ValueExchange) graph.getModel().getValue(valueExchange);
					Resource veRes = getResource.apply(veInfo.getSUID());
					
					if (vpInfo.incoming) {
						res.addProperty(E3value.vp_in_connects_ve, veRes);
						veRes.addProperty(E3value.ve_has_in_po, res);
					} else {
						res.addProperty(E3value.vp_out_connects_ve, veRes);
						veRes.addProperty(E3value.ve_has_out_po, res);
					}
					
					if (veInfo.valueObject != null) {
						Resource valueObjectRes = getValueObject.apply(veInfo.valueObject);
						res.addProperty(E3value.vp_requests_offers_vo, valueObjectRes);
						valueObjectRes.addProperty(E3value.vo_offered_requested_by_vp, res);
					}
				}
			} else if (value instanceof ValueExchange) {
				res.addProperty(RDF.type, E3value.value_exchange);
			} else if (value instanceof StartSignal) {
				res.addProperty(RDF.type, E3value.start_stimulus);
				
				ConnectionVisitor cv = new ConnectionVisitor(graph, getResource, flowMap);
				try {
					System.out.println("Starting visitor");
					cv.accept(cell);
				} catch (MalformedFlowException e) {
					System.out.println("Malformed flow! Cause: " + e.subject);
					e.printStackTrace();
				}
			} else if (value instanceof EndSignal) {
				res.addProperty(RDF.type, E3value.end_stimulus);
			} else if (value instanceof ConnectionElement) {
				res.addProperty(RDF.type, E3value.connection_element);
				System.out.println("Connection element with SUID: " + value.getSUID() + " and name: " + value.name);
			} else if (value instanceof LogicBase) {
				LogicBase lbInfo = (LogicBase) value;
				if (((LogicBase) value).isOr) {
					res.addProperty(RDF.type, E3value.OR_node);
				} else {
					res.addProperty(RDF.type, E3value.AND_node);
				}
			}
		}
		
		// Convert to RDF
		StringWriter out = new StringWriter();
		model.write(out, "RDF/XML");
		result = out.toString();
		
		System.out.println(result);
	}
	
	@Override
	public String toString() {
		return result;
	}
}
