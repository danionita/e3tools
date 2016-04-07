package design.main;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

public class Main { 
	
	public final JFrame mainFrame = new JFrame("E3fraud editor");
	
	public Main() {
		// Set LaF to system
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e){
			System.out.println("Couldn't set Look and Feel to system");
		}

//		// Add context sensitive toolbar
//		JPanel containerPane = new JPanel();
//		mainFrame.getRootPane().setGlassPane(containerPane);
//		JPanel toolbarPane = new JPanel();
//		
//		containerPane.add(toolbarPane);
//		containerPane.setLayout(null);
//		containerPane.setVisible(true);
//		containerPane.setOpaque(false);
//
//		toolbarPane.setBounds(100, 100, 30, 70);
//		toolbarPane.setLayout(new BoxLayout(toolbarPane, BoxLayout.Y_AXIS));
//		toolbarPane.setBackground(new Color(100, 100, 0, 128));
//		toolbarPane.setSize(300, 300);
//		toolbarPane.setLocation(300, 100);
//		toolbarPane.add(new JButton("Add"));
//		toolbarPane.add(new JButton("Remove"));
//		toolbarPane.setVisible(true);
//		toolbarPane.setOpaque(true);
		JPanel toolbarPane = null;

		// Add menubar
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(new JMenu("File"));
		menuBar.add(new JMenu("Graph"));
		mainFrame.setJMenuBar(menuBar);
		
		mxGraph graph = new E3Graph();
		Object root = graph.getDefaultParent();
		mxGraphComponent graphComponent = new E3GraphComponent(graph, menuBar, toolbarPane);
		
		graph.getModel().beginUpdate();
		try {
			// Playground for custom shapes
		} finally {
			graph.getModel().endUpdate();
		}

		// Create tool pane
		mxGraphComponent tools = new ToolComponent();
		
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
