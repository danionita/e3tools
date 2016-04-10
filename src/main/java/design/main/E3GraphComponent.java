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

public class E3GraphComponent extends mxGraphComponent {
	// Make pop-up menu
	JPopupMenu defaultMenu = new JPopupMenu();
	JPopupMenu logicMenu = new JPopupMenu();
	JPopupMenu partDotMenu = new JPopupMenu();
	JPopupMenu valueInterfaceMenu = new JPopupMenu();
	JPopupMenu valuePortMenu = new JPopupMenu();
	JPopupMenu valueExchangeMenu = new JPopupMenu();
	JPopupMenu actorMenu = new JPopupMenu();
	Object contextTarget = null;
	
	public E3GraphComponent(mxGraph graph) {
		super(graph);
		
		// Construct context menus
		JMenu addMenu = new JMenu("Add");
		addMenu.add(new JMenuItem("ValueActivity"));
		addMenu.add(new JMenuItem("Actor"));
		addMenu.add(new JMenuItem("MarketSegment"));
		defaultMenu.add(addMenu);
		
		logicMenu.add(new JMenuItem(new AbstractAction("Add port") {
			@Override
			public void actionPerformed(ActionEvent e) {
				E3Graph.addDot(graph, (mxCell) contextTarget);
			}
		}));
		logicMenu.add(new JMenuItem(new AbstractAction("Rotate right") {
			@Override
			public void actionPerformed(ActionEvent e) {
				LogicBase lb = (LogicBase) graph.getModel().getValue(contextTarget);
				lb.direction = lb.direction.rotateRight();

				mxGeometry gm = (mxGeometry) graph.getCellGeometry(contextTarget).clone();
				double width = gm.getWidth();
				double height = gm.getHeight();
				gm.setWidth(height);
				gm.setHeight(width);
				graph.getModel().beginUpdate();
				try {
					graph.getModel().setGeometry(contextTarget, gm);
				} finally {
					graph.getModel().endUpdate();
				}

				E3Graph.straightenLogicUnit(graph, (mxCell) contextTarget);
			}
		}));
		logicMenu.add(new JMenuItem(new AbstractAction("Rotate left") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				LogicBase lb = (LogicBase) graph.getModel().getValue(contextTarget);
				lb.direction = lb.direction.rotateLeft();

				mxGeometry gm = (mxGeometry) graph.getCellGeometry(contextTarget).clone();
				double width = gm.getWidth();
				double height = gm.getHeight();
				gm.setWidth(height);
				gm.setHeight(width);
				graph.getModel().beginUpdate();
				try {
					graph.getModel().setGeometry(contextTarget, gm);
				} finally {
					graph.getModel().endUpdate();
				}

				E3Graph.straightenLogicUnit(graph, (mxCell) contextTarget);
			}
		}));
		
		partDotMenu.add(new JMenuItem(new AbstractAction("Remove port") {
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.getModel().beginUpdate();
				try {
					mxCell logicUnit = (mxCell) graph.getModel().getParent(contextTarget);
					graph.getModel().remove(contextTarget);
					E3Graph.straightenLogicUnit(graph, logicUnit);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}));
		
		valueInterfaceMenu.add(new JMenuItem(new AbstractAction("Add incoming port") {
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.getModel().beginUpdate();
				try {
					E3Graph.addValuePort(graph, (mxICell) contextTarget, true);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}));
		
		valueInterfaceMenu.add(new JMenuItem(new AbstractAction("Add outgoing port") {
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.getModel().beginUpdate();
				try {
					E3Graph.addValuePort(graph, (mxICell) contextTarget, false);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}));
		
		valueInterfaceMenu.add(new JMenuItem(new AbstractAction("Edit E3Properties") {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Main.E3PropertiesEditor(Main.mainFrame, (Base) graph.getModel().getValue(contextTarget));
			}
		}));
		
		valuePortMenu.add(new JMenuItem(new AbstractAction("Flip direction") {
			@Override
			public void actionPerformed(ActionEvent e) {
				mxICell vp = (mxICell) contextTarget;
				mxICell vi = (mxICell) vp.getParent();
				ValuePort vpInfo = (ValuePort) vp.getValue();
				ValueInterface viInfo = (ValueInterface) vi.getValue();
	
				vpInfo.incoming ^= true;
				
				graph.getModel().beginUpdate();
				try {
					graph.getModel().setStyle(vp, "ValuePort" + vpInfo.getDirection(viInfo));
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}));
		
		valuePortMenu.addSeparator();
		
		valuePortMenu.add(new JMenuItem(new AbstractAction("Add incoming port") {
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.getModel().beginUpdate();
				try {
					Object parent = graph.getModel().getParent(contextTarget);
					E3Graph.addValuePort(graph, (mxICell) parent, true);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}));
		
		valuePortMenu.add(new JMenuItem(new AbstractAction("Add outgoing port") {
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.getModel().beginUpdate();
				try {
					Object parent = graph.getModel().getParent(contextTarget);
					E3Graph.addValuePort(graph, (mxICell) parent, false);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}));
		
		valuePortMenu.add(new JMenuItem(new AbstractAction("Edit E3Properties") {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Object parent = graph.getModel().getParent(contextTarget);
				new Main.E3PropertiesEditor(Main.mainFrame, (Base) graph.getModel().getValue(parent));
			}
		}));
		
		JMenu attachValueObjectMenu = new JMenu("Attach ValueObject");
		attachValueObjectMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuCanceled(MenuEvent arg0) { }

			@Override
			public void menuDeselected(MenuEvent e) { }

			@Override
			public void menuSelected(MenuEvent e) {
				attachValueObjectMenu.removeAll();
				for (String valueObject : Main.valueObjects) {
					attachValueObjectMenu.add(new JMenuItem(new AbstractAction(valueObject) {
						@Override
						public void actionPerformed(ActionEvent arg0) {
							graph.getModel().beginUpdate();
							try {
								ValueExchange ve = (ValueExchange) (((Base) graph.getModel().getValue(contextTarget)).getCopy());
								ve.valueObject = valueObject;
								graph.getModel().setValue(contextTarget, ve);
							} finally {
								graph.getModel().endUpdate();
							}
						}
					}));
				}
				attachValueObjectMenu.addSeparator();
				attachValueObjectMenu.add(new JMenuItem(new AbstractAction("Add new ValueObject to ValueExchange") {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						String newName = JOptionPane.showInputDialog(
								Main.mainFrame,
								"Enter the name of the new ValueObject",
								"New ValueObject",
								JOptionPane.QUESTION_MESSAGE);
						if (newName == null || newName.trim().length() == 0) return;

						Main.valueObjects.add(newName);
						
						ValueExchange ve = (ValueExchange) (((Base) graph.getModel().getValue(contextTarget)).getCopy());
						ve.valueObject = newName;
						graph.getModel().setValue(contextTarget, ve);
					}
				}));
			}
		});
		valueExchangeMenu.add(attachValueObjectMenu);
		JMenuItem removeValueObjectMenu = new JMenuItem(new AbstractAction("Remove ValueObject") {
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.getModel().beginUpdate();
				try {
					ValueExchange ve = (ValueExchange) (((Base) graph.getModel().getValue(contextTarget)).getCopy());
					ve.valueObject = null;
					graph.getModel().setValue(contextTarget, ve);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		});
		valueExchangeMenu.add(removeValueObjectMenu);
		valueExchangeMenu.add(new JMenuItem(new AbstractAction("Show/hide ValueObject") {
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.getModel().beginUpdate();
				try {
					ValueExchange ve = (ValueExchange) (((Base) graph.getModel().getValue(contextTarget)).getCopy());
					ve.labelHidden ^= true;
					graph.getModel().setValue(contextTarget, ve);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}));
		// This is to make the "Remove ValueObject" button grey out when there's no ValueObject
		valueExchangeMenu.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuCanceled(PopupMenuEvent arg0) { }

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) { }

			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
				ValueExchange ve = (ValueExchange) (((Base) graph.getModel().getValue(contextTarget)).getCopy());
				removeValueObjectMenu.setEnabled(ve.valueObject != null);
			}
		});
		
		actorMenu.add(new JMenuItem(new AbstractAction("Edit E3Properties") {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Main.E3PropertiesEditor(Main.mainFrame, (Base) graph.getModel().getValue(contextTarget)); 
			}
		}));

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
		
		contextTarget = obj;
		
		if (obj == null) {
			menu = defaultMenu;
			contextTarget = new mxPoint(e.getX(), e.getY());
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
				contextTarget = graph.getModel().getParent(contextTarget);
				menu = logicMenu;
			}
			if (style.equals("Bar")) {
				contextTarget = graph.getModel().getParent(contextTarget);
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