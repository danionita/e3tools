package design.main;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

import design.main.Info.Actor;
import design.main.Info.MarketSegment;
import design.main.Info.ValueActivity;
import design.main.Info.ValueInterface;

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
				
				return !cell.getStyle().startsWith("ValuePort")
						&& !style.equals("Dot");
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
			ValueActivity vaInfo = new ValueActivity();
			vaInfo.name = "ValueActivity1";
			Object va = graph.insertVertex(root, null, vaInfo, 20, 20, 120, 120, "ValueActivity");
			
			Actor acInfo = new Actor();
			acInfo.name = "Actor1";
			Object ac = graph.insertVertex(root, null, acInfo, 20, 160, 120, 120, "Actor");
			
			MarketSegment msInfo = new MarketSegment();
			msInfo.name = "MarketSegment1";
			Object ms = graph.insertVertex(root, null, msInfo, 20, 300, 120, 120, "MarketSegment");
			
			// Value Interface
			ValueInterface viInfo = new ValueInterface();
			viInfo.side = ValueInterface.Side.LEFT;
			mxICell vi = (mxICell) graph.insertVertex(root, null, viInfo, 20, 440, 20, 50, "ValueInterface");
			E3Graph.addValuePort(graph, vi, true);
			E3Graph.addValuePort(graph, vi, false);
			mxICell viDot = (mxICell) graph.insertVertex(vi, null, null,
					vi.getGeometry().getWidth() - 2 * E3Style.DOTRADIUS,
					vi.getGeometry().getHeight() / 2 - E3Style.DOTRADIUS,
					E3Style.DOTRADIUS * 2, E3Style.DOTRADIUS * 2,
					"Dot");
			
			// TODO: Add info's here?
			// TODO: Change to DOTRADIUS
			mxCell ss = (mxCell) graph.insertVertex(root, null, null, 20, 500, 30, 30, "StartSignal");
			ss.setConnectable(false);
			mxICell dot = (mxICell) graph.insertVertex(ss, null, null, 0.5, 0.5, 5, 5, "Dot");
			dot.getGeometry().setRelative(true);
			dot.getGeometry().setOffset(new mxPoint(-2.5, -2.5));
			
			mxCell es = (mxCell) graph.insertVertex(root, null, null, 20, 550, 45, 45, "EndSignal");
			es.setConnectable(false);
			mxCell dot2 = (mxCell) graph.insertVertex(es, null, null, 0.5, 0.5, 5, 5, "Dot");
			dot2.getGeometry().setRelative(true);
			dot2.getGeometry().setOffset(new mxPoint(-2.5, -2.5));
		} finally {
			graph.getModel().endUpdate();
		}
	}
}