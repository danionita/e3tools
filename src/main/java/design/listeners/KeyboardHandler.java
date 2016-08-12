/*******************************************************************************
 * Copyright (C) 2016 Bob Rubbens
 *  
 *  
 * This file is part of e3tool.
 *  
 * e3tool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * e3tool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with e3tool.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package design.listeners;

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
		
		map.put("delete", new mxGraphActions.DeleteAction("Delete"));
//		map.put("delete", new mxGraphActions.DeleteAction("Delete") {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				mxGraph graph = mxGraphActions.getGraph(e);
//				
//				if (graph != null) {
//					Object selectedCell = graph.getSelectionCell();
//					Object value = graph.getModel().getValue(selectedCell);
//					graph.removeCells();
//				}
//			}
//		});
		
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
