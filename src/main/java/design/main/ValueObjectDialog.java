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
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.mxgraph.util.mxConstants;

import design.main.Info.Base;
import design.main.Info.ValueExchange;

public class ValueObjectDialog {
	private E3Graph graph;

	public ValueObjectDialog(E3Graph graph) {
		this.graph = graph;
	}
	
	public void show() {
		JDialog dialog = new JDialog(Main.mainFrame, "ValueObjects", Dialog.ModalityType.DOCUMENT_MODAL);
		dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		JButton newButton = new JButton();
		buttonPanel.add(newButton);
		JButton deleteButton = new JButton();
		buttonPanel.add(deleteButton);
		dialog.add(buttonPanel);

		Map<String, Integer> count = new HashMap<>();
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
						Main.mainFrame,
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
}
