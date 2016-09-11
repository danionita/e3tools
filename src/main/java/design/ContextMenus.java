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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;

import design.info.Actor;
import design.info.Base;
import design.info.EndSignal;
import design.info.LogicDot;
import design.info.MarketSegment;
import design.info.StartSignal;
import design.info.ValueExchange;
import design.info.ValuePort;
import design.properties.E3PropertiesEditor;
import design.properties.E3PropertiesEvent;
import design.properties.E3PropertiesEventListener;
import design.style.E3StyleEditor;
import design.style.E3StyleEvent;
import design.style.E3StyleEventListener;

public class ContextMenus {
	public static void addDefaultMenu(JPopupMenu menu, mxGraph graph) {
		// Construct context menus
		JMenu addMenu = new JMenu("Add");
		addMenu.add(new JMenuItem(new AbstractAction("ValueActivity") {
			@Override
			public void actionPerformed(ActionEvent e) {
				mxCell va = (mxCell) Main.globalTools.clone(Main.globalTools.valueActivity, Utils.getMaxID(graph));
				// getGeometry is allowed here because at this point the cell is not a member of any graph in particular.
				va.getGeometry().setX(Main.contextPos.getX());
				va.getGeometry().setY(Main.contextPos.getY());
				graph.addCell(va);
			}
		}));
		addMenu.add(new JMenuItem(new AbstractAction("Actor") {
			@Override
			public void actionPerformed(ActionEvent e) {
				mxCell va = (mxCell) Main.globalTools.clone(Main.globalTools.actor, Utils.getMaxID(graph));
				va.getGeometry().setX(Main.contextPos.getX());
				va.getGeometry().setY(Main.contextPos.getY());
				graph.addCell(va);
			}
		}));
		addMenu.add(new JMenuItem(new AbstractAction("MarketSegment") {
			@Override
			public void actionPerformed(ActionEvent e) {
				mxCell va = (mxCell) Main.globalTools.clone(Main.globalTools.marketSegment, Utils.getMaxID(graph));
				va.getGeometry().setX(Main.contextPos.getX());
				va.getGeometry().setY(Main.contextPos.getY());
				graph.addCell(va);
			}
		}));
		menu.add(addMenu);
	}
	
	public static void addE3PropertiesMenu(JPopupMenu menu, mxGraph graph) {
		menu.add(new JMenuItem(new AbstractAction("Edit E3Properties") {
			@Override
			public void actionPerformed(ActionEvent e) {
				E3PropertiesEditor editor = new E3PropertiesEditor(Main.mainFrame, Utils.base(graph, Main.contextTarget));
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
				Base value = Utils.base(graph, Main.contextTarget);
				
				if (value instanceof LogicDot) {
					Main.contextTarget = graph.getModel().getParent(Main.contextTarget);
				}
				
				E3Graph.addLogicDot(graph, (mxCell) Main.contextTarget);
			}
		}));
		
		@SuppressWarnings("serial")
		class SetPortsAction extends AbstractAction {
			final int n;
			
			public SetPortsAction(int n_) {
				super(n_ + "");
				n = n_;
			}
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Base value = Utils.base(graph, Main.contextTarget);
				
				if (value instanceof LogicDot) {
					Main.contextTarget = graph.getModel().getParent(Main.contextTarget);
				}
				
				List<Object> dots = new ArrayList<>();
				for (int j = 0; j < graph.getModel().getChildCount(Main.contextTarget); j++) {
					Base childValue = Utils.base(graph, graph.getModel().getChildAt(Main.contextTarget, j));
					if (childValue instanceof LogicDot) {
						if (!((LogicDot) childValue).isUnit) {
							dots.add(graph.getModel().getChildAt(Main.contextTarget, j));
						}
					}
				}
				
				if (n < dots.size()) {
					graph.getModel().beginUpdate();
					try {
						for (int j = n; j < dots.size(); j++) {
							graph.removeCells(new Object[]{dots.get(j)});
						}
						E3Graph.straightenLogicUnit(graph, (mxCell) Main.contextTarget);
					} finally {
						graph.getModel().endUpdate();
					}
				} else if (dots.size() < n) {
					graph.getModel().beginUpdate();
					try {
						for (int j = dots.size(); j < n; j++) {
							E3Graph.addLogicDot(graph, (mxCell) Main.contextTarget);
						}
					} finally {
						graph.getModel().endUpdate();
					}
				}
			}
		}
		
