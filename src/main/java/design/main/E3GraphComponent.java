package design.main;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.handler.mxGraphHandler;
import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

import design.main.Info.Base;
import design.main.Info.Dot;
import design.main.Info.LogicBase;
import design.main.Info.ValueExchange;
import design.main.Info.ValueInterface;
import design.main.Info.ValuePort;
import design.main.listeners.ProxySelection;
import design.main.properties.E3PropertiesEditor;
import design.main.properties.E3PropertiesEvent;
import design.main.properties.E3PropertiesEventListener;

public class E3GraphComponent extends mxGraphComponent {
	JPopupMenu defaultMenu = new JPopupMenu();
	JPopupMenu logicMenu = new JPopupMenu();
	JPopupMenu partDotMenu = new JPopupMenu();
	JPopupMenu valueInterfaceMenu = new JPopupMenu();
	JPopupMenu valuePortMenu = new JPopupMenu();
	JPopupMenu valueExchangeMenu = new JPopupMenu();
	JPopupMenu actorMenu = new JPopupMenu();

	public E3GraphComponent(mxGraph graph) {
		super(graph);
		
		ContextMenus.addDefaultMenu(defaultMenu, graph);
		ContextMenus.addLogicMenus(logicMenu, graph);
		ContextMenus.addPartDotMenu(partDotMenu, graph);
		ContextMenus.addValueInterfaceMenu(valueInterfaceMenu, graph);
		ContextMenus.addValuePortMenu(valuePortMenu, graph);
		ContextMenus.addValueExchangeMenu(valueExchangeMenu, graph);
		
		ContextMenus.addE3PropertiesMenu(actorMenu, graph);
		ContextMenus.addActorMenu(actorMenu, graph);
		
		// Enable delete key et. al.
		// TODO: Only allow useful keybindings to be added
		new mxKeyboardHandler(this);
		
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
		
		// Set styling of nodes, background color, etc.
		E3Style.styleGraphComponent(this);
		
		graph.addListener(mxEvent.CELLS_ADDED, new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				Object[] cells = ((Object[]) evt.getProperty("cells"));
				mxCell cell = (mxCell) cells[0];
				
				String style = cell.getStyle();

				graph.getModel().beginUpdate();
				try {
					if (style != null) {
						if (style.equals("ValueInterface")) {
							mxICell parent = (mxICell) cell.getParent();
							if (parent == graph.getDefaultParent()) {
								graph.getModel().remove(cell);
							}
							
							graph.constrainChild(cell);
						} else if (style.equals("StartSignal") || style.equals("EndSignal")) {
							Object parent = graph.getModel().getParent(cell);
							if (parent == graph.getDefaultParent()) {
								graph.getModel().remove(cell);
							}
						} else if (style.equals("LogicBase")) {
							Object parent = graph.getModel().getParent(cell);
							if (parent == graph.getDefaultParent()) {
								graph.getModel().remove(cell);
							}
						}
					}
				} finally {
					graph.getModel().endUpdate();
				}
				
				// Maybe it's an edge being added
				mxICell source = (mxICell) evt.getProperty("source");
				mxICell target = (mxICell) evt.getProperty("target");
				
				if (source != null && target != null) {
					String sourceStyle = source.getStyle();
					String targetStyle = target.getStyle();
					
					if (sourceStyle.equals("Dot") && sourceStyle.equals(targetStyle)) {
						graph.getModel().setStyle(cell, "ConnectionElement");
						Object[] sourceEdges = graph.getEdges(source);
						Object[] targetEdges = graph.getEdges(target);
						if (sourceEdges.length + targetEdges.length > 2) {
							graph.getModel().beginUpdate();
							try {
								graph.getModel().remove(cell);
							} finally {
								graph.getModel().endUpdate();
							}
						}
					} else if (sourceStyle.startsWith("ValuePort") && targetStyle.startsWith("ValuePort")) {
						boolean sourceIncoming = ((ValuePort) source.getValue()).incoming;
						boolean targetIncoming = ((ValuePort) target.getValue()).incoming;
						
						// Reverse engineered from the original editor:
						// For two top level actors, one should be incoming and one
						// Should be outgoing. If one of them is nested, anything goes.
						boolean sourceIsTopLevel = Utils.isToplevelValueInterface(graph, source);
						boolean targetIsTopLevel = Utils.isToplevelValueInterface(graph, target);
						
						// One should be an incoming value interface, other one should be outgoing
						// But only if they are both top-level
						graph.getModel().beginUpdate();
						try {
							graph.getModel().setStyle(cell, new String("ValueExchange"));
							graph.getModel().setValue(cell, new ValueExchange());
							mxGeometry gm = Utils.geometry(graph, cell);
							gm.setRelative(true);
							gm.setY(-30);
							graph.getModel().setGeometry(cell, gm);
							
							if (!(sourceIncoming ^ targetIncoming) && (sourceIsTopLevel && targetIsTopLevel)) {
								graph.getModel().remove(cell);
							}
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
				
				String style = graph.getModel().getStyle(cell);
				if (style == null) return;
				
				if (style.equals("LogicBase")) {
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
		
		if (obj == null) {
			menu = defaultMenu;
			Main.contextTarget = new mxPoint(e.getX(), e.getY());
		} else if (style != null) {
			if (style.equals("LogicBase")) menu = logicMenu;
			if (style.equals("ValueInterface")) menu = valueInterfaceMenu;
			if (style.startsWith("ValuePort")) menu = valuePortMenu;
			if (style.startsWith("ValueExchange")) menu = valueExchangeMenu;
			if (style.equals("Actor")) menu = actorMenu;
			if (style.equals("Dot")) {
				Dot valueObj = (Dot) graph.getModel().getValue(obj);
				if (valueObj != null) {
					if (!valueObj.isUnit) {
						menu = partDotMenu;
					}
				}
			}
			if (style.endsWith("Triangle")) {
				Main.contextTarget = graph.getModel().getParent(Main.contextTarget);
				menu = logicMenu;
			}
			if (style.equals("Bar")) {
				Main.contextTarget = graph.getModel().getParent(Main.contextTarget);
				menu = logicMenu;
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
				Object obj = cells[0];
				String style = graph.getModel().getStyle(obj);
				
				if (style != null && (
						style.equals("Actor")
							|| style.equals("ValueActivity")
							|| style.equals("MarketSegment")
						)) {
					return super.shouldRemoveCellFromParent(parent, cells, e);
				}
				
				return false;
			}
		};
	}
}