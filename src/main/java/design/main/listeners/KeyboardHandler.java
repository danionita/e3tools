package design.main.listeners;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.swing.util.mxGraphActions;
import com.mxgraph.util.mxUndoManager;
import com.mxgraph.view.mxGraph;

import design.main.Info.ValueExchange;
import design.main.Info.ValueExchangeLabel;
import design.main.Main;
import design.main.Utils;

public class KeyboardHandler extends mxKeyboardHandler {

	private mxUndoManager undoManager;

	public KeyboardHandler(mxGraphComponent graphComponent, mxUndoManager undoManager) {
		super(graphComponent);
		
		this.undoManager = undoManager;
	}
	
	@Override
	protected InputMap getInputMap(int condition) {
		InputMap map = super.getInputMap(condition);
		
		if (condition == JComponent.WHEN_FOCUSED) {
			map.put(KeyStroke.getKeyStroke("control Z"), "undo");
			map.put(KeyStroke.getKeyStroke("control Y"), "redo");
		}
		
		return map;
	}

	protected ActionMap createActionMap() {
		ActionMap map = new ActionMap();
		
		// Allows inline editing of cell names
		map.put("edit", mxGraphActions.getEditAction());
		// TODO: toBack and toFront don't have keybindings by default
		map.put("toBack", mxGraphActions.getToBackAction());
		map.put("toFront", mxGraphActions.getToFrontAction());

		// Haven't checked if these keybindings work
		map.put("selectNone", mxGraphActions.getSelectNoneAction());
		map.put("selectNext", mxGraphActions.getSelectNextAction());
		map.put("selectPrevious", mxGraphActions.getSelectPreviousAction());
		map.put("selectParent", mxGraphActions.getSelectParentAction());
		map.put("selectChild", mxGraphActions.getSelectChildAction());

		// Work
		map.put("cut", TransferHandler.getCutAction());
		map.put("copy", TransferHandler.getCopyAction());
		map.put("paste", TransferHandler.getPasteAction());
		map.put("zoomIn", mxGraphActions.getZoomInAction());
		map.put("zoomOut", mxGraphActions.getZoomOutAction());
		
		map.put("delete", new mxGraphActions.DeleteAction("Delete") {
			@Override
			public void actionPerformed(ActionEvent e) {
				mxGraph graph = mxGraphActions.getGraph(e);
				
				if (graph != null) {
					Object selectedCell = graph.getSelectionCell();
					Object value = graph.getModel().getValue(selectedCell);
					if (value instanceof ValueExchangeLabel) {
						graph.getModel().beginUpdate();
						try {
							ValueExchangeLabel veLabelInfo = (ValueExchangeLabel) Utils.base(graph, selectedCell);
							ValueExchange veInfo = (ValueExchange) Utils.base(graph, graph.getModel().getParent(selectedCell));
							
							if (veLabelInfo.isValueObjectLabel) {
								veInfo.valueObjectHidden ^= true;
							} else {
								veInfo.labelHidden ^= true;
							}
							
							graph.getModel().setValue(Main.contextTarget, veInfo);
							Utils.setValueExchangeValueObjectLabelVisibility(graph, graph.getModel().getParent(selectedCell));
							Utils.setValueExchangeNameLabelVisibility(graph, graph.getModel().getParent(selectedCell));
						} finally {
							graph.getModel().endUpdate();
						}
					} else {
						graph.removeCells();
					}
				}
			}
		});
		
		map.put("undo", new AbstractAction("Undo") {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Undoing");
				undoManager.undo();
			}
		});
		
		map.put("redo", new AbstractAction("Redo") {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Redoing");
				undoManager.redo();
			}
		});
		
		return map;
	}

}