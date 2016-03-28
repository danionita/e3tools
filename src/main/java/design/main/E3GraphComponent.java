package design.main;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

import design.main.Info.Dot;
import design.main.Info.LogicBase;
import design.main.Info.Side;
import design.main.Info.ValueInterface;
import design.main.Info.ValuePort;

public class E3GraphComponent extends mxGraphComponent {
	JMenuBar menuBar;
	JPanel toolbarPane;

	// Make pop-up menu
	JPopupMenu defaultMenu = new JPopupMenu();
	JPopupMenu logicMenu = new JPopupMenu();
	JPopupMenu partDotMenu = new JPopupMenu();
	JPopupMenu valueInterfaceMenu = new JPopupMenu();
	JPopupMenu valuePortMenu = new JPopupMenu();
	Object contextTarget = null;
	
	public E3GraphComponent(mxGraph graph, JMenuBar menuBar_, JPanel toolbarPane_) {
		super(graph);
		
		menuBar = menuBar_;
		toolbarPane = toolbarPane_;
		
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
		
		valueInterfaceMenu.add(new JMenuItem("Add port"));
		
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

		// Enable delete key et. al.
		// TODO: Only allow useful keybindings to be added
		new mxKeyboardHandler(this);
		
		getConnectionHandler().setCreateTarget(false);
		graph.setAllowDanglingEdges(false);
		graph.setPortsEnabled(false);
		getGraphHandler().setRemoveCellsFromParent(true);
		// This makes drag and drop behave properly
		// If you turn these on a drag-shadow that is sometimes offset improperly
		// is shown. Once they fix it in mxGraph we can turn it back on but it's not really needed.
		getGraphHandler().setImagePreview(false);
		getGraphHandler().setCenterPreview(false);
//		getGraphHandler().setCloneEnabled(false);
		graph.getSelectionModel().setSingleSelection(true);
		
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
								cell.removeFromParent();
							}
							
							graph.constrainChild(cell);
						} else if (style.equals("StartSignal") || style.equals("EndSignal")) {
							Object parent = graph.getModel().getParent(cell);
							if (parent == graph.getDefaultParent()) {
								cell.removeFromParent();
							}
						} else if (style.equals("LogicBase")) {
							Object parent = graph.getModel().getParent(cell);
							if (parent == graph.getDefaultParent()) {
								cell.removeFromParent();
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
					
					System.out.println(sourceStyle + " -> " + targetStyle);
					
					if (sourceStyle.equals("Dot") && sourceStyle.equals(targetStyle)) {
						graph.getModel().setStyle(cell, "ConnectionElement");
					} else if (sourceStyle.startsWith("ValuePort") && targetStyle.startsWith("ValuePort")) {
						graph.getModel().setStyle(cell, "ValueExchange");
						
						System.out.println("Connecting " + sourceStyle + " --> " + targetStyle);

						// TODO: Make this NOT throw an exceptino when connecting dot to value port
						boolean sourceIncoming = ((ValuePort) source.getValue()).incoming;
						boolean targetIncoming = ((ValuePort) target.getValue()).incoming;
						
						System.out.println(sourceIncoming);
						System.out.println(targetIncoming);
						System.out.println(sourceIncoming ^ targetIncoming);
						
						// Reverse engineered from the original editor:
						// For two top level actors, one should be incoming and one
						// Should be outgoing. If one of them is nested, anything goes.
						boolean sourceIsTopLevel = Utils.isToplevelValueInterface(graph, source);
						boolean targetIsTopLevel = Utils.isToplevelValueInterface(graph, target);
						
						// One should be an incoming value interface, other one should be outgoing
						// But only if they are both top-level
						graph.getModel().beginUpdate();
						try {
							if (!(sourceIncoming ^ targetIncoming) && (sourceIsTopLevel && targetIsTopLevel)) {
								cell.removeFromParent();
							}
						} finally {
							graph.getModel().endUpdate();
						}
					} else {
						graph.getModel().beginUpdate();
						try {
							cell.removeFromParent();
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
				System.out.println("Moved: " + cell.getStyle());

				graph.getModel().beginUpdate();
				try {
					// Straighten ports & constrain cell if needed
					graph.constrainChild(cell);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		});
		
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
		
		// If a context sensitive menu is needed:
//		graph.getSelectionModel().addListener(mxEvent.CHANGE, new mxIEventListener() {
//			
//			@Override
//			public void invoke(Object sender, mxEventObject evt) {
//				if (!(sender instanceof mxGraphSelectionModel)) return;
//				mxGraphSelectionModel selectionModel = (mxGraphSelectionModel) sender;
//				
//				Object[] objs = selectionModel.getCells();
//				
//				if (objs.length == 0) {
//					toolbarPane.setVisible(false);
//					return;
//				}
//				toolbarPane.setVisible(true);
//				Object obj = objs[0];
//				
//				if (!(obj instanceof mxICell)) return;
//				mxICell cell = (mxICell) obj;
//				
//				mxGeometry gm = cell.getGeometry();
//				Rectangle bounds = getBounds();
//				int padding = 20;
//				mxPoint pos = toScreen(new mxPoint(gm.getX() + gm.getWidth() + 20, gm.getY()));
//				toolbarPane.setBounds((int) pos.getX(), (int) pos.getY(), 30, 70);
//			}
//		});
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
			if (style.equals("Dot")) {
				Dot valueObj = (Dot) graph.getModel().getValue(obj);
				if (valueObj != null) {
					if (!valueObj.isUnit) {
						menu = partDotMenu;
					}
				}
			}
		}
		
		if (e.isPopupTrigger() && menu != null) {
			menu.show(e.getComponent(), e.getX(), e.getY());
		}
	}
	
	public mxPoint toScreen(mxPoint point) {
		mxPoint result = new mxPoint(point);
		Rectangle bounds = getBounds();
		Rectangle menuBounds = menuBar.getBounds();
		result.setX(result.getX() + bounds.getX());
		result.setY(result.getY() + menuBounds.getHeight() + bounds.getY());
		
		return result;
	}
}