package design.main;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
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
import com.mxgraph.view.mxGraph;

import design.main.Info.Base;
import design.main.Info.LogicBase;
import design.main.Info.ValueExchange;
import design.main.Info.ValueInterface;
import design.main.Info.ValuePort;
import design.main.properties.E3PropertiesEditor;
import design.main.properties.E3PropertiesEvent;
import design.main.properties.E3PropertiesEventListener;

public class ContextMenus {
	public static void addDefaultMenu(JPopupMenu menu, mxGraph graph) {
		// Construct context menus
		JMenu addMenu = new JMenu("Add");
		addMenu.add(new JMenuItem("ValueActivity"));
		addMenu.add(new JMenuItem("Actor"));
		addMenu.add(new JMenuItem("MarketSegment"));
		menu.add(addMenu);
	}
	
	public static void addE3PropertiesMenu(JPopupMenu menu, mxGraph graph) {
		menu.add(new JMenuItem(new AbstractAction("Edit E3Properties") {
			@Override
			public void actionPerformed(ActionEvent e) {
				E3PropertiesEditor editor = new E3PropertiesEditor(Main.mainFrame, (Base) graph.getModel().getValue(Main.contextTarget));
				editor.addE3PropertiesListener(new E3PropertiesEventListener() {
					@Override
					public void invoke(E3PropertiesEvent event) {
						graph.getModel().beginUpdate();
						try {
							graph.getModel().setValue(Main.contextTarget, event.resultObject);
						} finally {
							graph.getModel().endUpdate();
						}
					}
				});
				editor.show();
			}
		}));
	}
	
