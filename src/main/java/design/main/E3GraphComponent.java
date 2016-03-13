package design.main;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.handler.mxCellHandler;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;

public class E3GraphComponent extends mxGraphComponent {
		public E3GraphComponent(mxGraph graph) {
			super(graph);
			
			// I think this is to enable dropping cells
			getConnectionHandler().setCreateTarget(true);
			graph.setAllowDanglingEdges(false);
			getGraphHandler().setRemoveCellsFromParent(false);
			// This makes drag and drop behave properly
			// If you turn these on a drag-shadow that is sometimes offset improperly
			// is shown. Once they fix it in mxGraph we can turn it back on but it's not really needed.
			getGraphHandler().setImagePreview(false);
			getGraphHandler().setCenterPreview(false);
//			setConnectable(false);
			
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
						if (style != null && style.equals("ValueInterface")) {
							mxICell parent = (mxICell) cell.getParent();
							if (parent == graph.getDefaultParent()) {
								cell.removeFromParent();
							}
							
							graph.constrainChild(cell);
						}
					} finally {
						graph.getModel().endUpdate();
					}
					
					// Maybe it's an edge being added
					mxICell source = (mxICell) evt.getProperty("source");
					mxICell target = (mxICell) evt.getProperty("target");
					
					if (source != null && target != null) {
						graph.getModel().setStyle(cell, "ValueExchange");

						boolean sourceIncoming = (Boolean) source.getValue();
						boolean targetIncoming = (Boolean) target.getValue();
						
						System.out.println(sourceIncoming);
						System.out.println(targetIncoming);
						System.out.println(sourceIncoming ^ targetIncoming);
						
						// One should be an incoming value interface, other one should be outgoing
						if (!(sourceIncoming ^ targetIncoming)) {
							cell.removeFromParent();
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