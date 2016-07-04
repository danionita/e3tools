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

import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;

import design.main.Info.Base;
import design.main.Info.SignalDot;
import design.main.Info.ValueExchange;
import design.main.Info.ValueInterface;
import design.main.Info.ValuePort;
import design.main.Utils.ClosableTabHeading;
import e3fraud.gui.MainWindow;
import e3fraud.model.E3Model;

public class Main { 
	
	public static final JFrame mainFrame = new JFrame("e3tools editor");
	public static Object contextTarget = null;
	public static mxPoint contextPos = new mxPoint(-1, -1);
	public static int newGraphCounter = 1;
	public static ToolComponent globalTools;
	public static final boolean mirrorMirrorOnTheWallWhoIsTheFairestOfThemAll = false;

	public JTabbedPane views;
	
	public E3Graph getCurrentGraph() {
		JSplitPane pane = (JSplitPane) views.getComponentAt(views.getSelectedIndex());
		E3GraphComponent graphComponent = (E3GraphComponent) pane.getRightComponent();
		return (E3Graph) graphComponent.getGraph();
	}
	
	public String getCurrentGraphName() {
		return ((ClosableTabHeading) views.getTabComponentAt(views.getSelectedIndex())).title;
	}
	
	public void addNewTabAndSwitch() {
		addNewTabAndSwitch(new E3Graph());
	}
	
	public void addNewTabAndSwitch(E3Graph graph) {
		E3GraphComponent graphComponent = new E3GraphComponent(graph);
		
		graph.getModel().beginUpdate();
		try {
			// Playground for custom shapes
		} finally {
			graph.getModel().endUpdate();
		}

		graphComponent.refresh();

		// Create split view
		JSplitPane mainpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new ToolComponent(), graphComponent);
		mainpane.setResizeWeight(0.025);

