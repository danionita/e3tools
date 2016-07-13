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

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;

import com.mxgraph.model.mxGraphModel;

import design.main.Info.ValuePort;

public class ExampleModels {
	public static class SmallTricky extends AbstractAction {
		private Main main;

		public SmallTricky(Main main) {
			super("Small tricky model");
			
			this.main = main;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			E3Graph graph = main.getCurrentGraph();
			mxGraphModel model = (mxGraphModel) graph.getModel();
			
			ToolComponent tc = Main.globalTools;
			
			Object tlBottom, blTop;
			
			model.beginUpdate();
			try {
				System.out.println("Adding small tricky graph");
				Object tl = graph.addActor(100, 100);
				Object bl = graph.addActor(100, 250);
				
				tlBottom = graph.addValueInterface(tl, 30, 50);
				blTop = graph.addValueInterface(bl, 30, 0);
				
				graph.connectVE(tlBottom, blTop);
				graph.connectVE(blTop, tlBottom);
				
				Object ss = graph.addStartSignal(bl, 20, 20);
				Object es = graph.addEndSignal(tl, 20, 20);
				
				graph.connectCE(ss, blTop);
				graph.connectCE(tlBottom, es);

//				System.out.println("Pre-commit: ");
//				Base info = (Base) ((mxCell) model.getChildAt(tlBottom, 0)).getValue();
//				System.out.println("SUID of ValuePort " + 0 + ": " + info.getSUID());
//				info = (Base) ((mxCell) model.getChildAt(tlBottom, 1)).getValue();
//				System.out.println("SUID of ValuePort " + 1 + ": " + info.getSUID());
//				info = (Base) ((mxCell) model.getChildAt(blTop, 0)).getValue();
//				System.out.println("SUID of ValuePort " + 0 + ": " + info.getSUID());
//				info = (Base) ((mxCell) model.getChildAt(blTop, 1)).getValue();
//				System.out.println("SUID of ValuePort " + 1 + ": " + info.getSUID());
			} finally {
				model.endUpdate();
			}
		}
	}

	public static class MediumTricky extends AbstractAction {
		private Main main;

		public MediumTricky(Main main) {
			super("Medium tricky model");
			this.main = main;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			E3Graph graph = main.getCurrentGraph();
			mxGraphModel model = (mxGraphModel) graph.getModel();
			
			ToolComponent tc = Main.globalTools;
			
			Object root = graph.getDefaultParent();
			
			model.beginUpdate();
			try {
				Object tl = graph.addActor(100, 100);
				Object bl = graph.addActor(100, 250);
				Object tr = graph.addActor(250, 100);
				
				Object tlBottom = graph.addValueInterface(tl, 30, 50);
				Object blTop = graph.addValueInterface(bl, 30, 0);
				Object tlRight = graph.addValueInterface(tl, 50, 30);
				Object trLeft = graph.addValueInterface(tr, 0, 30);
				
				graph.connectVE(tlBottom, blTop);
				graph.connectVE(blTop, tlBottom);
				graph.connectVE(tlRight, trLeft);
				graph.connectVE(trLeft, tlRight);
				
				Object ss = graph.addStartSignal(bl, 20, 20);
				Object es =  graph.addEndSignal(tr, 20, 20);
				
				graph.connectCE(ss, blTop);
				graph.connectCE(tlBottom, tlRight);
				graph.connectCE(trLeft, es);
			} finally {
				model.endUpdate();
			}	
		}
	}

	public static class FlatRateTelephony extends AbstractAction {
		private Main main;

