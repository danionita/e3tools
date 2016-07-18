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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.view.mxGraph;

import design.main.Info.Actor;
import design.main.Info.Base;
import design.main.Info.ConnectionElement;
import design.main.Info.EndSignal;
import design.main.Info.LogicBase;
import design.main.Info.LogicDot;
import design.main.Info.MarketSegment;
import design.main.Info.SignalDot;
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
	
	enum Flow {
		SEND,
		RECEIVE
	}
	
	public static class MalformedFlowException extends Exception {
		private static final long serialVersionUID = 271250946666843043L;

		public Object subject;

		public MalformedFlowException() {}
		
		public MalformedFlowException(String message) {
			super(message);
			this.subject = null;
		}
		
		public MalformedFlowException(String message, Object subject) {
			super(message);
			this.subject = subject;
		}
		
		public MalformedFlowException(Object subject) {
			this.subject = subject;
		}
	}
	
	public static class ConnectionVisitor {
		mxGraph graph;
		mxGraphModel model;
		Function<Long, Resource> getResource;
		Map<Object, Flow> flowMap;
		
		ConnectionVisitor(mxGraph graph, Function<Long, Resource> getResource, Map<Object, Flow> flowMap) {
			this.graph = graph;
			model = (mxGraphModel) graph.getModel();
			
			this.getResource = getResource;
			this.flowMap = flowMap;
		}
		
		void setSend(Object obj) throws MalformedFlowException {
			if (flowMap.containsKey(obj)) {
				if (flowMap.get(obj) != Flow.SEND) {
					throw new MalformedFlowException(obj);
				}
			} else {
				flowMap.put(obj, Flow.SEND);
			}
		}

		void setReceive(Object obj) throws MalformedFlowException {
			if (flowMap.containsKey(obj)) {
				if (flowMap.get(obj) != Flow.RECEIVE) {
					throw new MalformedFlowException(obj);
				}
			} else {
				flowMap.put(obj, Flow.RECEIVE);
			}
		}
		
		/**
		 * 
		 * @param startSignal The startsignal node in the graph
		 * @throws MalformedFlowException 
		 */
		void accept(Object startSignal) throws MalformedFlowException {
			StartSignal ssInfo = (StartSignal) model.getValue(startSignal);
			visit(startSignal, ssInfo);
		}
		
		void visit(Object ss, StartSignal ssInfo) throws MalformedFlowException {
			System.out.println("Visiting StartSignal");
			
			Resource res = getResource.apply(ssInfo.getSUID());
			Object child = model.getChildAt(ss, 0);
			
			setSend(child);
			
			if (Utils.EdgeAndSides.hasDotChildEdge(graph, ss)) {
				Object edge = model.getEdgeAt(child, 0);
				ConnectionElement edgeInfo = (ConnectionElement) model.getValue(edge);
				
				Resource edgeRes = getResource.apply(edgeInfo.getSUID());
				res.addProperty(E3value.de_down_ce, edgeRes);
				
				visit(child, edge, edgeInfo);
			}
		}
		
		void visit(Object upDot, Object ce, ConnectionElement ceInfo) throws MalformedFlowException {
			System.out.println("Visiting ConnectionElement");
			
			Resource ceRes = getResource.apply(ceInfo.getSUID());
			
			Object downDot = Utils.getOpposite(graph, ce, upDot);
			Object opposite = model.getParent(downDot);
			Base oppositeValue = (Base) model.getValue(opposite);
			
			Object up = model.getParent(upDot);
			Base upValue = (Base) model.getValue(up);
			
			ceRes.addProperty(E3value.ce_with_up_de, getResource.apply(upValue.getSUID()));
			ceRes.addProperty(E3value.ce_with_down_de, getResource.apply(oppositeValue.getSUID()));
			
			setSend(upDot);
			setReceive(downDot);
			
			if (oppositeValue instanceof ValueInterface) {
				Resource viRes = getResource.apply(oppositeValue.getSUID());
				viRes.addProperty(E3value.de_up_ce, ceRes);
				
				// TODO: This can be moved to its own function
				List<Object> ports = Utils.getChildrenWithValue(graph, opposite, ValuePort.class);
				for (Object port : ports) {
					if (model.getEdgeCount(port) > 0) {
						Object otherPort = Utils.getOpposite(graph, model.getEdgeAt(port, 0), port);
						Object valueInterface = model.getParent(otherPort);
						
						System.out.println("Source value interface: " + oppositeValue.getSUID());
						System.out.println("End value interface: " + ((Base) model.getValue(valueInterface)).getSUID());
						
						Object otherDot = Utils.getChildrenWithValue(graph, valueInterface, SignalDot.class).get(0);
						
						if (flowMap.containsKey(otherDot)) {
							// If flowmap contains otherDot, just check if it's set the right way, but don't
							// visit it any further (it has already been visited)
							setSend(otherDot);
						} else {
							setSend(otherDot);
							
							if (model.getEdgeCount(otherDot) == 1) {
								Object edge = model.getEdgeAt(otherDot, 0);
								ConnectionElement edgeInfo = (ConnectionElement) model.getValue(edge);

								Base viInfo = (Base) model.getValue(valueInterface);
								Resource otherViRes = getResource.apply(viInfo.getSUID());
								otherViRes.addProperty(E3value.de_down_ce, getResource.apply(edgeInfo.getSUID()));
								
								visit(otherDot, edge, edgeInfo);
							}
						}
					}
				}
			} else if (oppositeValue instanceof LogicBase) {
				LogicBase lbInfo = (LogicBase) model.getValue(opposite);
				visit(downDot, opposite, lbInfo);
			} else if (oppositeValue instanceof EndSignal) {
				EndSignal esInfo = (EndSignal) model.getValue(opposite);
				visit(opposite, esInfo);
			} else {
				throw new MalformedFlowException("This connectionelement is not connected to a proper ending", opposite);
			}
		}
		
		void visit(Object upDot, Object lb, LogicBase lbInfo) throws MalformedFlowException {
			System.out.println("Visiting logicbase");
			
			List<Object> dots = Utils.getChildrenWithValue(graph, lb, LogicDot.class);
			int unitPos = -1;
			
			for (int i = 0; i < dots.size(); i++) {
				LogicDot ld = (LogicDot) model.getValue(dots.get(i));
				if (ld.isUnit) {
					unitPos = i;
					break;
				}
			}
			
			assert(unitPos != -1);
			
			Object unitDot = dots.remove(unitPos);
			
			Resource lbRes = getResource.apply(lbInfo.getSUID());
			
			if (unitDot == upDot) {
				setReceive(upDot);
				
				Object ce = model.getEdgeAt(upDot, 0);
				ConnectionElement ceInfo = (ConnectionElement) model.getValue(ce);
				lbRes.addProperty(E3value.de_up_ce, getResource.apply(ceInfo.getSUID()));
				
				for (Object dot : dots) {
					if (flowMap.containsKey(dot)) {
						setSend(dot);
					} else {
						setSend(dot);
						
						if (model.getEdgeCount(dot) == 1) {
							Object edge = model.getEdgeAt(dot, 0);
							ConnectionElement edgeInfo = (ConnectionElement) model.getValue(edge);
							lbRes.addProperty(E3value.de_down_ce, getResource.apply(edgeInfo.getSUID()));
							visit(dot, edge, edgeInfo);
						}
					}
				}
			} else {
				if (flowMap.containsKey(unitDot)) {
					setSend(unitDot);
				} else {
					setSend(unitDot);

					if (model.getEdgeCount(unitDot) == 1) {
						Object ce = model.getEdgeAt(unitDot, 0);
						ConnectionElement ceInfo = (ConnectionElement) model.getValue(ce);
						lbRes.addProperty(E3value.de_down_ce, getResource.apply(ceInfo.getSUID()));
					
						visit(unitDot, ce, ceInfo);
					}
				}

				for (Object dot : dots) {
					setReceive(dot);

					if (model.getEdgeCount(dot) == 1) {
						Object edge = model.getEdgeAt(dot, 0);
						ConnectionElement edgeInfo = (ConnectionElement) model.getValue(edge);
						lbRes.addProperty(E3value.de_up_ce, getResource.apply(edgeInfo.getSUID()));
					}
				}
			}
		}
		
		void visit(Object es, EndSignal esInfo) {
			System.out.println("Visiting end-signal");
			
			Object edge = model.getEdgeAt(model.getChildAt(es, 0), 0);
			ConnectionElement edgeInfo = (ConnectionElement) model.getValue(edge);
			
			Resource endRes = getResource.apply(esInfo.getSUID());
			endRes.addProperty(E3value.de_up_ce, getResource.apply(edgeInfo.getSUID()));
		}
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
				
				List<Object> valuePorts = Utils.getChildrenWithValue(graph, cell, ValuePort.class);

				for (Object valuePort : valuePorts) {
					ValuePort vpInfo = (ValuePort) graph.getModel().getValue(valuePort);
					Resource vpRes = getResource.apply(vpInfo.getSUID());
					vpRes.addProperty(RDF.type, E3value.value_port);

					if (vpInfo.incoming) {
						Resource offIn = getOfferingIn.apply(value.getSUID());
						offIn.addProperty(E3value.vo_consists_of_vp, vpRes);					
						vpRes.addProperty(E3value.vp_in_vo, offIn);
					} else {
						Resource offOut = getOfferingOut.apply(value.getSUID());
						offOut.addProperty(E3value.vo_consists_of_vp, vpRes);					
						vpRes.addProperty(E3value.vp_in_vo, offOut);
					}
					
					// False = in
					// True = out
					vpRes.addProperty(E3value.vp_has_dir, (!vpInfo.incoming) + "");
					
					assert(graph.getModel().getEdgeCount(valuePort) < 2);
					System.out.println("Checking edges for cell...");
					System.out.println("TESTING");
					if (graph.getModel().getEdgeCount(valuePort) == 1) {
						System.out.println("One!");
						Object valueExchange = graph.getModel().getEdgeAt(valuePort, 0);
						ValueExchange veInfo = (ValueExchange) graph.getModel().getValue(valueExchange);
						Resource veRes = getResource.apply(veInfo.getSUID());
						
						if (vpInfo.incoming) {
							vpRes.addProperty(E3value.vp_in_connects_ve, veRes);
							veRes.addProperty(E3value.ve_has_in_po, vpRes);
						} else {
							vpRes.addProperty(E3value.vp_out_connects_ve, veRes);
							veRes.addProperty(E3value.ve_has_out_po, vpRes);
						}
						
						if (veInfo.valueObject != null) {
							Resource valueObjectRes = getValueObject.apply(veInfo.valueObject);
							vpRes.addProperty(E3value.vp_requests_offers_vo, valueObjectRes);
							valueObjectRes.addProperty(E3value.vo_offered_requested_by_vp, vpRes);
						}
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
