package design.checker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.mxgraph.util.mxConstants;

import design.E3Graph;
import design.Main;
import design.Utils;
import design.info.Bar;
import design.info.Base;
import design.info.LogicBase;
import design.info.Triangle;

/**
 * TODO: Checks still to be implemented:
 * - Check wrong formulas
 * - For an and gate, if checkable, incoming ce's must have the same occurrence rates
 * 
 * Done:
 * - Loops
 * - End stimuli check
 * - Conflicting flow check
 * - Start stimuli check
 * - Unused ports
 * - Connected ports must have the same value objects (Dan? doubly ports or ports on either side of a VE)
 *   - But we cannot assign value objects to ports, so only doubly edges?
 *   - Jup!
 *   
 * @author bobe
 *
 */
public class E3CheckDialog extends JDialog {
	public static boolean isOpened = false;
	
	DefaultListModel<String> model;
	List<ModelError> errors;
	E3Graph graph;
	Main main;
	ModelError previousErrorMsg;
	ChangeListener changeListener;
	JList<String> errorList; 
	
	final String ERROR_COLOR = "#FFFF00";
	final String ERROR_WIDTH = "2";

	public E3CheckDialog(Main main) {
		this.main = main;
		
		isOpened = true;
		
		setTitle("Error checking dialog");

		model = new DefaultListModel<String>();
		
		// This should refresh the window every time the user switches to a new tab
		changeListener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				setFocusToCurrentGraph();
			}
		};
		main.views.addChangeListener(changeListener);

		// Clean up the graph when the window closes
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				cleanupGraph();
				graph.repaint();

				main.views.removeChangeListener(changeListener);
				
				isOpened = false;
			}
		});
		
		/**********************/
		/** Build the dialog **/
		/**********************/
		
		JPanel descriptionPanel = new JPanel();
		descriptionPanel.setBorder(new EmptyBorder(6, 6, 6, 6));
		getContentPane().add(descriptionPanel, BorderLayout.NORTH);
		descriptionPanel.setLayout(new BorderLayout(0, 0));
		
		JLabel descriptionLabel = new JLabel("Select an error to highlight the objects involved in yellow.");
		descriptionPanel.add(descriptionLabel);
		
		JPanel errorPanel = new JPanel();
		errorPanel.setBorder(new EmptyBorder(6, 6, 0, 6));
		getContentPane().add(errorPanel, BorderLayout.CENTER);
		errorPanel.setLayout(new BorderLayout(0, 0));
		
		errorList = new JList<String>(model);
		errorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JScrollPane jsp = new JScrollPane(errorList);
		errorPanel.add(jsp, BorderLayout.CENTER);
		
		errorList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) return; // We only do something if the event is final, i.e. if the event is the last one
				
				int selectedIndex = errorList.getSelectedIndex();
				if (selectedIndex == -1) return;
				
				// If there are no errors, ignore all selections.
				if (errors.size() < 1) return;
				
				ModelError errorMsg = errors.get(selectedIndex);
				System.out.println("Message: " + errorMsg.message);
				
				if (previousErrorMsg != null) {
					removeHighlighting(previousErrorMsg);
				}
				
				applyHighlighting(errorMsg);
				
				previousErrorMsg = errorMsg;

				// Trigger repaint because we're kick-ass swing programmers.
				graph.repaint();
			}
		});
		
		JPanel buttonPanel = new JPanel();
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		buttonPanel.setBorder(new EmptyBorder(6, 6, 6, 6));
		buttonPanel.setLayout(new BorderLayout(0, 0));
		
		JPanel buttonSidePanel = new JPanel();
		buttonPanel.add(buttonSidePanel, BorderLayout.EAST);
		
		JButton refreshButton = new JButton("Refresh");
		refreshButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				setFocusToCurrentGraph();
			}
		});
		buttonSidePanel.add(refreshButton);
		
		JButton closeButton = new JButton("Close");
		buttonSidePanel.add(closeButton);
		closeButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				// Simple dialog close event
				E3CheckDialog.this.setVisible(false);
                E3CheckDialog.this.dispatchEvent(new WindowEvent(E3CheckDialog.this, WindowEvent.WINDOW_CLOSING));
			}
		});

		setSize(400, 300);

		/******************************/
		/** Done building the dialog **/
		/******************************/
		
		setFocusToCurrentGraph();
	}
	
	public void removeHighlighting(ModelError errorMsg) {
		if (errorMsg.subjects == null) return;

		for (Object obj : errorMsg.subjects) {
			Base info = (Base) graph.getModel().getValue(obj);

			// If highlighting a logic base, highlight the triangle/bar
			if (info instanceof LogicBase) {
				for (int i = 0; i < graph.getModel().getChildCount(obj); i++) {
					Object c =  graph.getModel().getChildAt(obj, i);
					if (graph.getModel().getValue(c) == null) {
						obj = c;
						break;
					}
				}
			}

			Utils.resetCellStateProperty(graph, obj, mxConstants.STYLE_STROKECOLOR);
			Utils.resetCellStateProperty(graph, obj, mxConstants.STYLE_STROKEWIDTH);
		}
	}
	
	public void applyHighlighting(ModelError errorMsg) {
		if (errorMsg.subjects == null) return;
		
		System.out.println("Applying highlighting!");
		
		for (Object obj : errorMsg.subjects) {
			Base info = (Base) graph.getModel().getValue(obj);

			// If highlighting a logic base, highlight the triangle/bar
			if (info instanceof LogicBase) {
				for (int i = 0; i < graph.getModel().getChildCount(obj); i++) {
					Object c =  graph.getModel().getChildAt(obj, i);
					if (graph.getModel().getValue(c) == null) {
						obj = c;
						break;
					}
				}
			}

			// Make it fat error color
			Utils.setCellStateProperty(graph, obj, mxConstants.STYLE_STROKECOLOR, ERROR_COLOR);
			Utils.setCellStateProperty(graph, obj, mxConstants.STYLE_STROKEWIDTH, ERROR_WIDTH);
		}
	}
	
	public void cleanupGraph() {
		if (previousErrorMsg != null) {
			removeHighlighting(previousErrorMsg);
			
			previousErrorMsg = null;
		}
	}
	
	public void setFocusToCurrentGraph() {
		if (graph != null) {
			cleanupGraph();
			graph.repaint();
		}
		
		// Get the new graph
		graph = main.getCurrentGraph();
		
		// Get all the errors
		errors = E3Checker.checkForErrors(graph);
		
		// Set the list properly
		model.clear();
		
		errors.stream().forEach(me -> model.addElement(me.message));
		
		if (errors.isEmpty()) {
			model.addElement("No errors detected in currently selected model.");
			errorList.setForeground(Color.GRAY);
		} else {
			errorList.setForeground(Color.BLACK);
		}
	}
}
