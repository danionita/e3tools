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

import java.util.HashSet;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

import design.Utils.IDReplacer;
import design.info.Actor;
import design.info.Base;
import design.info.EndSignal;
import design.info.Info.Side;
import design.info.LogicBase;
import design.info.LogicDot;
import design.info.MarketSegment;
import design.info.Note;
import design.info.SignalDot;
import design.info.StartSignal;
import design.info.ValueActivity;
import design.info.ValueInterface;
import design.listeners.ProxySelection;

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
	public final Object note;

	private E3Style style;
	
	public Object clone(Object cell, mxGraph targetGraph) {
		//System.out.println("I'm being called!");

		Object clone = graph.cloneCells(new Object[]{cell}, true)[0];

		new IDReplacer(targetGraph).renewBases(clone);

		return clone;
	}

	public ToolComponent(E3Style style) {
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
			 *  Label text behaviour is defined by Info objects and their toString functions.
			 */
			@Override
			public String convertValueToString(Object obj) {
				mxICell cell = (mxICell) obj;
				
				if (cell.getValue() instanceof Base) {
					return ((Base) cell.getValue()).toString();
				}

				return super.convertValueToString(cell);
			}
		});
		
		this.style = style; 
		
		style.styleGraphComponent(this);

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
		try {
			// Simple blocks
			{
				ValueActivity vaInfo = new ValueActivity(Utils.getUnusedID(graph));
				vaInfo.name = "ValueActivity";
				valueActivity = graph.insertVertex(root, null, vaInfo, 10, 20, 90, 90, "ValueActivity");
				
				Actor acInfo = new Actor(Utils.getUnusedID(graph));
				acInfo.name = "Actor";
				actor = graph.insertVertex(root, null, acInfo, 10, 120, 90, 90, "Actor");
				
				MarketSegment msInfo = new MarketSegment(Utils.getUnusedID(graph));
				msInfo.name = "MarketSegment";
				marketSegment = graph.insertVertex(root, null, msInfo, 10, 220, 90, 90, "MarketSegment");
			}
			
			// Value Interface
			{
				ValueInterface viInfo = new ValueInterface(Utils.getUnusedID(graph));
				viInfo.side = Side.LEFT;
				valueInterface = (mxICell) graph.insertVertex(root, null, viInfo, 80, 320, 20, 50, "ValueInterface");
				E3Graph.addValuePort(graph, (mxICell) valueInterface, true);
				E3Graph.addValuePort(graph, (mxICell) valueInterface, false);
				mxGeometry gm = Utils.geometry(graph, valueInterface);
				graph.insertVertex(valueInterface, null, new SignalDot(Utils.getUnusedID(graph)),
						gm.getWidth() - 2 * E3Style.DOTRADIUS,
						gm.getHeight() / 2 - E3Style.DOTRADIUS,
						E3Style.DOTRADIUS * 2, E3Style.DOTRADIUS * 2,
						"Dot");
			}
			
			// Start signal
			{
				startSignal = graph.insertVertex(root, null, new StartSignal(Utils.getUnusedID(graph)), 70, 380, 25, 25, "StartSignal");
				mxGeometry sgm = graph.getModel().getGeometry(startSignal);
				// Magic number to get the label to float nicely above
				sgm.setOffset(new mxPoint(0, -21));
				
				mxICell dot = (mxICell) graph.insertVertex(startSignal, null, new SignalDot(Utils.getUnusedID(graph)), 0.5, 0.5, 2 * E3Style.DOTRADIUS, 2 * E3Style.DOTRADIUS, "Dot");
				mxGeometry gm = Utils.geometry(graph, dot);
				gm.setRelative(true);
				gm.setOffset(new mxPoint(-E3Style.DOTRADIUS, -E3Style.DOTRADIUS));
				graph.getModel().setGeometry(dot, gm);
			}
			
			// End signal
			{
				endSignal = (mxCell) graph.insertVertex(root, null, new EndSignal(Utils.getUnusedID(graph)), 60, 420, 35, 35, "EndSignal");
				mxGeometry sgm = graph.getModel().getGeometry(endSignal);
				// Magic number to get the label to float nicely above
				sgm.setOffset(new mxPoint(0, -21));

				mxCell dot = (mxCell) graph.insertVertex(endSignal, null, new SignalDot(Utils.getUnusedID(graph)), 0.5, 0.5, 2 * E3Style.DOTRADIUS, 2 * E3Style.DOTRADIUS, "Dot");
				mxGeometry gm = Utils.geometry(graph, dot);
				gm.setRelative(true);
				gm.setOffset(new mxPoint(-E3Style.DOTRADIUS, -E3Style.DOTRADIUS));
				graph.getModel().setGeometry(dot, gm);
			}
			
			// And component
			{
				andGate = graph.insertVertex(root, null, new LogicBase(Utils.getUnusedID(graph)), 70, 475, 30, 50, "LogicBase");
				Object bar = graph.insertVertex(andGate, null, null, 0.5, 0, 1, 50, "Bar");
				mxGeometry barGm = (mxGeometry) graph.getCellGeometry(bar).clone();
				barGm.setRelative(true);
				graph.getModel().setGeometry(bar, barGm);
				
				Object mainDot = graph.insertVertex(andGate, null, new LogicDot(Utils.getUnusedID(graph), true), 0.75, 0.5,
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
				LogicBase lb = new LogicBase(Utils.getUnusedID(graph));
				lb.isOr = true;
				orGate = graph.insertVertex(root, null, lb, 70, 535, 30, 50, "LogicBase");
				Object triangle = graph.insertVertex(orGate, null, null, 0.5, 0, 15, 30, "EastTriangle");
				mxGeometry triangleGm = (mxGeometry) graph.getCellGeometry(triangle).clone();
				triangleGm.setRelative(true);
				graph.getModel().setGeometry(triangle, triangleGm);
				
				Object mainDot = graph.insertVertex(orGate, null, new LogicDot(Utils.getUnusedID(graph), true), 0.75, 0.5, 
						E3Style.DOTRADIUS * 2, E3Style.DOTRADIUS * 2, "Dot");
				mxGeometry dotGm = (mxGeometry) graph.getCellGeometry(mainDot).clone();
				dotGm.setRelative(true);
				dotGm.setOffset(new mxPoint(-E3Style.DOTRADIUS, -E3Style.DOTRADIUS));
				graph.getModel().setGeometry(mainDot, dotGm);
				
				for (int i = 0; i < 3; i++) {
					E3Graph.addLogicDot(graph, (mxCell) orGate);
				}
			}
			
			// Note thing
			{
				Note noteInfo = new Note(Utils.getUnusedID(graph));
				note = graph.insertVertex(root, null, noteInfo, 50, 600, 50, 50, "Note");
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
			graph.insertVertex(root, null, "Note", 120, 600, 100, 50, "NameText");

		} finally {
			graph.getModel().endUpdate();
		}
		
		// This enables clicking on the easttriangle as well (and gate)
		graph.getSelectionModel().addListener(mxEvent.CHANGE, new ProxySelection(graph));
	}
}