		JMenu setPortsMenu = new JMenu("Set ports");
		for (int i = 1; i < 7; i++) {
			final int targetAmount = i;
			setPortsMenu.add(new JMenuItem(new SetPortsAction(i)));
		}
		setPortsMenu.addSeparator();
		setPortsMenu.add(new AbstractAction("Other amount") {
			@Override
			public void actionPerformed(ActionEvent e) {
				String amountStr = JOptionPane.showInputDialog(
						Main.mainFrame,
						"Enter the desired amount of ValuePorts",
						"Available ValuePorts",
						JOptionPane.QUESTION_MESSAGE);
				
				int amount = 0;
				try {
					amount = Integer.parseInt(amountStr);
					if (amount < 0) return;
				} catch (Exception ex){
					return;
				}
				
				new SetPortsAction(amount).actionPerformed(e);
			}
		});
		menu.add(setPortsMenu);

		menu.add(new JMenuItem(new AbstractAction("Rotate right") {
			@Override
			public void actionPerformed(ActionEvent e) {
				((E3Graph) graph).rotateLogicRight(Main.contextTarget);
			}
		}));

		menu.add(new JMenuItem(new AbstractAction("Rotate left") {
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.getModel().beginUpdate();
				try {
					((E3Graph) graph).rotateLogicRight(Main.contextTarget);
					((E3Graph) graph).rotateLogicRight(Main.contextTarget);
					((E3Graph) graph).rotateLogicRight(Main.contextTarget);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}));
	}
	
