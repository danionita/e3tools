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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPopupMenu;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.handler.mxGraphHandler;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxUndoManager;
import com.mxgraph.util.mxUndoableEdit;
import com.mxgraph.util.mxUndoableEdit.mxUndoableChange;
import com.mxgraph.view.mxGraph;

import design.main.Info.Actor;
import design.main.Info.Base;
import design.main.Info.ConnectionElement;
import design.main.Info.EndSignal;
import design.main.Info.LogicBase;
import design.main.Info.LogicDot;
import design.main.Info.MarketSegment;
import design.main.Info.StartSignal;
import design.main.Info.ValueActivity;
import design.main.Info.ValueExchange;
import design.main.Info.ValueExchangeLabel;
import design.main.Info.ValueInterface;
import design.main.Info.ValuePort;
import design.main.listeners.KeyboardHandler;
import design.main.listeners.ProxySelection;

public class E3GraphComponent extends mxGraphComponent {
	JPopupMenu defaultMenu = new JPopupMenu();
	JPopupMenu logicMenu = new JPopupMenu();
	JPopupMenu partDotMenu = new JPopupMenu();
	JPopupMenu unitDotMenu = new JPopupMenu();
	JPopupMenu valueInterfaceMenu = new JPopupMenu();
	JPopupMenu valuePortMenu = new JPopupMenu();
	JPopupMenu valueExchangeMenu = new JPopupMenu();
	JPopupMenu actorMenu = new JPopupMenu();
	JPopupMenu valueActivityMenu = new JPopupMenu();
	JPopupMenu marketSegmentMenu = new JPopupMenu();
	JPopupMenu startSignalMenu = new JPopupMenu();
	JPopupMenu endSignalMenu = new JPopupMenu();

	public mxUndoManager undoManager;