		public FlatRateTelephony(Main main) {
			super("Flat rate telephony");
			this.main = main;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			E3Graph graph = main.getCurrentGraph();
			mxGraphModel model = (mxGraphModel) graph.getModel();
			
			ToolComponent tc = Main.globalTools;
			
			Object root = graph.getDefaultParent();
			
			model.beginUpdate();
			try {
				Object userA = graph.addActor(50, 300);
				Object userB = graph.addActor(400, 300);
				Object providerA = graph.addActor(50, 50);
				Object providerB = graph.addActor(400, 50);
				
				graph.setCellSize(userA, 200, 100);
				graph.setCellSize(userB, 100, 100);
				graph.setCellSize(providerA, 200, 100);
				graph.setCellSize(providerB, 100, 100);
				
				graph.setName(userA, "User A");
				graph.setName(userB, "User B");
				graph.setName(providerA, "Provider A");
				graph.setName(providerB, "Provider B");
				
				Object userALeftVI = graph.addValueInterface(userA, 40, 0);
				Object userARightVI = graph.addValueInterface(userA, 140, 0);
				
				Object providerALeftVI = graph.addValueInterface(providerA, 40, 50);
				Object providerARightVI = graph.addValueInterface(providerA, 140, 50);
				Object providerAWallVI = graph.addValueInterface(providerA, 200, 30);
				
				Object providerBLeftVI = graph.addValueInterface(providerB, 0, 30);
				Object providerBRightVI = graph.addValueInterface(providerB, 40, 50);
				
				Object userBVI = graph.addValueInterface(userB, 40, 0);
				
				Object userALeftSS = graph.addStartSignal(userA, 40, 30);
				Object userARightSS = graph.addStartSignal(userA, 140, 30);
				
				Object providerAES = graph.addEndSignal(providerA, 40, 30);
				
				Object userBES = graph.addEndSignal(userB, 40, 20);
				
				Object ve = graph.connectVE(userALeftVI, providerALeftVI);
				graph.setValueObject(ve, "MONEY");
				graph.setValueObjectLabelPosition(ve, 0, -50);
				graph.setFormulaOnEdgeAndPorts(ve, "VALUATION", "37.5");

				ve = graph.connectVE(providerALeftVI, userALeftVI);
				graph.setValueObject(ve, "SERVICE");
				graph.setValueObjectLabelPosition(ve, 0, -10);
				
				ve = graph.connectVE(userARightVI, providerARightVI);
				ve = graph.connectVE(providerARightVI, userARightVI);
				graph.setValueObject(ve, "SERVICE");
				graph.setValueObjectLabelPosition(ve, 0, -10);
				
				ve = graph.connectVE(providerAWallVI, providerBLeftVI);
				graph.setValueObject(ve, "MONEY");
				graph.setValueObjectLabelPosition(ve, 0.3, -10);
				graph.setFormulaOnEdgeAndPorts(ve, "VALUATION", "0.07");
				ve = graph.connectVE(providerBLeftVI, providerAWallVI);
				graph.setValueObject(ve, "SERVICE");
				graph.setValueObjectLabelPosition(ve, -0.3, -20);
				
				ve = graph.connectVE(providerBRightVI, userBVI);
				graph.setValueObject(ve, "SERVICE");
				graph.setValueObjectLabelPosition(ve, 0, -10);
				ve = graph.connectVE(userBVI, providerBRightVI);
				
				graph.connectCE(userALeftSS, userALeftVI);
				graph.connectCE(userARightSS, userARightVI);

				graph.connectCE(providerALeftVI, providerAES);
				graph.connectCE(providerARightVI, providerAWallVI);
				
				graph.connectCE(providerBLeftVI, providerBRightVI);

				graph.connectCE(userBVI, userBES);
				
			} finally {
				model.endUpdate();
			}	
		}
	}
 
	public static class SingleTransaction extends AbstractAction {
		private Main main;

		public SingleTransaction(Main main) {
			super("Single transaction");
			this.main = main;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			E3Graph graph = main.getCurrentGraph();
			mxGraphModel model = (mxGraphModel) graph.getModel();
			
			ToolComponent tc = Main.globalTools;
			
			Object root = graph.getDefaultParent();
			
			model.beginUpdate();
			try {
				Object l = graph.addActor(50, 50);
				Object r = graph.addActor(350, 50);
				
				graph.setName(l, "Producer");
				graph.setName(r, "Consumer");
				
				Object lvi = graph.addValueInterface(l, 50, 30);
				Object rvi = graph.addValueInterface(r, 0, 30);
				
				Object paymentVE = graph.connectVE(rvi, lvi);
				Object productVE = graph.connectVE(lvi, rvi);
				
				graph.setFormula(paymentVE, "VALUATION", "10");
				graph.setFormula(productVE, "VALUATION", "6");
				
				// TODO: This should be replaced by automatic propagation of valuations
				List<Object> valuePorts = Utils.getChildrenWithValue(graph, lvi, ValuePort.class);
				for (Object vp : valuePorts) {
					ValuePort vpInfo = (ValuePort) Utils.base(graph, vp);
					if (vpInfo.incoming) {
						graph.setFormula(vp, "VALUATION", "10");
					} else {
						graph.setFormula(vp, "VALUATION", "6");
					}
				}

				valuePorts = Utils.getChildrenWithValue(graph, rvi, ValuePort.class);
				for (Object vp : valuePorts) {
					ValuePort vpInfo = (ValuePort) Utils.base(graph, vp);
					if (vpInfo.incoming) {
						graph.setFormula(vp, "VALUATION", "6");
					} else {
						graph.setFormula(vp, "VALUATION", "10");
					}
				}
				
				graph.setValueObject(paymentVE, "MONEY");
				graph.setValueObject(productVE, "SERVICE");
				
				graph.setValueObjectLabelPosition(paymentVE, 0, -20);
				graph.setValueObjectLabelPosition(productVE, 0, -15);
				
				Object ss = graph.addStartSignal(r, 20, 20);
				Object es =  graph.addEndSignal(l, 20, 20);
				
				graph.connectCE(ss, rvi);
				graph.connectCE(es, lvi);
			} finally {
				model.endUpdate();
			}	
		}
	}
	
	
}
