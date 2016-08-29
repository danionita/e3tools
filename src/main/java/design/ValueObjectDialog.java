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

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;

import design.info.Base;
import design.info.ValueExchange;

/**
 * Value object dialog that shows an overview of all value objects in the current graph,
 * how often they are used, and allows to "batch-assign" value objects to value exchanges.
 * The dialog refreshes appropriately when tabs are switched.
 * @author Bobe
 *
 */
public class ValueObjectDialog {
	private E3Graph graph;
	private Main main;
	private JDialog dialog;
	private DefaultListModel<String> listModel;
	private mxIEventListener modelListener;
	private MouseAdapter selectionListener;
	private JList<String> valueObjectsList;
	private E3GraphComponent component;

	/**
	 * Creates a ValueObjectDialog that keeps an eye on the editor window
	 * if the graph is switched. If the graph is switched, the object window
	 * is refreshed to show the proper value objects. When a value object is selected
	 * you can click on edges to assign the value object to edges.
	 * @param main
	 * @param graph
	 */
	public ValueObjectDialog(Main main) {
		// Save the main so we can use it everywhere
		this.main = main;
		
		// This should refresh the window everytime the user switches to a new tab
		main.views.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				setFocusToCurrentGraph();
			}
		});
		
		// Constructs the dialog
		buildDialog();
		
		// This listener is attached to the current graph and rebuilds the list every
		// time something changes in the graph (mostly to keep the value object counts
		// in sync)
		modelListener = new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				rebuildList();
			}
		};
		
		// This listener applies the currently selected value object to the clicked
		// value exchange
		selectionListener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// Get the currently selected value object.
				// If nothing is selected, we don't have to do anything.
				int index = valueObjectsList.getSelectedIndex();
				if (index == -1) return;
				String newVO = graph.valueObjects.get(index);

				// Get the info object of the cell below the click position
				Object cell = main.getCurrentGraphComponent().getCellAt(e.getX(), e.getY());
				// Get the info object and copy it implicitly
				Base value = Utils.base(graph, cell);

				// If it is a value exchange...
				if (value instanceof ValueExchange) {
					ValueExchange veInfo = (ValueExchange) value;

					// If the valueexchange already has the value object, remove it.
					// Otherwise assign it. This way you can "toggle" value objects
					// by clicking.
					if (veInfo.valueObject != null && veInfo.valueObject.equals(newVO)) {
						veInfo.valueObject = null;
					} else {
						veInfo.valueObject = newVO;
					}
					
					// Update the graph with the new value exchange info object
					graph.getModel().beginUpdate();
					try {
						graph.getModel().setValue(cell, veInfo);
					} finally {
						graph.getModel().endUpdate();
					}
				}
			}
		};
		
		setFocusToCurrentGraph();
	}
	
	/**
	 * Removes listeners from the old graph(component) (if applicable), and adds them to
	 * the new graph(component). Also refreshes the list of value objects.
	 */
	public void setFocusToCurrentGraph() {
		// If there was a graph, remove all listeners from it & reset the highlighting
		if (graph != null) {
			cleanupGraph();
		}
		
		// Get and save the new current graph & component
		graph = main.getCurrentGraph();
		component = main.getCurrentGraphComponent();

		// Add the listeners
		graph.getModel().addListener("change", modelListener);
		component.getGraphControl().addMouseListener(selectionListener);
		
		// Refresh the list
		rebuildList();
	}
	
	/**
	 * Removes all listeners from the graph and resets the highlighting of the graph
	 */
	private void cleanupGraph() {
		// For all cells...
		for (Object obj : Utils.getAllCells(graph)) {
			Base val = Utils.base(graph, obj);
			// If they are a value exchange...
			if (val instanceof ValueExchange) {
				// Set their stroke color to blue
				graph.getView().getState(obj).getStyle().put(mxConstants.STYLE_STROKECOLOR, "#0000FF");
			}
		}
		
		graph.repaint();

		// Remove the listeners
		graph.getModel().removeListener(modelListener);
		component.getGraphControl().removeMouseListener(selectionListener);
	}

	/**
	 * Rebuilds the list with the current graph's value objects & statistics
	 */
	public void rebuildList() {
		// A map to keep track of the appearances of the value objects
		Map<String, Integer> count = new HashMap<>();
		
		// For every cell...
		for (Object cell : Utils.getAllCells(graph)) {
			Object val = graph.getModel().getValue(cell);
			// If it is a value exchange
			if (val instanceof ValueExchange) {
				ValueExchange ve = (ValueExchange) val;
				// That has a value object...
				if (ve.valueObject != null) {
					// Increase the count of that value object by one.
					count.put(ve.valueObject, count.getOrDefault(ve.valueObject, 0) + 1);
				}
			}
		}

		// Save the old selected index
		int oldIndex = valueObjectsList.getSelectedIndex();

		// Clear the list and repopulate it
		listModel.clear();
		for (String valueObject : graph.valueObjects) {
			listModel.addElement(valueObject + " (" + count.getOrDefault(valueObject, 0) + "x)");
		}
		
		// Since the order of the value objects cannot be changed (it is controlled by the graph)
		// we can just reselect the old index. Also, this triggers an event on the list object,
		// which causes the possibly added value exchange to be highlighted as well.
		valueObjectsList.setSelectedIndex(oldIndex);
	}
	
	/**
	 * This builds the actual swing dialog. Basically boring swing stuff.
	 * It is also the place where the edge coloring is done.
	 */
	@SuppressWarnings("serial")
	private void buildDialog() {
		dialog = new JDialog(Main.mainFrame, "ValueObjects", Dialog.ModalityType.MODELESS);
		dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		JButton newButton = new JButton();
		buttonPanel.add(newButton);
		JButton deleteButton = new JButton();
		buttonPanel.add(deleteButton);
		dialog.add(buttonPanel);
		
		listModel = new DefaultListModel<>();

		valueObjectsList = new JList<String>(listModel);
		valueObjectsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		valueObjectsList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) return; // We only do something if the event is final, i.e. if the event is the last one

				// TODO: If cell recognition is no longer done with styles, refactor this
				// such that only the explicit style of a cell is changed.
				// I don't like messing with this state thing
				// (Altough it worked almost immediately. Maybe this is the right way?)
				// A benefit of this method is that it does not affect undo history 
				int selectedIndex = valueObjectsList.getSelectedIndex();
				if (selectedIndex == -1) return;

				String valueObject = graph.valueObjects.get(selectedIndex);
				for (Object obj : Utils.getAllCells(graph)) {
					Base val = Utils.base(graph, obj);
					if (val instanceof ValueExchange) {
						ValueExchange ve = (ValueExchange) val;
						if (ve.valueObject != null && ve.valueObject.equals(valueObject)) {
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
						Main.mainFrame,
						"Enter the name of the new ValueObject",
						"New ValueObject",
						JOptionPane.QUESTION_MESSAGE);
				if (newName == null || newName.trim().length() == 0) return;
				if (graph.valueObjects.indexOf(newName) != -1) return;

				listModel.addElement(newName + " (0x)");
				graph.valueObjects.add(newName);
			}
		});
		
		deleteButton.setAction(new AbstractAction("Delete") {
			@Override
			public void actionPerformed(ActionEvent e) {
				int index = valueObjectsList.getSelectedIndex();
				if (index == -1) return;
				String valueObjectName = graph.valueObjects.get(index);

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
						Main.mainFrame,
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
					graph.valueObjects.remove(index);
				}
			}
		});
		
		// Makes all edges in the graph blue again in case they've been highlighted
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				cleanupGraph();
			}
		});
	}

	/**
	 * Shows the swing dialog.
	 */
	public void show() {
		dialog.setSize(300, 320);
		dialog.setVisible(true);
	}
}