	public static void addLogicMenus(JPopupMenu menu, mxGraph graph) {
		menu.add(new JMenuItem(new AbstractAction("Add port") {
			@Override
			public void actionPerformed(ActionEvent e) {
				Base value = (Base) graph.getModel().getValue(Main.contextTarget);
				
				if (value instanceof Info.Dot) {
					Main.contextTarget = graph.getModel().getParent(Main.contextTarget);
				}
				
				E3Graph.addDot(graph, (mxCell) Main.contextTarget);
			}
		}));

		menu.add(new JMenuItem(new AbstractAction("Rotate right") {
			@Override
			public void actionPerformed(ActionEvent e) {
				Base value = (Base) graph.getModel().getValue(Main.contextTarget);
				
				if (value instanceof Info.Dot) {
					Main.contextTarget = graph.getModel().getParent(Main.contextTarget);
				}

				LogicBase lb = (LogicBase) graph.getModel().getValue(Main.contextTarget);
				lb.direction = lb.direction.rotateRight();

				mxGeometry gm = (mxGeometry) graph.getCellGeometry(Main.contextTarget).clone();
				double width = gm.getWidth();
				double height = gm.getHeight();
				gm.setWidth(height);
				gm.setHeight(width);
				graph.getModel().beginUpdate();
				try {
					graph.getModel().setGeometry(Main.contextTarget, gm);
				} finally {
					graph.getModel().endUpdate();
				}

				E3Graph.straightenLogicUnit(graph, (mxCell) Main.contextTarget);
			}
		}));

		menu.add(new JMenuItem(new AbstractAction("Rotate left") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Base value = (Base) graph.getModel().getValue(Main.contextTarget);
				
				if (value instanceof Info.Dot) {
					Main.contextTarget = graph.getModel().getParent(Main.contextTarget);
				}

				LogicBase lb = (LogicBase) graph.getModel().getValue(Main.contextTarget);
				lb.direction = lb.direction.rotateLeft();

				mxGeometry gm = (mxGeometry) graph.getCellGeometry(Main.contextTarget).clone();
				double width = gm.getWidth();
				double height = gm.getHeight();
				gm.setWidth(height);
				gm.setHeight(width);
				graph.getModel().beginUpdate();
				try {
					graph.getModel().setGeometry(Main.contextTarget, gm);
				} finally {
					graph.getModel().endUpdate();
				}

				E3Graph.straightenLogicUnit(graph, (mxCell) Main.contextTarget);
			}
		}));
	}
	
	public static void addPartDotMenu(JPopupMenu menu, mxGraph graph) {
		menu.add(new JMenuItem(new AbstractAction("Remove port") {
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.getModel().beginUpdate();
				try {
					mxCell logicUnit = (mxCell) graph.getModel().getParent(Main.contextTarget);
					graph.getModel().remove(Main.contextTarget);
					E3Graph.straightenLogicUnit(graph, logicUnit);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}));
		
		menu.addSeparator();
		
		addLogicMenus(menu, graph);
	}
	
	public static void addValueInterfaceMenu(JPopupMenu menu, mxGraph graph) {
		menu.add(new JMenuItem(new AbstractAction("Add incoming port") {
			@Override
			public void actionPerformed(ActionEvent e) {
				Base value = (Base) graph.getModel().getValue(Main.contextTarget);
				
				if (value instanceof ValuePort) {
					Main.contextTarget = graph.getModel().getParent(Main.contextTarget);
				}
				
				graph.getModel().beginUpdate();
				try {
					E3Graph.addValuePort(graph, (mxICell) Main.contextTarget, true);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}));
		
		menu.add(new JMenuItem(new AbstractAction("Add outgoing port") {
			@Override
			public void actionPerformed(ActionEvent e) {
				Base value = (Base) graph.getModel().getValue(Main.contextTarget);
				
				if (value instanceof ValuePort) {
					Main.contextTarget = graph.getModel().getParent(Main.contextTarget);
				}
				
				graph.getModel().beginUpdate();
				try {
					E3Graph.addValuePort(graph, (mxICell) Main.contextTarget, false);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}));
	}
	
	public static void addValuePortMenu(JPopupMenu menu, mxGraph graph) {
		menu.add(new JMenuItem(new AbstractAction("Flip direction") {
			@Override
			public void actionPerformed(ActionEvent e) {
				mxICell vp = (mxICell) Main.contextTarget;
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
		
		menu.addSeparator();
		
		addValueInterfaceMenu(menu, graph);
	}
	
	public static void addValueExchangeMenu(JPopupMenu menu, mxGraph graph) {
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
								ValueExchange ve = (ValueExchange) (((Base) graph.getModel().getValue(Main.contextTarget)).getCopy());
								ve.valueObject = valueObject;
								graph.getModel().setValue(Main.contextTarget, ve);
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
						
						ValueExchange ve = (ValueExchange) (((Base) graph.getModel().getValue(Main.contextTarget)).getCopy());
						ve.valueObject = newName;
						graph.getModel().setValue(Main.contextTarget, ve);
					}
				}));
			}
		});
		menu.add(attachValueObjectMenu);

		JMenuItem removeValueObjectMenu = new JMenuItem(new AbstractAction("Remove ValueObject") {
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.getModel().beginUpdate();
				try {
					ValueExchange ve = (ValueExchange) (((Base) graph.getModel().getValue(Main.contextTarget)).getCopy());
					ve.valueObject = null;
					graph.getModel().setValue(Main.contextTarget, ve);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		});
		menu.add(removeValueObjectMenu);

		menu.add(new JMenuItem(new AbstractAction("Show/hide ValueObject") {
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.getModel().beginUpdate();
				try {
					ValueExchange ve = (ValueExchange) (((Base) graph.getModel().getValue(Main.contextTarget)).getCopy());
					ve.labelHidden ^= true;
					graph.getModel().setValue(Main.contextTarget, ve);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}));

		// This is to make the "Remove ValueObject" button grey out when there's no ValueObject
		menu.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuCanceled(PopupMenuEvent arg0) { }

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) { }

			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
				ValueExchange ve = (ValueExchange) (((Base) graph.getModel().getValue(Main.contextTarget)).getCopy());
				removeValueObjectMenu.setEnabled(ve.valueObject != null);
			}
		});
	}
	
	public static void addActorMenu(JPopupMenu menu, mxGraph graph) {

	}
}
