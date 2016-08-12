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
package design;

import com.thoughtworks.xstream.XStream;

public class ImportTest {
	public static class Person {
		private String firstname;
		private String lastname;
		private PhoneNumber phone;
		private PhoneNumber fax;
		
		public Person(String firstname, String lastname) {
			this.firstname = firstname;
			this.lastname = lastname;
		}
		
		public Person() {}

		public String getFirstname() {
			return firstname;
		}
		public void setFirstname(String firstname) {
			this.firstname = firstname;
		}
		public String getLastname() {
			return lastname;
		}
		public void setLastname(String lastname) {
			this.lastname = lastname;
		}
		public PhoneNumber getPhone() {
			return phone;
		}
		public void setPhone(PhoneNumber phone) {
			this.phone = phone;
		}
		public PhoneNumber getFax() {
			return fax;
		}
		public void setFax(PhoneNumber fax) {
			this.fax = fax;
		}

		@Override
		public String toString() {
			return "Person [firstname=" + firstname + ", lastname=" + lastname + ", phone=" + phone + ", fax=" + fax
					+ "]";
		}
	}

	public static class PhoneNumber {
		private int code;
		private String number;
		
		public PhoneNumber(int code, String number) {
			super();
			this.code = code;
			this.number = number;
		}
		
		public PhoneNumber() {}

		public int getCode() {
			return code;
		}
		public void setCode(int code) {
			this.code = code;
		}
		public String getNumber() {
			return number;
		}
		public void setNumber(String number) {
			this.number = number;
		}

		@Override
		public String toString() {
			return "PhoneNumber [code=" + code + ", number=" + number + "]";
		}
	}

