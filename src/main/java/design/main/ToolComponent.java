package design.main;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

import design.main.Info.Actor;
import design.main.Info.Dot;
import design.main.Info.LogicBase;
import design.main.Info.MarketSegment;
import design.main.Info.Side;
import design.main.Info.ValueActivity;
import design.main.Info.ValueInterface;
import design.main.listeners.ProxySelection;

public class ToolComponent extends mxGraphComponent {
	public final mxGraph graph = getGraph();

	public ToolComponent() {
		super(new mxGraph() {
			/**
			 * To prevent the labels from being selected
			 * @param cell The cell to check
			 * @return True if the cell can be selected
			 */
			@Override
			public boolean isCellSelectable(Object cell) {
				String style = getModel().getStyle(cell);
				return style != null && !style.equals("NameText");
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
			vaInfo.name = "ValueActivity";
			graph.insertVertex(root, null, vaInfo, 10, 20, 90, 90, "ValueActivity");
			
			Actor acInfo = new Actor();
			acInfo.name = "Actor";
			graph.insertVertex(root, null, acInfo, 10, 120, 90, 90, "Actor");
			
			MarketSegment msInfo = new MarketSegment();
			msInfo.name = "MarketSegment";
			graph.insertVertex(root, null, msInfo, 10, 220, 90, 90, "MarketSegment");
			
			// Value Interface
			ValueInterface viInfo = new ValueInterface();
			viInfo.side = Side.LEFT;
			mxICell vi = (mxICell) graph.insertVertex(root, null, viInfo, 80, 320, 20, 50, "ValueInterface");
			E3Graph.addValuePort(graph, vi, true);
			E3Graph.addValuePort(graph, vi, false);
			graph.insertVertex(vi, null, null,
					vi.getGeometry().getWidth() - 2 * E3Style.DOTRADIUS,
					vi.getGeometry().getHeight() / 2 - E3Style.DOTRADIUS,
					E3Style.DOTRADIUS * 2, E3Style.DOTRADIUS * 2,
					"Dot");
			
			// Start signal
			mxCell ss = (mxCell) graph.insertVertex(root, null, null, 70, 380, 30, 30, "StartSignal");
			ss.setConnectable(false);
			mxICell dot = (mxICell) graph.insertVertex(ss, null, null, 0.5, 0.5, 2 * E3Style.DOTRADIUS, 2 * E3Style.DOTRADIUS, "Dot");
			dot.getGeometry().setRelative(true);
			dot.getGeometry().setOffset(new mxPoint(-E3Style.DOTRADIUS, -E3Style.DOTRADIUS));
			
			// End signal
			mxCell es = (mxCell) graph.insertVertex(root, null, null, 55, 420, 45, 45, "EndSignal");
			es.setConnectable(false);
			mxCell dot2 = (mxCell) graph.insertVertex(es, null, null, 0.5, 0.5, 2 * E3Style.DOTRADIUS, 2 * E3Style.DOTRADIUS, "Dot");
			dot2.getGeometry().setRelative(true);
			dot2.getGeometry().setOffset(new mxPoint(-E3Style.DOTRADIUS, -E3Style.DOTRADIUS));
			
			// Or component
			LogicBase lb = new LogicBase();
			lb.isOr = true;
			Object orLogicBase = graph.insertVertex(root, null, lb, 70, 475, 30, 50, "LogicBase");
			Object bar = graph.insertVertex(orLogicBase, null, null, 0.5, 0, 1, 50, "Bar");
			mxGeometry barGm = (mxGeometry) graph.getCellGeometry(bar).clone();
			barGm.setRelative(true);
			graph.getModel().setGeometry(bar, barGm);
			
			Object mainDot = graph.insertVertex(orLogicBase, null, new Dot(true), 0.75, 0.5,
					E3Style.DOTRADIUS * 2, E3Style.DOTRADIUS * 2, "Dot");
			mxGeometry dotGm = (mxGeometry) graph.getCellGeometry(mainDot).clone();
			dotGm.setRelative(true);
			dotGm.setOffset(new mxPoint(-E3Style.DOTRADIUS, -E3Style.DOTRADIUS));
			graph.getModel().setGeometry(mainDot, dotGm);
			
			for (int i = 0; i < 3; i++) {
				E3Graph.addDot(graph, (mxCell) orLogicBase);
			}
			
			// And component
			Object andLogicBase = graph.insertVertex(root, null, new LogicBase(), 70, 535, 30, 50, "LogicBase");
			Object triangle = graph.insertVertex(andLogicBase, null, null, 0.5, 0, 15, 30, "EastTriangle");
			mxGeometry triangleGm = (mxGeometry) graph.getCellGeometry(triangle).clone();
			triangleGm.setRelative(true);
			graph.getModel().setGeometry(triangle, triangleGm);
			
			mainDot = graph.insertVertex(andLogicBase, null, new Dot(true), 0.75, 0.5, 
					E3Style.DOTRADIUS * 2, E3Style.DOTRADIUS * 2, "Dot");
			dotGm = (mxGeometry) graph.getCellGeometry(mainDot).clone();
			dotGm.setRelative(true);
			dotGm.setOffset(new mxPoint(-E3Style.DOTRADIUS, -E3Style.DOTRADIUS));
			graph.getModel().setGeometry(mainDot, dotGm);
			
			for (int i = 0; i < 3; i++) {
				E3Graph.addDot(graph, (mxCell) andLogicBase);
			}
			
			// Add some fancy labels
			graph.insertVertex(root, null, "Value Activity", 120, 20, 100, 100, "NameText");
			graph.insertVertex(root, null, "Actor", 120, 120, 100, 100, "NameText");
			graph.insertVertex(root, null, "Market Segment", 120, 220, 100, 100, "NameText");
			graph.insertVertex(root, null, "Value interface", 120, 320, 100, 50, "NameText");
			graph.insertVertex(root, null, "Start signal", 120, 380, 100, 33, "NameText");
			graph.insertVertex(root, null, "End signal", 120, 420, 100, 45, "NameText");
			graph.insertVertex(root, null, "Or gate", 120, 475, 100, 50, "NameText");
			graph.insertVertex(root, null, "And gate", 120, 535, 100, 50, "NameText");

		} finally {
			graph.getModel().endUpdate();
		}

		// This enables clicking on the easttriangle as well (and gate)
		graph.getSelectionModel().addListener(mxEvent.CHANGE, new ProxySelection(graph));
	}
}