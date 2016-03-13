package design.main;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;

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

		mxGraph graph = new E3Graph();
		mxGraphComponent graphComponent = new E3GraphComponent(graph);
		Object root = graph.getDefaultParent();

		graph.insertVertex(root, null, "Actor1", 200, 200, 100, 100, "Actor");
		
		// Create tool pane
		mxGraphComponent tools = new ToolComponent();
		
		// Create split view
		JSplitPane mainPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tools, graphComponent);
		mainPane.setResizeWeight(0.1);
		mainFrame.getContentPane().add(mainPane);
		
		// Add menubar
		JMenuBar jmb = new JMenuBar();
		jmb.add(new JMenu("File"));
		jmb.add(new JMenu("Graph"));
		mainFrame.setJMenuBar(jmb);
		
		// Show main screen
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setSize(1024, 768);
		mainFrame.setVisible(true);
	}
	
	public static void main(String[] args) {
		Main t = new Main();
	}
}
