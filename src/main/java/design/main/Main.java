package design.main;

import java.awt.Color;
import java.io.IOException;
import java.util.Hashtable;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.shape.mxStencilShape;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.handler.mxGraphHandler;
import com.mxgraph.swing.handler.mxMovePreview;
import com.mxgraph.swing.view.mxCellStatePreview;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;

public class Main { 
	
	public final JFrame mainFrame = new JFrame("E3fraud editor");
	
	public Main() {
		// Graph test code
		mxGraph graph = new E3Graph();
		mxGraphComponent graphComponent = new E3GraphComponent(graph);
		Object root = graph.getDefaultParent();

		Object actor1 = graph.insertVertex(root, null, "Actor1", 200, 200, 100, 100, "Actor");
		
		// Create tree view
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Root");
		DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("Child 1");
		DefaultMutableTreeNode child2 = new DefaultMutableTreeNode("Child 2");
		JTree tree = new JTree(rootNode);
		rootNode.add(child1);
		rootNode.add(child2);
		
		// Create tool pane
		mxGraphComponent tools = new ToolComponent();
		
		// Create split view
		JSplitPane rightPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, graphComponent, tree);
		JSplitPane mainPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tools, rightPane);
		rightPane.setResizeWeight(0.8);
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
		
		// Set LaF to system
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e){
			System.out.println("Couldn't set Look and Feel");
		}
	}
	
	public static void main(String[] args) {
		Main t = new Main();
	}
}