		Utils.addClosableTab(views, "Such Model " + newGraphCounter++, mainpane);
		views.setSelectedIndex(views.getTabCount() - 1);
	}
	
	public Main() {
		// Silly log4j
		Logger.getRootLogger().setLevel(Level.OFF);
		
		if (mirrorMirrorOnTheWallWhoIsTheFairestOfThemAll) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			catch(Exception e){
				System.out.println("Couldn't set Look and Feel to system");
			}
		}

		// Add menubar
		JMenuBar menuBar = new JMenuBar();
		
		JMenu fileMenu = new JMenu("File");
		fileMenu.add(new JMenuItem(new AbstractAction("New e3graph") {
			@Override
			public void actionPerformed(ActionEvent e) {
				addNewTabAndSwitch();
			}
		}));
		JMenuItem duplicateGraph = new JMenuItem(new AbstractAction("Duplicate current graph") {
			@Override
			public void actionPerformed(ActionEvent e) {
				addNewTabAndSwitch(Utils.cloneGraph(getCurrentGraph()));
			}
		});
		fileMenu.add(duplicateGraph);
		JMenuItem saveAs = new JMenuItem(new AbstractAction("Save as...") {
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(mainFrame, "Save functionality is not implemented yet", "Error", JOptionPane.ERROR_MESSAGE);
			}
		});
		fileMenu.add(saveAs);
		JMenuItem exportRDF = new JMenuItem(new AbstractAction("Export to RDF...") {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println(new RDFExport(getCurrentGraph()).toString());
			}
		});
		fileMenu.add(exportRDF);
		fileMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(MenuEvent e) {
				boolean enabled = views.getTabCount() > 0;
				duplicateGraph.setEnabled(enabled);
				saveAs.setEnabled(enabled);
				exportRDF.setEnabled(enabled);
			}
			
			@Override
			public void menuDeselected(MenuEvent e) { }
			
			@Override
			public void menuCanceled(MenuEvent e) { }
		});
		menuBar.add(fileMenu);
		
		JMenu graphMenu = new JMenu("Model");
		graphMenu.add(new JMenuItem(new AbstractAction("Perform e3fraud analysis") {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO: Convert this option to something greyed out, just like in the file menu?
				if (views.getTabCount() == 0) {
					JOptionPane.showMessageDialog(
							Main.mainFrame, 
							"A model must be opened to analyze. Click File ➡ New model to open a new model.",
							"No model available",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				RDFExport rdfExporter = new RDFExport(getCurrentGraph());
				MainWindow main = new MainWindow(new E3Model(rdfExporter.model), getCurrentGraphName());
				main.showGraph(getCurrentGraph());

				Component analysis = Utils.addClosableTab(views, "Fraud analysis of \"" + getCurrentGraphName() + "\"", main);

				views.setSelectedIndex(views.indexOfComponent(analysis));
			}
		}));
		
		graphMenu.add(new JMenuItem(new AbstractAction("Show ValueObjects") {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO: Maybe prefer greyed out menu item?
				if (views.getTabCount() == 0) {
					JOptionPane.showMessageDialog(
							Main.mainFrame, 
							"A model must be opened to display its ValueObjects. Click File ➡ New model to open a new model.",
							"No model available",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				JDialog dialog = new JDialog(mainFrame, "ValueObjects", Dialog.ModalityType.DOCUMENT_MODAL);
				dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
				
				JPanel buttonPanel = new JPanel();
				buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
				JButton newButton = new JButton();
				buttonPanel.add(newButton);
				JButton deleteButton = new JButton();
				buttonPanel.add(deleteButton);
				dialog.add(buttonPanel);

				Map<String, Integer> count = new HashMap<>();
				E3Graph graph = getCurrentGraph();
				for (Object cell : Utils.getAllCells(graph)) {
					Object val = graph.getModel().getValue(cell);
					if (val instanceof ValueExchange) {
						ValueExchange ve = (ValueExchange) val;
						if (ve.valueObject != null) {
							count.put(ve.valueObject, count.getOrDefault(ve.valueObject, 0) + 1);
						}
					}
				}
				
				List<String> valueObjects = graph.valueObjects;
				DefaultListModel<String> listModel = new DefaultListModel<>();
				for (String valueObject : valueObjects) {
					listModel.addElement(valueObject + " (" + count.getOrDefault(valueObject, 0) + "x)");
				}
				
				JList valueObjectsList = new JList(listModel);
				valueObjectsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				valueObjectsList.addListSelectionListener(new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent e) {
						if (e.getValueIsAdjusting()) return; // We only do something if the event is final, i.e. if the event is the last one

						// TODO: If cell recognition is no longer done with styles, refactor this
						// such that only the explicit style of a cell is changed.
						// I don't like messing with this state thing
						// (Altough it worked almost immediately. Maybe this is the right way?)
						// A benefit of this method is that it does not affect undo history (but is that actually true?).
						int selectedIndex = valueObjectsList.getSelectedIndex();
						if (selectedIndex == -1) return;

						String valueObject = valueObjects.get(selectedIndex);
						for (Object obj : Utils.getAllCells(graph)) {
							Base val = Utils.base(graph, obj);
							if (val instanceof ValueExchange) {
								ValueExchange ve = (ValueExchange) val;
								if (ve.valueObject.equals(valueObject)) {
									graph.getView().getState(obj).getStyle().put(mxConstants.STYLE_STROKECOLOR, "#00FF00");
								} else {
									graph.getView().getState(obj).getStyle().put(mxConstants.STYLE_STROKECOLOR, "#0000FF");
								}
							}
						}
						
						graph.repaint();
					}
				});

				JScrollPane listScroller = new JScrollPane(valueObjectsList);
				dialog.add(listScroller);
				
				newButton.setAction(new AbstractAction("New") {
					@Override
					public void actionPerformed(ActionEvent e) {
						String newName = JOptionPane.showInputDialog(
								mainFrame,
								"Enter the name of the new ValueObject",
								"New ValueObject",
								JOptionPane.QUESTION_MESSAGE);
						if (newName == null || newName.trim().length() == 0) return;
						if (valueObjects.indexOf(newName) != -1) return;

						listModel.addElement(newName + " (0x)");
						valueObjects.add(newName);
					}
				});
				
				deleteButton.setAction(new AbstractAction("Delete") {
					@Override
					public void actionPerformed(ActionEvent e) {
						int index = valueObjectsList.getSelectedIndex();
						if (index == -1) return;
						String valueObjectName = valueObjects.get(index);

						List<Object> usingCells = new ArrayList<>();
						for (Object cell : Utils.getAllCells(graph)) {
							Object val = graph.getModel().getValue(cell);
							if (val instanceof ValueExchange) {
								ValueExchange ve = (ValueExchange) val;
								if (ve.valueObject != null && ve.valueObject.equals(valueObjectName)) {
									usingCells.add(cell);
								}
							}
						}
						
						int response = JOptionPane.showConfirmDialog(
								mainFrame,
								"You are about to delete the ValueObject \""
										+ valueObjectName 
										+ "\". This ValueObject is used "
										+ usingCells.size()
										+ " times in this model. Would you like to proceed?",
								"Deletion confirmation",
								JOptionPane.OK_CANCEL_OPTION,
								JOptionPane.WARNING_MESSAGE
								);
						
						if (response == JOptionPane.OK_OPTION) {
							graph.getModel().beginUpdate();
							try {
								for (Object cell : usingCells) {
									ValueExchange ve = Utils.getValueExchange(cell);
									ve.valueObject = null;
									graph.getModel().setValue(cell, ve);
								}
							} finally {
								graph.getModel().endUpdate();
							}

							listModel.remove(valueObjectsList.getSelectedIndex());
							valueObjects.remove(index);
						}
					}
				});
				
				// Makes all edges in the graph blue again in case they've been highlighted
				dialog.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent e) {
						for (Object obj : Utils.getAllCells(graph)) {
							Base val = Utils.base(graph, obj);
							if (val instanceof ValueExchange) {
								ValueExchange ve = (ValueExchange) val;
								graph.getView().getState(obj).getStyle().put(mxConstants.STYLE_STROKECOLOR, "#0000FF");
							}
						}
						
						graph.repaint();
					}
				});
				
				dialog.setSize(300, 320);
				dialog.setVisible(true);
			}
		}));
		
		menuBar.add(graphMenu);

		JMenu exampleMenu = new JMenu("Examples");

		exampleMenu.add(new JMenuItem(new AbstractAction("Small tricky graph") {
			@Override
			public void actionPerformed(ActionEvent e) {
				E3Graph graph = getCurrentGraph();
				mxGraphModel model = (mxGraphModel) graph.getModel();
				
				ToolComponent tc = Main.globalTools;
				
				Object root = graph.getDefaultParent();
				
				model.beginUpdate();
				try {
					Object tl = graph.addActor(100, 100);
					Object bl = graph.addActor(100, 250);
					
					Object tlBottom = graph.addValueInterface(tl, 30, 50);
					Object blTop = graph.addValueInterface(bl, 30, 0);
					
					graph.connectVE(tlBottom, blTop);
					graph.connectVE(blTop, tlBottom);
					
					Object ss = graph.addStartSignal(bl, 20, 20);
					Object es =  graph.addEndSignal(tl, 20, 20);
					
					graph.connectCE(ss, blTop);
					graph.connectCE(tlBottom, es);
				} finally {
					model.endUpdate();
				}
			}
		}));

		exampleMenu.add(new JMenuItem(new AbstractAction("Medium tricky graph") {
			@Override
			public void actionPerformed(ActionEvent e) {
				E3Graph graph = getCurrentGraph();
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
		}));
		
		menuBar.add(exampleMenu);
		
		mainFrame.setJMenuBar(menuBar);
		
		globalTools = new ToolComponent();
		
		views = new JTabbedPane();	
		
		addNewTabAndSwitch();
		
		mainFrame.getContentPane().add(views);
		
		// Show main screen
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setSize(1024, 768);
		mainFrame.setVisible(true);
		
//		// Add default model
//		mxCell marketSegment = (mxCell) tools.clone(tools.marketSegment);
//		Base marketSegmentValue = (Base) marketSegment.getValue();
//		marketSegmentValue.name = "Average User";
//		graph.addCell(marketSegment);
//		
//		mxCell actor = (mxCell) tools.clone(tools.actor);
//		Base actorValue = (Base) actor.getValue();
//		actorValue.name = "Average company";
//		graph.addCell(actor);
//		
//		mxCell topValueInterface = (mxCell) tools.clone(tools.valueInterface);
	}
	
	public static void main(String[] args) {
		Main t = new Main();
	}
}
