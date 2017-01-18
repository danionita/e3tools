package design.checker;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import design.E3Graph;
import design.Main;

public class E3CheckDialog extends JDialog {
	public static boolean isOpened = false;
	
	DefaultListModel<String> model;
	List<ModelError> errors;
	E3Graph graph;
	Main main;

	public E3CheckDialog(Main main) {
		this.main = main;
		
		isOpened = false;
		
		setTitle("Error checking dialog");
		
		/**********************/
		/** Build the dialog **/
		/**********************/
		
		JPanel buttonPanel = new JPanel();
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		buttonPanel.setBorder(new EmptyBorder(6, 6, 6, 6));
		buttonPanel.setLayout(new BorderLayout(0, 0));
		
		JPanel buttonSidePanel = new JPanel();
		buttonPanel.add(buttonSidePanel, BorderLayout.EAST);
		
		JButton refreshButton = new JButton("Refresh");
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
		
		JPanel errorPanel = new JPanel();
		errorPanel.setBorder(new EmptyBorder(6, 6, 0, 6));
		getContentPane().add(errorPanel, BorderLayout.CENTER);
		errorPanel.setLayout(new BorderLayout(0, 0));
		
		model = new DefaultListModel<String>();
		
		JList<String> errorList = new JList<String>(model);
		errorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JScrollPane jsp = new JScrollPane(errorList);
		errorPanel.add(jsp, BorderLayout.CENTER);
		
		/******************************/
		/** Done building the dialog **/
		/******************************/
		
		errorList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				
			}
		});
		
		setFocusToCurrentGraph();
	}
	
	public void cleanupGraph() {
		
	}
	
	public void setFocusToCurrentGraph() {
		if (graph != null) {
			cleanupGraph();
		}
		
		// Get the new graph
		graph = main.getCurrentGraph();
		
		// Get all the errors
		errors = E3Checker.checkForErrors(graph);
		
		// Set the list properly
		model.clear();
		
		errors.stream().forEach(me -> model.addElement(me.message));
	}

	public static void main(String[] args) {
//		try {
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//		} catch (Exception e) {
//			System.out.println("Couldn't set Look and Feel to system");
//		}
//
//		E3CheckDialog e3mcd = new E3CheckDialog(;
//		
//		e3mcd.setSize(400, 300);
//		e3mcd.setVisible(true);
	}
}
