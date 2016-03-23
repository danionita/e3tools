package design.main;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.handler.mxCellHandler;
import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;

import design.main.Info.ValuePort;

public class E3GraphComponent extends mxGraphComponent {
	public E3GraphComponent(mxGraph graph) {
		super(graph);
		
		// Enable delete key et. al.
		// TODO: Only allow useful keybindings to be added
		new mxKeyboardHandler(this);
		
		getConnectionHandler().setCreateTarget(false);
		graph.setAllowDanglingEdges(false);
		graph.setPortsEnabled(false);
		getGraphHandler().setRemoveCellsFromParent(true);
		// This makes drag and drop behave properly
		// If you turn these on a drag-shadow that is sometimes offset improperly
		// is shown. Once they fix it in mxGraph we can turn it back on but it's not really needed.
		getGraphHandler().setImagePreview(false);
		getGraphHandler().setCenterPreview(false);
		
		// Set styling of nodes, background color, etc.
		E3Style.styleGraphComponent(this);
		
		graph.addListener(mxEvent.CELLS_ADDED, new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				Object[] cells = ((Object[]) evt.getProperty("cells"));
				mxCell cell = (mxCell) cells[0];
				
				String style = cell.getStyle();

				graph.getModel().beginUpdate();
				try {
					if (style != null) {
						if (style.equals("ValueInterface")) {
							mxICell parent = (mxICell) cell.getParent();
							if (parent == graph.getDefaultParent()) {
								cell.removeFromParent();
							}
							
							graph.constrainChild(cell);
						} else if (style.equals("StartSignal") || style.equals("EndSignal")) {
							Object parent = graph.getModel().getParent(cell);
							if (parent == graph.getDefaultParent()) {
								cell.removeFromParent();
							}
						}
					}
				} finally {
					graph.getModel().endUpdate();
				}
				
				// Maybe it's an edge being added
				mxICell source = (mxICell) evt.getProperty("source");
				mxICell target = (mxICell) evt.getProperty("target");
				
				if (source != null && target != null) {
					String sourceStyle = source.getStyle();
					String targetStyle = source.getStyle();
					
					System.out.println(sourceStyle + " -> " + targetStyle);
					
					if (sourceStyle.equals("Dot") && sourceStyle.equals(targetStyle)) {
						graph.getModel().setStyle(cell, "ConnectionElement");
					} else if (sourceStyle.startsWith("ValuePort") && targetStyle.startsWith("ValuePort")) {
						graph.getModel().setStyle(cell, "ValueExchange");

						// TODO: Make this NOT throw an exceptino when connecting dot to value port
						boolean sourceIncoming = ((ValuePort) source.getValue()).incoming;
						boolean targetIncoming = ((ValuePort) target.getValue()).incoming;
						
						System.out.println(sourceIncoming);
						System.out.println(targetIncoming);
						System.out.println(sourceIncoming ^ targetIncoming);
						
						// Reverse engineered from the original editor:
						// For two top level actors, one should be incoming and one
						// Should be outgoing. If one of them is nested, anything goes.
						boolean sourceIsTopLevel = Utils.isToplevelValueInterface(graph, source);
						boolean targetIsTopLevel = Utils.isToplevelValueInterface(graph, target);
						
						// One should be an incoming value interface, other one should be outgoing
						if (!(sourceIncoming ^ targetIncoming) && (sourceIsTopLevel && targetIsTopLevel)) {
							cell.removeFromParent();
						}
					}
				}
			}
		});
		
		graph.addListener(mxEvent.RESIZE_CELLS, new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				// TODO: Make cells stick to their previous positions when resizing
				
				Object[] cells = ((Object[]) evt.getProperty("cells"));
				mxCell cell = (mxCell) cells[0];
				
				graph.getModel().beginUpdate();
				try {
					for (int i = 0; i < cell.getChildCount(); i++) {
						// Straighten ports & constrain cell if needed
						graph.constrainChild(cell.getChildAt(i));
					}
				} finally {
					graph.getModel().endUpdate();
				}
			}
		});
		
		graph.addListener(mxEvent.MOVE_CELLS, new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				Object[] cells = ((Object[]) evt.getProperty("cells"));
				mxCell cell = (mxCell) cells[0];

				graph.getModel().beginUpdate();
				try {
					// Straighten ports & constrain cell if needed
					graph.constrainChild(cell);
				} finally {
					graph.getModel().endUpdate();
				}
			}
		});
	}
}