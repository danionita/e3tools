package design.main;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

import design.main.Info.Base;
import design.main.Info.ValueExchange;

public class Main { 
	
	public static final JFrame mainFrame = new JFrame("E3fraud editor");
	public static mxGraph graph = null;
	public static E3GraphComponent graphComponent = null;
	public static Object contextTarget = null;
	public static mxPoint contextPos = new mxPoint(-1, -1);
	public static ToolComponent tools = null;
	
	public static final ArrayList<String> valueObjects = new ArrayList<>(
			Arrays.asList("MONEY", "SERVICE")
			);
	
	public Main() {
		// Set LaF to system
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e){
			System.out.println("Couldn't set Look and Feel to system");
		}

		// Add menubar
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(new JMenu("File"));
		menuBar.add(new JMenu("Graph"));
		
		JMenu valueObjectsMenu = new JMenu("ValueObjects");
		valueObjectsMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(MenuEvent e) {
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
				for (Object cell : Utils.getAllCells(graph)) {
					Object val = graph.getModel().getValue(cell);
					if (val instanceof ValueExchange) {
						ValueExchange ve = (ValueExchange) val;
						if (ve.valueObject != null) {
							count.put(ve.valueObject, count.getOrDefault(ve.valueObject, 0) + 1);
						}
					}
				}
				
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
						String valueObject = valueObjects.get(valueObjectsList.getSelectedIndex());
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
				
				valueObjectsMenu.setSelected(false);
				
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
			
			@Override
			public void menuDeselected(MenuEvent e) { }
			
			@Override
			public void menuCanceled(MenuEvent e) { }
		});
		menuBar.add(valueObjectsMenu);
		
		mainFrame.setJMenuBar(menuBar);
		
		graph = new E3Graph();
		Object root = graph.getDefaultParent();
		graphComponent = new E3GraphComponent(graph);
		
		graph.getModel().beginUpdate();
		try {
			// Playground for custom shapes
		} finally {
			graph.getModel().endUpdate();
		}

		tools = new ToolComponent();
		
		JTabbedPane valueProperties = new JTabbedPane();	
		
		// Create split view
		JSplitPane mainPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tools, graphComponent);
		mainPane.setResizeWeight(0.025);
		mainFrame.getContentPane().add(mainPane);
		
		// Show main screen
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setSize(1024, 768);
		mainFrame.setVisible(true);
	}
	
	public static void main(String[] args) {
		Main t = new Main();
	}
}
