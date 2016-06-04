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

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

import design.main.Info.Actor;
import design.main.Info.Base;
import design.main.Info.EndSignal;
import design.main.Info.LogicBase;
import design.main.Info.LogicDot;
import design.main.Info.MarketSegment;
import design.main.Info.Side;
import design.main.Info.SignalDot;
import design.main.Info.StartSignal;
import design.main.Info.ValueActivity;
import design.main.Info.ValueInterface;
import design.main.listeners.ProxySelection;

public class ToolComponent extends mxGraphComponent {
	public final mxGraph graph = getGraph();
	
	public final Object valueActivity;
	public final Object actor;
	public final Object marketSegment;
	public final Object valueInterface;
	public final Object startSignal;
	public final Object endSignal;
	public final Object orGate;
	public final Object andGate;
	
	public Object clone(Object cell) {
		return graph.cloneCells(new Object[]{cell})[0];
	}

	public ToolComponent() {
		super(new mxGraph() {
			/**
			 * To prevent the labels from being selected
			 * @param cell The cell to check
			 * @return True if the cell can be selected
			 */
			@Override
			public boolean isCellSelectable(Object cell) {
				String style = getModel().getStyle(cell);
				return style != null && !style.equals("NameText");
			}
			
			/**
			 * Valueports should not have a label, so they are silenced (by returning
			 * an empty string). Anything else should just excert default behaviour.
			 */
			@Override
			public String convertValueToString(Object obj) {
				mxICell cell = (mxICell) obj;
				
				// TODO: Special-case this per type
				if (cell.getValue() instanceof StartSignal) {
					return "";
				} else if (cell.getValue() instanceof Info.Base) {
					return ((Info.Base) cell.getValue()).toString();
				}

				return super.convertValueToString(cell);
			}

			/**
			 * Properly clones the Info.Base objects
			 */
			@Override
			public Object[] cloneCells(Object[] cells, boolean allowInvalidEdges) {
				Object[] clones = super.cloneCells(cells, allowInvalidEdges);
				
				for ( Object obj : clones) {
					if (model.getValue(obj) instanceof Info.Base) {
						model.setValue(obj, Utils.base(this, obj));
					}
				}
				
				return clones;
			}
		});
		
		E3Style.styleGraphComponent(this);

		// To make sure cells are immovable and unresizable and such
		graph.setCellsLocked(true);
		// Disallow edges to be created from nodes
		// This makes dragging nice
		setConnectable(false);
		// Make sure only one node is selected
		graph.getSelectionModel().setSingleSelection(true);
		// Disallow dropping of nodes into the toolbox from the main screen
		setImportEnabled(false);

		Object root = graph.getDefaultParent();
		
		graph.getModel().beginUpdate();
		Object ess;
		try {
			// Simple blocks
			{
				ValueActivity vaInfo = new ValueActivity();
				vaInfo.name = "ValueActivity";
				valueActivity = graph.insertVertex(root, null, vaInfo, 10, 20, 90, 90, "ValueActivity");
				
				Actor acInfo = new Actor();
				acInfo.name = "Actor";
				actor = graph.insertVertex(root, null, acInfo, 10, 120, 90, 90, "Actor");
				
				MarketSegment msInfo = new MarketSegment();
				msInfo.name = "MarketSegment";
				marketSegment = graph.insertVertex(root, null, msInfo, 10, 220, 90, 90, "MarketSegment");
			}
			
			// Value Interface
			{
				ValueInterface viInfo = new ValueInterface();
				viInfo.side = Side.LEFT;
				valueInterface = (mxICell) graph.insertVertex(root, null, viInfo, 80, 320, 20, 50, "ValueInterface");
				E3Graph.addValuePort(graph, (mxICell) valueInterface, true);
				E3Graph.addValuePort(graph, (mxICell) valueInterface, false);
				mxGeometry gm = Utils.geometry(graph, valueInterface);
				graph.insertVertex(valueInterface, null, new SignalDot(),
						gm.getWidth() - 2 * E3Style.DOTRADIUS,
						gm.getHeight() / 2 - E3Style.DOTRADIUS,
						E3Style.DOTRADIUS * 2, E3Style.DOTRADIUS * 2,
						"Dot");
			}
			
			// Start signal
			{
				startSignal = graph.insertVertex(root, null, new StartSignal(), 70, 380, 30, 30, "StartSignal");
				mxGeometry sgm = graph.getModel().getGeometry(startSignal);
				// Magic number to get the label to float nicely above
				sgm.setOffset(new mxPoint(0, -21));
				
				mxICell dot = (mxICell) graph.insertVertex(startSignal, null, new SignalDot(), 0.5, 0.5, 2 * E3Style.DOTRADIUS, 2 * E3Style.DOTRADIUS, "Dot");
				mxGeometry gm = Utils.geometry(graph, dot);
				gm.setRelative(true);
				gm.setOffset(new mxPoint(-E3Style.DOTRADIUS, -E3Style.DOTRADIUS));
				graph.getModel().setGeometry(dot, gm);
			}
			
			// End signal
			{
				endSignal = (mxCell) graph.insertVertex(root, null, new EndSignal(), 55, 420, 45, 45, "EndSignal");
				ess = endSignal;
				mxCell dot = (mxCell) graph.insertVertex(endSignal, null, new SignalDot(), 0.5, 0.5, 2 * E3Style.DOTRADIUS, 2 * E3Style.DOTRADIUS, "Dot");
				mxGeometry gm = Utils.geometry(graph, dot);
				gm.setRelative(true);
				gm.setOffset(new mxPoint(-E3Style.DOTRADIUS, -E3Style.DOTRADIUS));
				graph.getModel().setGeometry(dot, gm);
			}
			
			// And component
			{
				andGate = graph.insertVertex(root, null, new LogicBase(), 70, 475, 30, 50, "LogicBase");
				Object bar = graph.insertVertex(andGate, null, null, 0.5, 0, 1, 50, "Bar");
				mxGeometry barGm = (mxGeometry) graph.getCellGeometry(bar).clone();
				barGm.setRelative(true);
				graph.getModel().setGeometry(bar, barGm);
				
				Object mainDot = graph.insertVertex(andGate, null, new LogicDot(true), 0.75, 0.5,
						E3Style.DOTRADIUS * 2, E3Style.DOTRADIUS * 2, "Dot");
				mxGeometry dotGm = (mxGeometry) graph.getCellGeometry(mainDot).clone();
				dotGm.setRelative(true);
				dotGm.setOffset(new mxPoint(-E3Style.DOTRADIUS, -E3Style.DOTRADIUS));
				graph.getModel().setGeometry(mainDot, dotGm);
				
				for (int i = 0; i < 3; i++) {
					E3Graph.addLogicDot(graph, (mxCell) andGate);
				}
			}
			
			// Or component
			{
				LogicBase lb = new LogicBase();
				lb.isOr = true;
				orGate = graph.insertVertex(root, null, lb, 70, 535, 30, 50, "LogicBase");
				Object triangle = graph.insertVertex(orGate, null, null, 0.5, 0, 15, 30, "EastTriangle");
				mxGeometry triangleGm = (mxGeometry) graph.getCellGeometry(triangle).clone();
				triangleGm.setRelative(true);
				graph.getModel().setGeometry(triangle, triangleGm);
				
				Object mainDot = graph.insertVertex(orGate, null, new LogicDot(true), 0.75, 0.5, 
						E3Style.DOTRADIUS * 2, E3Style.DOTRADIUS * 2, "Dot");
				mxGeometry dotGm = (mxGeometry) graph.getCellGeometry(mainDot).clone();
				dotGm.setRelative(true);
				dotGm.setOffset(new mxPoint(-E3Style.DOTRADIUS, -E3Style.DOTRADIUS));
				graph.getModel().setGeometry(mainDot, dotGm);
				
				for (int i = 0; i < 3; i++) {
					E3Graph.addLogicDot(graph, (mxCell) orGate);
				}
			}
			
			// Add some fancy labels
			graph.insertVertex(root, null, "Value Activity", 120, 20, 100, 100, "NameText");
			graph.insertVertex(root, null, "Actor", 120, 120, 100, 100, "NameText");
			graph.insertVertex(root, null, "Market Segment", 120, 220, 100, 100, "NameText");
			graph.insertVertex(root, null, "Value interface", 120, 320, 100, 50, "NameText");
			graph.insertVertex(root, null, "Start signal", 120, 380, 100, 33, "NameText");
			graph.insertVertex(root, null, "End signal", 120, 420, 100, 45, "NameText");
			graph.insertVertex(root, null, "And gate", 120, 475, 100, 50, "NameText");
			graph.insertVertex(root, null, "Or gate", 120, 535, 100, 50, "NameText");

		} finally {
			graph.getModel().endUpdate();
		}
		
		// This enables clicking on the easttriangle as well (and gate)
		graph.getSelectionModel().addListener(mxEvent.CHANGE, new ProxySelection(graph));
	}
}