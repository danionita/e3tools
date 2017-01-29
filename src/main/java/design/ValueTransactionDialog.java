package design;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;

/**
 * To-do list to correctly implement value transactions:
 * - vt dialog
 * - E3Properties needs to be able to handle a value transaction
 * - (de-)serialization & saving
 * - do/undo?
 * @author bobe
 *
 */
public class ValueTransactionDialog extends JDialog {
	private ChangeListener changeListener;
	private Main main;
	private E3Graph graph;
	private E3GraphComponent component;
	public static boolean isOpened = false;
	private mxIEventListener modelListener;
	private MouseAdapter selectionListener;

	public ValueTransactionDialog(Main main) {
		this.main = main;
		
		isOpened = true;
		
		setTitle("ValueTransaction Editor");
		
		//////////////////////
		// Build the dialog //
		//////////////////////

		JPanel mainPanel = new JPanel();
		mainPanel.setBorder(new EmptyBorder(4, 4, 4, 2));
		getContentPane().add(mainPanel, BorderLayout.CENTER);
		mainPanel.setLayout(new BorderLayout(0, 0));
		
		JPanel panel = new JPanel();
		mainPanel.add(panel, BorderLayout.CENTER);
		panel.setLayout(new BorderLayout(0, 0));
		
		JPanel buttonPanel = new JPanel();
		panel.add(buttonPanel, BorderLayout.NORTH);
		
		JButton newButton = new JButton("New");
		buttonPanel.add(newButton);
		
		JButton btnEdit = new JButton("Edit");
		buttonPanel.add(btnEdit);
		
		JButton deleteButton = new JButton("Delete");
		buttonPanel.add(deleteButton);
		
		JList vtList = new JList();
		
		JScrollPane scrollPane = new JScrollPane(vtList);
		panel.add(scrollPane, BorderLayout.CENTER);
		
		JPanel closePanel = new JPanel();
		panel.add(closePanel, BorderLayout.SOUTH);
		
		JButton closeButton = new JButton("Close");
		closePanel.add(closeButton);
		
		//////////////////////////////
		// Done building the dialog //
		//////////////////////////////

		// This should refresh the window everytime the user switches to a new tab
		changeListener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				setFocusToCurrentGraph();
			}
		};
		main.views.addChangeListener(changeListener);

		// To listen when the dialog closes.
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				cleanupGraph();
				main.views.removeChangeListener(changeListener);
				
				isOpened = false;
			}
		});

		// Don't think we need this
		modelListener = new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				rebuildList();
			}
		};
		
		selectionListener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// Get the currently selected value object.
				// If nothing is selected, we don't have to do anything.
//				int index = valueObjectsList.getSelectedIndex();
//				if (index == -1) return;
//				String newVO = graph.valueObjects.get(index);
//
//				// Get the info object of the cell below the click position
//				Object cell = main.getCurrentGraphComponent().getCellAt(e.getX(), e.getY());
//				// Get the info object and copy it implicitly
//				Base value = Utils.base(graph, cell);
//
//				// If it is a value exchange...
//				if (value instanceof ValueExchange) {
//					ValueExchange veInfo = (ValueExchange) value;
//
//					// If the valueexchange already has the value object, remove it.
//					// Otherwise assign it. This way you can "toggle" value objects
//					// by clicking.
//					if (veInfo.valueObject != null && veInfo.valueObject.equals(newVO)) {
//						veInfo.valueObject = null;
//					} else {
//						veInfo.valueObject = newVO;
//					}
//					
//					// Update the graph with the new value exchange info object
//					graph.getModel().beginUpdate();
//					try {
//						graph.getModel().setValue(cell, veInfo);
//					} finally {
//						graph.getModel().endUpdate();
//					}
//				}
				System.out.println("Mouse clicked!");
			}
		};
		
		setFocusToCurrentGraph();
	}
	
	public void cleanupGraph() {
		// TODO: Remove optional styling here
		
		graph.repaint();

		graph.getModel().removeListener(modelListener);
		component.getGraphControl().removeMouseListener(selectionListener);
	}
	
	public void setFocusToCurrentGraph() {
		if (graph != null) {
			cleanupGraph();
		}
		
		// Get and save the new current graph & component
		graph = main.getCurrentGraph();
		component = main.getCurrentGraphComponent();

		// Add the listeners
		graph.getModel().addListener("change", modelListener);
		component.getGraphControl().addMouseListener(selectionListener);
	}
	
	public void rebuildList() {
		
	}

}