	public static void addPartDotMenu(JPopupMenu menu, mxGraph graph) {
		addProportionMenu(menu, graph);
		
		menu.add(new JMenuItem(new AbstractAction("Remove port") {
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.getModel().beginUpdate();
				try {
					mxCell logicUnit = (mxCell) graph.getModel().getParent(Main.contextTarget);
					graph.removeCells(new Object[]{Main.contextTarget});
					E3Graph.straightenLogicUnit(graph, logicUnit);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}));
		
		addLogicMenus(menu, graph);
	}
	
	public static void addProportionMenu(JPopupMenu menu, mxGraph graph) {
		menu.add(new JMenuItem(new AbstractAction("Set proportion") {
			@Override
			public void actionPerformed(ActionEvent e) {
				LogicDot logicDot = (LogicDot) Utils.base(graph, Main.contextTarget);
				
				String amountStr = (String) JOptionPane.showInputDialog(
						Main.mainFrame,
						"Set the desired proportion for the selected ValuePort",
						"ValuePort proportion",
						JOptionPane.INFORMATION_MESSAGE,
						null,
						null,
						logicDot.proportion + "");
				
				if (amountStr != null) amountStr = amountStr.trim();
				
				int amount = 0;
				
				try {
					amount = Integer.parseInt(amountStr);
					if (amount < 0) {
						JOptionPane.showMessageDialog(
								Main.mainFrame,
								"Proportion can only be more than or equal to 0",
								"Proportion out of bounds",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
				} catch (NumberFormatException ex) {
					if (amountStr == null)
						return;
					
					try {
						BigInteger bi = new BigInteger(amountStr);
						JOptionPane.showMessageDialog(
								Main.mainFrame,
								"Number \"" + amountStr + "\" is too big",
								"Invalid proportion",
								JOptionPane.ERROR_MESSAGE);
					} catch (NumberFormatException ex2) {
						JOptionPane.showMessageDialog(
								Main.mainFrame,
								"\"" + amountStr + "\" is not a valid proportion",
								"Invalid proportion",
								JOptionPane.ERROR_MESSAGE);
					}
					
					return;
				}
				
				logicDot.proportion = amount;
				graph.getModel().beginUpdate();
				try {
					graph.getModel().setValue(Main.contextTarget, logicDot);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}));
	}
	
	public static void addValueInterfaceMenu(JPopupMenu menu, mxGraph graph) {
		menu.add(new JMenuItem(new AbstractAction("Add incoming port") {
			@Override
			public void actionPerformed(ActionEvent e) {
				Base value = Utils.base(graph, Main.contextTarget);
				
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
				Base value = Utils.base(graph, Main.contextTarget);
				
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
		JMenuItem flipPortMenu = new JMenuItem(new AbstractAction("Flip port") {
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.getModel().beginUpdate();
				try {
					E3Graph e3graph = (E3Graph) graph;
					
					mxICell vp = (mxICell) Main.contextTarget;
					
					e3graph.setValuePortDirection(vp, !e3graph.getValuePortDirection(vp));
					
					Object[] edges = mxGraphModel.getEdges(graph.getModel(), Main.contextTarget);

					if (edges.length == 1) {
						Object edge = edges[0];
						Object otherVP = Utils.getOpposite(graph, edge, Main.contextTarget);
						
						e3graph.setValuePortDirection(otherVP, !e3graph.getValuePortDirection(otherVP));
					}
				} finally {
					graph.getModel().endUpdate();
				}
			}
		});
		
		menu.add(flipPortMenu);
		
		// Should grey out the "Flip port" menu item if the valueport has a valueexchange attached to it
		menu.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
				Object[] edges = mxGraphModel.getEdges(graph.getModel(), Main.contextTarget);
				if (edges.length == 0) {
					flipPortMenu.setEnabled(true);
					flipPortMenu.setText("Flip port");
				} else if (edges.length == 1) {
					flipPortMenu.setEnabled(true);
					flipPortMenu.setText("Flip port and port on other ends");
				} else {
					flipPortMenu.setEnabled(false);
					flipPortMenu.setText("Flip port");
				}
			}
			
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) { }
			@Override
			public void popupMenuCanceled(PopupMenuEvent arg0) { }
		});
		
		menu.add(new JMenuItem(new AbstractAction("Remove ValuePort") {
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.getModel().beginUpdate();
				try {
					Object parent = graph.getModel().getParent(Main.contextTarget);
					graph.removeCells(new Object[]{Main.contextTarget});
					E3Graph.straightenValueInterface(graph, (mxICell) parent);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}));
		
		menu.addSeparator();
		
		addValueInterfaceMenu(menu, graph);
	}
	
	public static void addValueExchangeMenu(JPopupMenu menu, mxGraph graph) {
		E3Graph e3graph = (E3Graph) graph;

		JMenu valueObjectMenu = new JMenu("ValueObject");
		valueObjectMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuCanceled(MenuEvent arg0) { }

			@Override
			public void menuDeselected(MenuEvent e) { }

			@Override
			public void menuSelected(MenuEvent e) {
				valueObjectMenu.removeAll();
				
				ValueExchange ve = (ValueExchange) Utils.base(graph, Main.contextTarget);
				
				for (String valueObject : e3graph.valueObjects) {
					if (valueObject.equals(ve.valueObject)) {
						JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(new AbstractAction(valueObject) {
							@Override
							public void actionPerformed(ActionEvent arg0) {
								// TODO: Replace this with the function in E3Graph
								graph.getModel().beginUpdate();
								try {
									ValueExchange ve = (ValueExchange) Utils.base(graph, Main.contextTarget); 
									ve.valueObject = null;
									graph.getModel().setValue(Main.contextTarget, ve);
								} finally {
									graph.getModel().endUpdate();
								}
							}
						});
						menuItem.setSelected(true);
						valueObjectMenu.add(menuItem);
					} else {
						valueObjectMenu.add(new JMenuItem(new AbstractAction(valueObject) {
							@Override
							public void actionPerformed(ActionEvent arg0) {
								graph.getModel().beginUpdate();
								try {
									ValueExchange ve = (ValueExchange) Utils.base(graph, Main.contextTarget); 
									ve.valueObject = valueObject;
									graph.getModel().setValue(Main.contextTarget, ve);
								} finally {
									graph.getModel().endUpdate();
								}
							}
						}));
					}
					
				}
				valueObjectMenu.addSeparator();
				valueObjectMenu.add(new JMenuItem(new AbstractAction("New value object...") {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						String newName = JOptionPane.showInputDialog(
								Main.mainFrame,
								"Enter the name of the new ValueObject",
								"New ValueObject",
								JOptionPane.QUESTION_MESSAGE);
						if (newName == null || newName.trim().length() == 0) return;

						e3graph.valueObjects.add(newName);
						
						ValueExchange ve = (ValueExchange) Utils.base(graph, Main.contextTarget);
						ve.valueObject = newName;
						graph.getModel().setValue(Main.contextTarget, ve);
					}
				}));
			}
		});
		menu.add(valueObjectMenu);
		
		JMenuItem removeValueObjectMenu = new JMenuItem(new AbstractAction("Remove ValueObject") {
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.getModel().beginUpdate();
				try {
					ValueExchange ve = (ValueExchange) Utils.base(graph, Main.contextTarget);
					ve.valueObject = null;
					graph.getModel().setValue(Main.contextTarget, ve);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		});
		menu.add(removeValueObjectMenu);
		
		JMenu fraudMenu = new JMenu("Fraud");
		fraudMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(MenuEvent e) {
				fraudMenu.removeAll();
				
				JCheckBoxMenuItem nonOccurringMenu = new JCheckBoxMenuItem(new AbstractAction("Non-occurring") {
					@Override
					public void actionPerformed(ActionEvent e) {
						ValueExchange ve = (ValueExchange) Utils.base(graph, Main.contextTarget);
						
						((E3Graph) graph).setValueExchangeNonOcurring(Main.contextTarget, !ve.isNonOccurring());
					}
				});
				
				JCheckBoxMenuItem hiddenMenu = new JCheckBoxMenuItem(new AbstractAction("Hidden") {
					@Override
					public void actionPerformed(ActionEvent e) {
						ValueExchange ve = (ValueExchange) Utils.base(graph, Main.contextTarget);
						
						((E3Graph) graph).setValueExchangeHidden(Main.contextTarget, !ve.isHidden());
					}
				});
				
				ValueExchange ve = (ValueExchange) Utils.base(graph, Main.contextTarget);
				if (ve.formulas.containsKey("DASHED")) {
					nonOccurringMenu.setSelected(true);
				} else if (ve.formulas.containsKey("DOTTED")) {
					hiddenMenu.setSelected(true);
				} 				

				fraudMenu.add(nonOccurringMenu);
				fraudMenu.add(hiddenMenu);
			}
			
			@Override
			public void menuDeselected(MenuEvent e) { }
			
			@Override
			public void menuCanceled(MenuEvent e) { }
		});
		
		// Disable the menu if the current graph is not a fraud graph
		if (!((E3Graph) graph).isFraud) {
			fraudMenu.setEnabled(false);
		}
		
		menu.add(fraudMenu);

		JMenuItem hideValueObjectMenu = new JMenuItem(new AbstractAction("Show/hide ValueObject") {
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.getModel().beginUpdate();
				try {
					ValueExchange ve = (ValueExchange) Utils.base(graph, Main.contextTarget);
					ve.valueObjectHidden ^= true;
					graph.getModel().setValue(Main.contextTarget, ve);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		});
		menu.add(hideValueObjectMenu);
		
		JMenuItem hideNameMenu = new JMenuItem(new AbstractAction("Show/hide name") {
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.getModel().beginUpdate();
				try {
					ValueExchange ve = (ValueExchange) Utils.base(graph, Main.contextTarget);
					ve.labelHidden ^= true;
					graph.getModel().setValue(Main.contextTarget, ve);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		});
		menu.add(hideNameMenu);
		
		// This is to make the "Remove ValueObject" button grey out when there's no ValueObject
		menu.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuCanceled(PopupMenuEvent arg0) { }

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) { }

			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
				ValueExchange ve = (ValueExchange) Utils.base(graph, Main.contextTarget);
				removeValueObjectMenu.setEnabled(ve.valueObject != null);
				hideValueObjectMenu.setEnabled(ve.valueObject != null);
				hideNameMenu.setEnabled(ve.name != null && !ve.name.trim().isEmpty());
			}
		});
		
