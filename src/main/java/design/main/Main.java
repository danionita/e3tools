package design.main;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.UIManager;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

public class Main { 
	
	public final JFrame mainFrame = new JFrame("E3fraud editor");
	
	public static int nextSUID = 0;
	public static int getSUID() {
		return nextSUID++;
	}
	
	public static class E3Object {
		public final int SUID;
		String name;
		public final HashMap<String, String> formulas = new LinkedHashMap<>();
		
		E3Object(String name) {
			SUID = getSUID();
		}
	}
	
	public static class E3PropertiesEditor {
		public E3PropertiesEditor(JDialog owner, E3Object object) {
			JDialog dialog = new JDialog(owner, "Edit object");
			Container contentPane = dialog.getContentPane();
			SpringLayout layout = new SpringLayout();
			contentPane.setLayout(layout);
			
			JLabel idLabel = new JLabel("ID:");
			dialog.add(idLabel);
			JLabel idNumLabel = new JLabel(object.SUID + "");
			dialog.add(idNumLabel);
			
			JLabel nameLabel = new JLabel("Name:");
			dialog.add(nameLabel);
			JTextField nameField = new JTextField(object.name);
			dialog.add(nameField);
			
			layout.putConstraint(SpringLayout.WEST, contentPane,
					5,
					SpringLayout.WEST, idLabel);
			layout.putConstraint(SpringLayout.EAST, contentPane,
					5,
					SpringLayout.EAST, idNumLabel);
			
			dialog.setSize(400, 500);
			dialog.pack();
			dialog.setVisible(true);
		}
	}
	
	public static final ArrayList<E3Object> valueObjects = new ArrayList<>();
	public static final ArrayList<E3Object> transactionObjects = new ArrayList<>();
	
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
		
		JMenu showMenu = new JMenu("Show");
		showMenu.add(new JMenuItem(new AbstractAction("ValueObjects") {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Opening ValueObjects...");
				JDialog dialog = new JDialog(mainFrame, "ValueObjects", Dialog.ModalityType.DOCUMENT_MODAL);
				dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
				
				JPanel buttonPanel = new JPanel();
				buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
				buttonPanel.add(new JButton("New"));
				buttonPanel.add(new JButton(new AbstractAction("Edit") {
					@Override
					public void actionPerformed(ActionEvent e) {
						new E3PropertiesEditor(dialog, new E3Object("BobeObject"));
					}
				}));
				buttonPanel.add(new JButton("Delete"));
				dialog.add(buttonPanel);
				
				JList valueObjectsList = new JList(new Object[]{"Item 1", "Item 2"});
				JScrollPane listScroller = new JScrollPane(valueObjectsList);
				dialog.add(listScroller);
				
				JButton closeButton = new JButton("Close");
				closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
				dialog.add(closeButton);
				System.out.println("Ok1");
				
				dialog.setSize(300, 320);
				dialog.setVisible(true);
			}
		}));
		showMenu.add(new JMenuItem(new AbstractAction("ValueTransactions") {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Opening ValueTransactions...");
			}
		}));
		menuBar.add(showMenu);
		
		mainFrame.setJMenuBar(menuBar);
		
		mxGraph graph = new E3Graph();
		Object root = graph.getDefaultParent();
		mxGraphComponent graphComponent = new E3GraphComponent(graph, menuBar);
		
		graph.getModel().beginUpdate();
		try {
			// Playground for custom shapes
		} finally {
			graph.getModel().endUpdate();
		}

		// Create tool pane
		mxGraphComponent tools = new ToolComponent();
		
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
