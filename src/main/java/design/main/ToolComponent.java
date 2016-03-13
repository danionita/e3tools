package design.main;

import java.awt.Point;

import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

public class ToolComponent extends mxGraphComponent {
	public final mxGraph graph = getGraph();

	public ToolComponent() {
		super(new mxGraph() {
			/**
			 * This ensures that ValuePorts can't be selected
			 */
			@Override
			public boolean isCellSelectable(Object obj) {
				if (!(obj instanceof mxICell)) return true;
				
				mxICell cell = (mxICell) obj;
				
				String style = cell.getStyle();
				
				if (style == null) return true;
				
				return !cell.getStyle().startsWith("ValuePort");
			}

			/**
			 * Valueports should not have a label, so they are silenced (by returning
			 * an empty string). Anything else should just excert default behaviour.
			 */
			@Override
			public String convertValueToString(Object obj) {
				mxICell cell = (mxICell) obj;
				
				if (cell.getValue() instanceof Info.Base) {
					return ((Info.Base) cell.getValue()).toString();
				}

				return super.convertValueToString(cell);
			}
		});
		
		E3Style.styleGraphComponent(this);

		// To make sure cells are immovable and unresizable and such
		graph.setCellsLocked(true);
		// Disallow edges to be created from nodes
		// This makes dragging nice
		setConnectable(false);
		// Make sure only one node is selected
		graph.getSelectionModel().setSingleSelection(true);
		// Disallow dropping of nodes into the toolbox from the main screen
		setImportEnabled(false);

		Object root = graph.getDefaultParent();
		
		graph.getModel().beginUpdate();
		try {
			// Simple blocks
			Object va = graph.insertVertex(root, null, new Info.ValueActivity(), 20, 20, 120, 120, "ValueActivity");
			Object ac = graph.insertVertex(root, null, new Info.Actor(), 20, 160, 120, 120, "Actor");
			Object ms = graph.insertVertex(root, null, new Info.MarketSegment(), 20, 300, 120, 120, "MarketSegment");
			
			// Value Interface
			mxICell vi = (mxICell) graph.insertVertex(root, null, new Info.ValueInterface(), 20, 440, 20, 50, "ValueInterface");
			E3Graph.addValuePort(graph, vi, true);
			E3Graph.addValuePort(graph, vi, false);

		} finally {
			graph.getModel().endUpdate();
		}
	}
}