		menu.add(new JMenuItem(new AbstractAction("Change style...") {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object subject = Main.contextTarget;
				E3StyleEditor editor = new E3StyleEditor(graph, subject);
				editor.addListener(new E3StyleEventListener() {
					@Override
					public void invoke(E3StyleEvent event) {
						String strokeColor = Utils.colorToHex(event.fillColor);
						String fontColor = Utils.colorToHex(event.fontColor);
						int fontSize = event.fontSize;
						
						graph.getModel().beginUpdate();
						try {
							graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, strokeColor, new Object[]{subject});
							graph.setCellStyles(mxConstants.STYLE_FONTCOLOR, fontColor, new Object[]{subject});
							graph.setCellStyles(mxConstants.STYLE_FONTSIZE, ""+fontSize, new Object[]{subject});
						} finally {
							graph.getModel().endUpdate();
						}
					}
				});
				
				editor.setVisible(true);
			}
		}));
	}
	
	public static void addActorMenu(JPopupMenu menu, mxGraph graph) {
		JMenu fraudMenu = new JMenu("Fraud");
		JCheckBoxMenuItem colluding = new JCheckBoxMenuItem(new AbstractAction("Colluding") {
			@Override
			public void actionPerformed(ActionEvent e) {
				Actor acInfo = (Actor) Utils.base(graph, Main.contextTarget);
				((E3Graph) graph).setColludingActor(Main.contextTarget, !acInfo.colluded);
			}
		});
		
		fraudMenu.add(colluding);
		
		if (!((E3Graph) graph).isFraud) {
			fraudMenu.setEnabled(false);
		}
		
		menu.add(fraudMenu);
		menu.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				Actor acInfo = (Actor) Utils.base(graph, Main.contextTarget);
				colluding.setSelected(acInfo.colluded);
			}
			
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }
			
			@Override
			public void popupMenuCanceled(PopupMenuEvent e) { }
		});
	}

	public static void addStartSignalMenu(JPopupMenu menu, mxGraph graph) {
		menu.add(new JMenuItem(new AbstractAction("Toggle label visibility") {
			@Override
			public void actionPerformed(ActionEvent e) {
				StartSignal value = (StartSignal) Utils.base(graph, Main.contextTarget);
				
				value.showLabel ^= true;
				
				graph.getModel().beginUpdate();
				try {
					graph.getModel().setValue(Main.contextTarget, value);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}));
	}

	public static void addEndSignalMenu(JPopupMenu menu, mxGraph graph) {
		menu.add(new JMenuItem(new AbstractAction("Toggle label visibility") {
			@Override
			public void actionPerformed(ActionEvent e) {
				EndSignal value = (EndSignal) Utils.base(graph, Main.contextTarget);
				
				value.showLabel ^= true;
				
				graph.getModel().beginUpdate();
				try {
					graph.getModel().setValue(Main.contextTarget, value);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}));
	}

	public static void addStyleMenu(JPopupMenu menu, mxGraph graph) {
		menu.add(new JMenuItem(new AbstractAction("Change style...") {
			@Override
			public void actionPerformed(ActionEvent e) {
				// We save the context target
				Object subject = Main.contextTarget;
				// And make an editor
				E3StyleEditor editor = new E3StyleEditor(graph, subject);
				editor.addListener(new E3StyleEventListener() {
					@Override
					public void invoke(E3StyleEvent event) {
						// When the ok button is clicked...
						graph.getModel().beginUpdate();
						try {
							// Get the colors in hex format
							String backgroundColor = Utils.colorToHex(event.fillColor);
							String fontColor = Utils.colorToHex(event.fontColor);

							// Set the appropriate styles
							graph.setCellStyles(mxConstants.STYLE_GRADIENTCOLOR, backgroundColor, new Object[]{subject});
							graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, backgroundColor, new Object[]{subject});

							graph.setCellStyles(mxConstants.STYLE_FONTCOLOR, fontColor, new Object[]{subject});
							graph.setCellStyles(mxConstants.STYLE_FONTSIZE, event.fontSize + "", new Object[]{subject});
							
							// If it's a market segment we have to invoke a special stencil generation function
							if (graph.getModel().getValue(subject) instanceof MarketSegment) {
								// This generates the stencil and adds it to mxGraph
								E3Style.addMarketSegmentColor(backgroundColor);
								// And this sets the style appropriately
								graph.setCellStyles(mxConstants.STYLE_SHAPE, E3Style.getMarketSegmentShapeName(backgroundColor), new Object[]{subject});
							}
						} finally {
							graph.getModel().endUpdate();
						}
					}
				});
				
				// If everything set we show the dialog :)
				editor.setVisible(true);
			}
		}));
	}
}