	// @SuppressWarnings("static-access")
	public static void main(String[] args) {
		XStream xstream = new XStream();
		
		Person joe = new Person("Joe", "Walnes\nSama");
		joe.setPhone(new PhoneNumber(123, "1234-456"));
		joe.setFax(new PhoneNumber(123, "9999-999"));
		
		
		System.out.println(joe);
		String xml = xstream.toXML(joe);
		
		System.out.println("XML: \n");
		System.out.println(xml);
		System.out.println("END XML\n");
		
		Person newJoe = (Person)xstream.fromXML(xml);
		
		System.out.println(newJoe);
		
//		
//		
//		Main t = new Main();
//		
//		// t.graphComponent.setEnabled(false);
//		
//		// Import test
//        //Load file
//        InputStream inputStream = null;
//        String file = "All.rdf";
//        try {
//            inputStream = new FileInputStream(file);
//        } catch (FileNotFoundException ex) {
//        	System.out.println("Whoops, file " + file + "  not found");
//        	return;
//        }
//
//        //First, replace undeline (_) with dashes(-)
//        //This is because e3valuetoolkit does a bad job at exporting RDF and outputs _ instead of -
//        SearchAndReplaceInputStream fixedInputStream = new SearchAndReplaceInputStream(inputStream, "_", "-");
//
//        //creating THE JENA MODEL
//        Model model = ModelFactory.createDefaultModel();
//        model.read(fixedInputStream, null);
//
//        t.getCurrentGraph().getModel().beginUpdate();
//        try {
//			// Import ValueObjects
//			{
//				List<String> valueObjects = t.getCurrentGraph().valueObjects;
//				valueObjects.clear();
//				
//				ResIterator it = model.listSubjectsWithProperty(RDF.type, E3value.value_object);
//				while (it.hasNext()) {
//					Resource res = it.next();
//					String valueObject = res.getProperty(E3value.e3_has_name).getString();
//					if (!valueObjects.contains(valueObject)) valueObjects.add(valueObject);
//				}
//			}
//			
//			Map<Resource, Object> resourceToCell = new HashMap<>();
//			
//			// Import Actors
//			{
//				ResIterator it = model.listSubjectsWithProperty(RDF.type, E3value.elementary_actor);
//				while (it.hasNext()) {
//					Resource res = it.next();
//					String name = res.getProperty(E3value.e3_has_name).getString();
//					int UID = res.getProperty(E3value.e3_has_uid).getInt();
//					StmtIterator formulas = res.listProperties(E3value.e3_has_formula);
//					
//					mxCell actor = (mxCell) Main.globalTools.clone(Main.globalTools.actor);
//					Actor actorValue = (Actor) actor.getValue();
//					actorValue.formulas.clear();
//
//					actorValue.setSUID(UID);
//					actorValue.name = name;
//
//					while(formulas.hasNext()) {
//						String formula = formulas.next().getString();
//						String formulaName = formula.split("=")[0];
//						String value = Utils.concatTail(formula.split("="));
//
//						actorValue.formulas.put(formulaName, value);
//					}
//					
//					t.getCurrentGraph().addCell(actor);
//					
//					resourceToCell.put(res, actor);
//				}
//			}
//			
//			// Import MarketSegments
//			{
//				ResIterator it = model.listSubjectsWithProperty(RDF.type, E3value.market_segment);
//				while (it.hasNext()) {
//					Resource res = it.next();
//					String name = res.getProperty(E3value.e3_has_name).getString();
//					int UID = res.getProperty(E3value.e3_has_uid).getInt();
//					StmtIterator formulas = res.listProperties(E3value.e3_has_formula);
//					
//					mxCell marketSegment = (mxCell) Main.globalTools.clone(Main.globalTools.marketSegment);
//					MarketSegment marketSegmentValue = (MarketSegment) marketSegment.getValue();
//					marketSegmentValue.formulas.clear();
//
//					marketSegmentValue.setSUID(UID);
//					marketSegmentValue.name = name;
//
//					while(formulas.hasNext()) {
//						String formula = formulas.next().getString();
//						String formulaName = formula.split("=")[0];
//						String value = Utils.concatTail(formula.split("="));
//
//						marketSegmentValue.formulas.put(formulaName, value);
//						
//						System.out.println("Formula: " + formula);
//					}
//					
//					t.getCurrentGraph().addCell(marketSegment);
//
//					resourceToCell.put(res, marketSegment);
//				}
//			}
//			
//			// Import ValueActivities
//			{
//				ResIterator it = model.listSubjectsWithProperty(RDF.type, E3value.value_activity);
//				while (it.hasNext()) {
//					Resource res = it.next();
//					String name = res.getProperty(E3value.e3_has_name).getString();
//					int UID = res.getProperty(E3value.e3_has_uid).getInt();
//					StmtIterator formulas = res.listProperties(E3value.e3_has_formula);
//					
//					mxCell valueActivity = (mxCell) Main.globalTools.clone(Main.globalTools.valueActivity);
//					ValueActivity valueActivityValue = (ValueActivity) valueActivity.getValue();
//					valueActivityValue.formulas.clear();
//
//					valueActivityValue.setSUID(UID);
//					valueActivityValue.name = name;
//
//					while(formulas.hasNext()) {
//						String formula = formulas.next().getString();
//						String formulaName = formula.split("=")[0];
//						String value = Utils.concatTail(formula.split("="));
//
//						valueActivityValue.formulas.put(formulaName, value);
//					}
//					
//					t.getCurrentGraph().addCell(valueActivity);
//					
//					resourceToCell.put(res, valueActivity);
//				}
//			}
//			
//			// Import ValueInterfaces
//			{
//				ResIterator it = model.listSubjectsWithProperty(RDF.type, E3value.value_interface);
//				while (it.hasNext()) {
//					Resource res = it.next();
//
//					Consumer<Statement> printCellName = p -> {
//						Object parent = resourceToCell.get(p.getResource());
//						mxCell valueInterface = (mxCell) t.globalTools.clone(t.globalTools.valueInterface);
//						
//						String name = res.getProperty(E3value.e3_has_name).getString();
//						int UID = res.getProperty(E3value.e3_has_uid).getInt();
//						
//						Base value = (Base) valueInterface.getValue();
//						value.name = name;
//						value.setSUID(UID);
//						
//						valueInterface.getGeometry().setX(0);
//						valueInterface.getGeometry().setY(0);
//						
//						t.getCurrentGraph().addCell(valueInterface, parent);
//						
//						List<Object> toRemove = new ArrayList<Object>();
//						for (int i = 0; i < valueInterface.getChildCount(); i++) {
//							Object child = valueInterface.getChildAt(i);
//							Base childValue = (Base) t.getCurrentGraph().getModel().getValue(child);
//							if (childValue instanceof ValuePort) toRemove.add(child);
//						}
//						t.getCurrentGraph().removeCells(toRemove.toArray());
//						
//						E3Graph.straightenValueInterface(t.getCurrentGraph(), valueInterface);
//						
//						resourceToCell.put(res, valueInterface);
//					};
//
//					if (res.hasProperty(E3value.vi_assigned_to_ac)) {
//						printCellName.accept(res.getProperty(E3value.vi_assigned_to_ac));
//					}
//					if (res.hasProperty(E3value.vi_assigned_to_ms)) {
//						printCellName.accept(res.getProperty(E3value.vi_assigned_to_ms));
//					}
//					if (res.hasProperty(E3value.vi_assigned_to_va)) {
//						printCellName.accept(res.getProperty(E3value.vi_assigned_to_va));
//					}
//				}
//			}
//			
//			// Import valueOfferings (wrapper objects of valuePorts, because the "name" property was already taken
//			// by something else so they needed a wrapper?)
//			{
//				ResIterator it = model.listSubjectsWithProperty(RDF.type, E3value.value_offering);
//				while (it.hasNext()) {
//					Resource res = it.next();
//					
//					Object parent = resourceToCell.get(res.getProperty(E3value.vo_in_vi).getResource());
//					boolean incoming = res.getProperty(E3value.e3_has_name).getString().equals("in");
//					
//					mxCell valuePort = (mxCell) E3Graph.addValuePort(t.getCurrentGraph(), (mxICell) parent, incoming);
//					
//					Resource valuePortRes = res.getProperty(E3value.vo_consists_of_vp).getResource();
//					String name = valuePortRes.getProperty(E3value.e3_has_name).getString();
//					int UID = valuePortRes.getProperty(E3value.e3_has_uid).getInt();
//					
//					Base valuePortValue = Utils.base(t.getCurrentGraph(), valuePort);
//					valuePortValue.name = name;
//					valuePortValue.setSUID(UID);
//					t.getCurrentGraph().getModel().setValue(valuePort, valuePortValue);
//					
//					resourceToCell.put(res, valuePort);
//					resourceToCell.put(valuePortRes, valuePort);
//				}
//			}
//			
//			// Import valueExchanges
//			{
//				ResIterator it = model.listResourcesWithProperty(RDF.type, E3value.value_exchange);
//				
//				while (it.hasNext()) {
//					Resource res = it.next();
//		
//					String name = res.getProperty(E3value.e3_has_name).getString();
//					int UID = res.getProperty(E3value.e3_has_uid).getInt();
//					
//					System.out.println(name + UID);
//					
//					ValueExchange veValue = new ValueExchange();
//					veValue.formulas.clear();
//					veValue.name = name;
//					veValue.setSUID(UID);
//					
//					StmtIterator formulas = res.listProperties(E3value.e3_has_formula);
//					
//					while(formulas.hasNext()) {
//						String formula = formulas.next().getString();
//						String formulaName = formula.split("=")[0];
//						String value = Utils.concatTail(formula.split("="));
//
//						veValue.formulas.put(formulaName, value);
//					}
//					
//					Object left = resourceToCell.get(res.getProperty(E3value.ve_has_in_po).getResource());
//					Object right = resourceToCell.get(res.getProperty(E3value.ve_has_out_po).getResource());
//					
//					Object valueExchange = t.getCurrentGraph().insertEdge(t.getCurrentGraph().getDefaultParent(), null, null, left, right);
//					// Have to set value here manually, the value parameter o the insertEdge method doesn't seem to work
//					t.getCurrentGraph().getModel().setValue(valueExchange, veValue);
//					
//					resourceToCell.put(res, valueExchange);
//				}
//			}
//			
//			// This was to enable editing again, so the state can't be altered while
//			// a model is loaded. I'm not sure if it works though
//			// t.graphComponent.setEnabled(true);
//
//			// Layout the graph nicely
//			mxIGraphLayout layout = new mxOrganicLayout(t.getCurrentGraph());
////			mxIGraphLayout layout = new mxHierarchicalLayout(t.graph);
////			mxIGraphLayout layout = new mxOrthogonalLayout(t.graph);
////			mxIGraphLayout layout = new mxPartitionLayout(t.graph);
////			mxIGraphLayout layout = new mxStackLayout(t.graph);
////			mxIGraphLayout layout = new mxCompactTreeLayout(t.graph);
//			
//			layout.execute(t.getCurrentGraph());
//			
//			// TODO: Recursively layout all actors/marketsegments/valueactivities
//        } finally {
//        	t.getCurrentGraph().getModel().endUpdate();
//        }
	}
}