	public E3GraphComponent(mxGraph graph) {
		super(graph);
		
		ContextMenus.addDefaultMenu(defaultMenu, graph);
		ContextMenus.addLogicMenus(logicMenu, graph);
		ContextMenus.addPartDotMenu(partDotMenu, graph);
		ContextMenus.addProportionMenu(unitDotMenu, graph);
		ContextMenus.addValueInterfaceMenu(valueInterfaceMenu, graph);
		
		ContextMenus.addE3PropertiesMenu(valuePortMenu, graph);
		ContextMenus.addValuePortMenu(valuePortMenu, graph);
		
		ContextMenus.addE3PropertiesMenu(valueExchangeMenu, graph);
		ContextMenus.addValueExchangeMenu(valueExchangeMenu, graph);
		
		ContextMenus.addE3PropertiesMenu(actorMenu, graph);
		ContextMenus.addActorMenu(actorMenu, graph);
		
		ContextMenus.addE3PropertiesMenu(valueActivityMenu, graph);
		ContextMenus.addE3PropertiesMenu(marketSegmentMenu, graph);
		
		ContextMenus.addE3PropertiesMenu(startSignalMenu, graph);
		ContextMenus.addStartSignalMenu(startSignalMenu, graph);
		
		ContextMenus.addE3PropertiesMenu(endSignalMenu, graph);
		
		// Some debug menus
		if (Main.DEBUG) {
			ContextMenus.addE3PropertiesMenu(valueInterfaceMenu, graph);
		}
		
		getConnectionHandler().setCreateTarget(false);
		graph.setAllowDanglingEdges(false);
		graph.setPortsEnabled(false);
		graph.setCellsDisconnectable(false);
		getGraphHandler().setRemoveCellsFromParent(true);
		// This makes drag and drop behave properly
		// If you turn these on a drag-shadow that is sometimes offset improperly
		// is shown. Once they fix it in mxGraph we can turn it back on but it's not really needed.
		getGraphHandler().setImagePreview(false);
		getGraphHandler().setCenterPreview(false);
//		getGraphHandler().setCloneEnabled(false);
		graph.getSelectionModel().setSingleSelection(true);
		setConnectable(true);
		
		undoManager = new mxUndoManager();
		
		// This handler propagates all the edits to the undo manager
		mxIEventListener undoHandler = new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				undoManager.undoableEditHappened((mxUndoableEdit) evt
						.getProperty("edit"));
			}
		};
		
		// This handler should keep the selection in sync with the command history
		// (Although I'm not sure if it actually does anything)
		mxIEventListener undoSelectionHandler = new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				List<mxUndoableChange> changes = ((mxUndoableEdit) evt
						.getProperty("edit")).getChanges();
				graph.setSelectionCells(graph
						.getSelectionCellsForChanges(changes));
			}
		};
		
		graph.getModel().addListener(mxEvent.UNDO, undoHandler);
		graph.getView().addListener(mxEvent.UNDO, undoHandler);
		undoManager.addListener(mxEvent.UNDO, undoSelectionHandler);
		undoManager.addListener(mxEvent.REDO, undoSelectionHandler);

		// Enable delete key et. al.
		// Pass the undoManager as well so ctrl+z/ctrl+y can trigger edits
		new KeyboardHandler(this, undoManager);
		
		// Set styling of nodes, background color, etc.
		E3Style.styleGraphComponent(this);
		
		graph.addListener(mxEvent.CELLS_ADDED, new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				Object[] cells = ((Object[]) evt.getProperty("cells"));
				mxCell cell = (mxCell) cells[0];
				
				graph.getModel().beginUpdate();
				try {
					Base value = Utils.base(graph, cell);
					
					if (value != null && !(value instanceof ValueExchangeLabel)) {
						value.name = value.getClass().getSimpleName() + value.getSUID();
						graph.getModel().setValue(cell, value);
					}
					
					if (value instanceof ValueInterface) {
						mxICell parent = (mxICell) cell.getParent();
						if (parent == graph.getDefaultParent()) {
							graph.getModel().remove(cell);
						}
						
						graph.constrainChild(cell);
					} else if (value instanceof StartSignal || value instanceof EndSignal) {
						Object parent = graph.getModel().getParent(cell);
						if (parent == graph.getDefaultParent()) {
							graph.getModel().remove(cell);
						}
					} else if (value instanceof LogicBase) {
						Object parent = graph.getModel().getParent(cell);
						if (parent == graph.getDefaultParent()) {
							graph.getModel().remove(cell);
						}
					}
				} finally {
					graph.getModel().endUpdate();
				}
				
				// Maybe it's an edge being added
				mxICell source = (mxICell) evt.getProperty("source");
				mxICell target = (mxICell) evt.getProperty("target");
				
				if (source != null && target != null) {
					Base sourceValue = Utils.base(graph, source);
					Base targetValue = Utils.base(graph, target);

					if (Utils.isDotValue(sourceValue) && Utils.isDotValue(targetValue)) {
						graph.getModel().setStyle(cell, "ConnectionElement");
						ConnectionElement value = new ConnectionElement();
						value.name = "ConnectionElement" + value.getSUID();
						graph.getModel().setValue(cell, value);

						Object[] sourceEdges = graph.getEdges(source);
						Object[] targetEdges = graph.getEdges(target);
						
						if (sourceEdges.length + targetEdges.length > 2) {
							graph.getModel().beginUpdate();
							try {
								// TODO: If this ever gives problems, change to graph.removeCells
								graph.getModel().remove(cell);
							} finally {
								graph.getModel().endUpdate();
							}
						}
					} else if (sourceValue instanceof ValuePort && targetValue instanceof ValuePort) {
						boolean sourceIncoming = ((ValuePort) sourceValue).incoming;
						boolean targetIncoming = ((ValuePort) targetValue).incoming;
						
						// Reverse engineered from the original editor:
						// For two top level actors, one should be incoming and one
						// Should be outgoing. If one of them is nested, anything goes.
						boolean sourceIsTopLevel = Utils.isToplevelValueInterface(graph, source);
						boolean targetIsTopLevel = Utils.isToplevelValueInterface(graph, target);
						
						// One should be an incoming value interface, other one should be outgoing
						// But only if they are both top-level
						graph.getModel().beginUpdate();
						try {
							// Set ValueExchange edge properties
							graph.getModel().setStyle(cell, new String("ValueExchange"));
							ValueExchange value = new ValueExchange();
							value.name = "ValueExchange" + value.getSUID();
							graph.getModel().setValue(cell, value);
							
							// Add two labels with values properly set
							ValueExchangeLabel valueObjectLabelValue = new ValueExchangeLabel();
							valueObjectLabelValue.isValueObjectLabel = true;
							ValueExchangeLabel nameLabelValue = new ValueExchangeLabel();
							
							mxCell nameLabel = new mxCell(nameLabelValue, new mxGeometry(0, -60, 0, 0), "NameText");
							nameLabel.getGeometry().setRelative(true);
							nameLabel.setVertex(true);
							nameLabel.setVisible(false);
							graph.addCell(nameLabel, cell);

							mxCell valueObjectLabel = new mxCell(valueObjectLabelValue, new mxGeometry(0, 30, 0, 0), "NameText");
							valueObjectLabel.getGeometry().setRelative(true);
							valueObjectLabel.setVertex(true);
							valueObjectLabel.setVisible(false);
							graph.addCell(valueObjectLabel, cell);
							
							if (!(sourceIncoming ^ targetIncoming) && (sourceIsTopLevel && targetIsTopLevel)) {
								graph.getModel().remove(cell);
							}
							
							Utils.updateValueExchangeNameLabel(graph, cell);
						} finally {
							graph.getModel().endUpdate();
						}
					} else {
						graph.getModel().beginUpdate();
						try {
							graph.getModel().remove(cell);
						} finally {
							graph.getModel().endUpdate();
						}
					}
				}
			}
		});
		
		graph.addListener(mxEvent.RESIZE_CELLS, new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				Object[] cells = ((Object[]) evt.getProperty("cells"));
				mxCell cell = (mxCell) cells[0];
				
				if (Utils.base(graph, cell) instanceof LogicBase) {
					E3Graph.straightenLogicUnit(graph, cell);
				} else {
					graph.getModel().beginUpdate();
					try {
						for (int i = 0; i < cell.getChildCount(); i++) {
							// Straighten ports & constrain cell if needed
							graph.constrainChild(cell.getChildAt(i));
						}
					} finally {
						graph.getModel().endUpdate();
					}
				}
			}
		});
		
		graph.addListener(mxEvent.MOVE_CELLS, new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				Object[] cells = ((Object[]) evt.getProperty("cells"));
				mxCell cell = (mxCell) cells[0];

				graph.getModel().beginUpdate();
				try {
					// Straighten ports & constrain cell if needed
					graph.constrainChild(cell);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		});
		
		graph.getSelectionModel().addListener(mxEvent.CHANGE, new ProxySelection(graph));		

		getGraphControl().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					triggerContextMenu(e);
				}
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					triggerContextMenu(e);
				}
			}
		});
	}
		
	public void triggerContextMenu(MouseEvent e) {
		Object obj = getCellAt(e.getX(), e.getY());
		String style = graph.getModel().getStyle(obj);
		JPopupMenu menu = null;
		
		Main.contextTarget = obj;
		Main.contextPos = new mxPoint(e.getX(), e.getY());
		
		if (obj == null) {
			obj = graph.getDefaultParent();
			menu = defaultMenu;
			Main.contextTarget = new mxPoint(e.getX(), e.getY());
		} else if (style != null) {
			if (style.endsWith("Triangle")) {
				Main.contextTarget = graph.getModel().getParent(Main.contextTarget);
			}
			if (style.equals("Bar")) {
				Main.contextTarget = graph.getModel().getParent(Main.contextTarget);
			}
			
			Object val = graph.getModel().getValue(Main.contextTarget);
			// If it does not have a base object as value
			// there's probably no use in checking for a right click menu
			if (!(val instanceof Base)) return;
			Base value = (Base) val;
			
			if (value instanceof LogicBase) menu = logicMenu;
			if (value instanceof ValueInterface) menu = valueInterfaceMenu;
			if (value instanceof ValuePort) menu = valuePortMenu;
			if (value instanceof ValueExchange) menu = valueExchangeMenu;
			if (value instanceof Actor) menu = actorMenu;
			if (value instanceof MarketSegment) menu = marketSegmentMenu;
			if (value instanceof ValueActivity) menu = valueActivityMenu;
			if (value instanceof StartSignal) menu = startSignalMenu;
			if (value instanceof EndSignal) menu = endSignalMenu;
			if (value instanceof LogicDot) {
				if (((LogicDot) value).isUnit) menu = unitDotMenu;
				else menu = partDotMenu;
			}
		}
			
		if (e.isPopupTrigger() && menu != null) {
			menu.show(e.getComponent(), e.getX(), e.getY());
		}
	}
	
	/**
	 * Makes sure that only actors can be moved out of parents.
	 */
	@Override
	protected mxGraphHandler createGraphHandler() {
		return new mxGraphHandler(this) {
			@Override
			protected boolean shouldRemoveCellFromParent(Object parent, Object[] cells, MouseEvent e) {
				Base value = Utils.base(graph, cells[0]);
				
				if (value instanceof Actor
						|| value instanceof ValueActivity
						|| value instanceof MarketSegment) {
					return super.shouldRemoveCellFromParent(parent, cells, e);
				}
				
				return false;
			}
		};
	}
}