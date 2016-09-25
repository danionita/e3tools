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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EventObject;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.ScrollPaneConstants;

import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.handler.mxGraphHandler;
import com.mxgraph.swing.util.mxICellOverlay;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxUndoManager;
import com.mxgraph.util.mxUndoableEdit;
import com.mxgraph.util.mxUndoableEdit.mxUndoableChange;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxGraphView;

import design.info.Actor;
import design.info.Base;
import design.info.EndSignal;
import design.info.LogicBase;
import design.info.LogicDot;
import design.info.MarketSegment;
import design.info.StartSignal;
import design.info.ValueActivity;
import design.info.ValueExchange;
import design.info.ValueInterface;
import design.info.ValuePort;
import design.listeners.KeyboardHandler;
import design.listeners.ProxySelection;

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
	
	private boolean popupTriggerEnabled = true;
	
	public E3GraphComponent makeShowcase(E3Graph graph) {
		E3GraphComponent component = new E3GraphComponent(graph);
		
		component.setEnabled(false);
    	component.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    	component.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    	
		return component;
	}

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
		ContextMenus.addStyleMenu(actorMenu, graph);
		
		ContextMenus.addE3PropertiesMenu(valueActivityMenu, graph);
		ContextMenus.addStyleMenu(valueActivityMenu, graph);

		ContextMenus.addE3PropertiesMenu(marketSegmentMenu, graph);
		ContextMenus.addStyleMenu(marketSegmentMenu, graph);
		
		ContextMenus.addE3PropertiesMenu(startSignalMenu, graph);
		ContextMenus.addStartSignalMenu(startSignalMenu, graph);
		
		ContextMenus.addE3PropertiesMenu(endSignalMenu, graph);
		ContextMenus.addEndSignalMenu(endSignalMenu, graph);
		
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
		((E3Graph) graph).style.styleGraphComponent(this, true);
		
		graph.getSelectionModel().addListener(mxEvent.CHANGE, new ProxySelection(graph));		

		getGraphControl().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				Object cell = graph.getSelectionCell();
				mxGeometry gm = graph.getCellGeometry(cell);
				
				if (graph.getModel().getValue(cell) instanceof Base) {
					Base base = (Base) graph.getModel().getValue(cell);
					//System.out.println(base.getSUID());
				}

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
		
		graph.getModel().addListener(mxEvent.CHANGE, new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				validateGraph();
			}
		});
	}
	
	public boolean isPopupTriggerEnabled() {
		return popupTriggerEnabled;
	}
	
	public void setPopupTriggerEnabled(boolean b) {
		popupTriggerEnabled = b;
	}
		
	public void triggerContextMenu(MouseEvent e) {
		// TODO: Use getCells() here to do some kind of fuzzy selection/snapping here?
		Object obj = getCellAtFuzzy(e.getX(), e.getY());
		String style = graph.getModel().getStyle(obj);
		JPopupMenu menu = null;
		
		Main.contextTarget = obj;
		Main.contextPos = new mxPoint(e.getX(), e.getY());
		
		if (obj == null) {
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
			
		if (e.isPopupTrigger() && menu != null && popupTriggerEnabled) {
			if (obj == null) {
				menu.show(e.getComponent(), e.getX(), e.getY());
			} else {
				
				mxPoint menuPos = new mxPoint(e.getX(), e.getY());
				Rectangle bounds = graph.getView().getState(obj).getRectangle();
				
				Base val = (Base) graph.getModel().getValue(Main.contextTarget);
				if (Utils.isDotValue(val)) {
					menuPos.setX(bounds.getCenterX());
					menuPos.setY(bounds.getCenterY());
				}
				
				menu.show(e.getComponent(), (int) menuPos.getX(), (int) menuPos.getY());
			}
		}
	}
	
	/**
	 * Gets a cell in a rectangular radius of 13 pixels
	 * (scaled with the view's getScale()) around x and y.
	 * Prefers the closest LogicDot. If there are none, it just returns the
	 * value of getCellAt.
	 * @param x
	 * @param y
	 * @return
	 */
	private Object getCellAtFuzzy(int x, int y) {
		Object trivialCell = getCellAt(x, y);
		
		int areaSize = (int) (13 * getGraph().getView().getScale());
		Rectangle areaOfInterest = new Rectangle(x, y, 0, 0);
		areaOfInterest.grow(areaSize, areaSize);
		Object[] candidates = getCells(areaOfInterest);
		
		mxPoint mousePos = new mxPoint(x, y);
		
		trivialCell = Arrays.stream(candidates)
					.filter(s -> graph.getModel().getValue(s) instanceof LogicDot)
					.sorted(new Comparator<Object>() {
						@Override
						public int compare(Object left, Object right) {
							mxCellState leftState = graph.getView().getState(left);
							mxPoint leftPos = new mxPoint(leftState.getCenterX(), leftState.getCenterY());
							
							mxCellState rightState = graph.getView().getState(right);
							mxPoint rightPos = new mxPoint(rightState.getCenterX(), rightState.getCenterY());
							
							double leftDist2 = Math.pow(leftPos.getX() - mousePos.getX(), 2) 
									+ Math.pow(leftPos.getY() - mousePos.getY(), 2);

							double rightDist2 = Math.pow(rightPos.getX() - mousePos.getX(), 2) 
									+ Math.pow(rightPos.getY() - mousePos.getY(), 2);
							
							return (int) (leftDist2 - rightDist2);
						}
					})
					.findFirst()
					.orElse(trivialCell);
		
		return trivialCell;
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
	
	public void centerAndScaleView(double viewportWidth, double viewportHeight) {
		mxGraphView view = getGraph().getView();
		
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE,
				maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
		
		for (Object obj : graph.getChildCells(graph.getDefaultParent())) {
			// Only look at the positions from top-level elements
			if (!(graph.getModel().getValue(obj) instanceof ValueActivity
					|| graph.getModel().getValue(obj) instanceof MarketSegment
					|| graph.getModel().getValue(obj) instanceof Actor)) continue;
			
			// Gather the bounds
			mxGeometry gm = graph.getCellGeometry(obj);
			minX = Math.min(minX, gm.getX());
			minY = Math.min(minY, gm.getY());			
			maxX = Math.max(maxX, gm.getX() + gm.getWidth());
			maxY = Math.max(maxY, gm.getY() + gm.getHeight());
		}
		
		double graphWidth = maxX - minX;
		double graphHeight = maxY - minY;
		
		double scale = 1;
		
		// We add 10 to ad a tiny border of white around the graph
		if (graphWidth > graphHeight) {
			scale = viewportWidth / (graphWidth + 10);
		} else {
			scale = viewportHeight / (graphHeight + 10);
		}
			   
		view.scaleAndTranslate(scale, -minX, -minY);
	}
	
	@SuppressWarnings("serial")
	public static class Highlighter extends JComponent implements mxICellOverlay {
		private Object cell;
		private mxGraphComponent graphComponent;

		public Highlighter(Object cell, String tooltip, mxGraphComponent graphComponent) {
			this.graphComponent = graphComponent;
			this.cell = cell;
			
			// TODO: Setting tooltips causes mouse events to be captured, causing
			// valueports to be unclickable.
		}

		public void paint(Graphics g1) {
			Graphics2D g = (Graphics2D) g1;
			
			g.setColor(Color.RED);
			
			g.setStroke(new BasicStroke(2));
			
			mxCellState state = graphComponent.getGraph().getView().getState(cell);
			Rectangle bounds = state.getRectangle();
			bounds.grow(3, 3);
			bounds.width += 1;
			bounds.height += 1;
			setBounds(bounds);
			
			g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
		}

		@Override
		public mxRectangle getBounds(mxCellState state) {
			return state.getBoundingBox();
		}
	}

	/**
	 * Overrides setCellWarning to add a Highlighter overlay (instead of a
	 * blinking yellow triangle). If warning == null or as length zero all highlighter
	 * overlays are removed from the cell.
	 */
	@Override
	public mxICellOverlay setCellWarning(final Object cell, String warning,
			ImageIcon icon, boolean select)
	{
		if (warning != null && warning.length() > 0)
		{
			return addCellOverlay(cell, new Highlighter(cell, warning, this));
		}
		else
		{
			removeCellOverlaysOfClass(cell, Highlighter.class);
		}

		return null;
	}
	
	@SuppressWarnings("serial")
	public static class ValuationOverlay extends JComponent implements mxICellOverlay {
		private Object cell;
		private mxGraphComponent graphComponent;

		public ValuationOverlay(Object cell, mxGraphComponent graphComponent) {
			this.graphComponent = graphComponent;
			this.cell = cell;
		}

		public void paint(Graphics g1) {
			Graphics2D g = (Graphics2D) g1;
			
			Base value = (Base) graphComponent.getGraph().getModel().getValue(cell);
			
			if (value == null || !value.formulas.containsKey("VALUATION")) {
				return;
			}
			
			String valuation = value.formulas.get("VALUATION");
			if (valuation.trim().isEmpty()) {
				valuation = "0";
			}

			mxCellState state = graphComponent.getGraph().getView().getState(cell);
			Rectangle bounds = state.getRectangle();
			bounds.grow(3, 3);
			bounds.width += 1;
			bounds.height += 1;
			
			// Move the rectangle to the top right of the value port
			bounds.x += bounds.width;
			bounds.y -= bounds.height;
			
			int fontsize = bounds.height;
			
			bounds.width = (int) (g.getFontMetrics().stringWidth(valuation) * 1.5);			
			
			int border = (int) (bounds.height * 0.2);
			
			bounds.grow(border, border);

			setBounds(bounds);
			
			// Draw rectangle with border
			g.setColor(Color.GREEN);
			g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
			g.setColor(Color.BLACK);
			g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
			
			// Draw text
			// TODO: Put some sort of reusing facility in place here. We
			// don't want the place crawling with fonts in no time.
			g.setFont(new Font("Serif", Font.PLAIN, fontsize));
			g.drawString(valuation, border, border + fontsize);
		}

		@Override
		public mxRectangle getBounds(mxCellState state) {
			return state.getBoundingBox();
		}
	}

	public void toggleValuationLabels(boolean on) {
		if (on) {
			Utils.getAllCells(getGraph()).stream()
				.filter(s -> E3GraphComponent.this.getGraph().getModel().getValue(s) instanceof ValuePort)
				.forEach(s -> addCellOverlay(s, new ValuationOverlay(s, E3GraphComponent.this)));
		} else {
			Utils.getAllCells(getGraph()).stream()
				.filter(s -> E3GraphComponent.this.getGraph().getModel().getValue(s) instanceof ValuePort)
				.forEach(s -> removeCellOverlays(s));
		}
	}
	
	public void removeCellOverlaysOfClass(Object cell, Class<?> c) {
		mxICellOverlay[] overlays = getCellOverlays(cell);
		
		if (overlays == null)  return;

		Arrays.stream(overlays)
			.filter(c::isInstance)
			.forEach(o -> removeCellOverlay(cell, o));
	}
	
	/**
	 * If the label to be edited belongs to a ValueExchange, only the name is returned,
	 * not the result of the toString() method. This is because otherwise
	 * the value object also becomes part of the name, unless the user removes
	 * it while editing. This is tedious.
	 */
	@Override
	public String getEditingValue(Object cell, EventObject trigger) {
		Object value = graph.getModel().getValue(cell);
		
		if (value instanceof ValueExchange) {
			return ((ValueExchange) value).name;
		}
		
		return super.getEditingValue(cell, trigger);
	}
}