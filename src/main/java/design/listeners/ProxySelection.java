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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxGraphSelectionModel;

public class ProxySelection implements mxIEventListener {
	mxGraph graph;
	
	public ProxySelection(mxGraph graph_) {
		graph = graph_;
	}
	
	@Override
	public void invoke(Object sender, mxEventObject evt) {
		mxGraphSelectionModel model = (mxGraphSelectionModel) sender;

		// If there are no cells "added" we have no business here
		if (evt.getProperty("removed") == null) return;

		// Added and removed are switched in mxGraphSelection.mxSelectionChange.execute
		// When fixed, turn these back around
		Collection<Object> added = new ArrayList<>((Collection<Object>) evt.getProperty("removed"));
		// Collection<Object> removed = new ArrayList<>((Collection<Object>) evt.getProperty("added"));
		
		Iterator<Object> it = added.iterator();
		Object triangleParentObj = null;
		while (it.hasNext()) {
			Object obj = it.next();
			String style = graph.getModel().getStyle(obj);

			if (style != null
					&& (style.endsWith("Triangle")
							|| style.startsWith("ValuePort")
							|| style.equals("Bar")
							|| style.equals("Dot")
					)) {
				triangleParentObj = graph.getModel().getParent(obj);
				break;
			}
		}
		
		if (triangleParentObj != null) {
			model.setCell(triangleParentObj);
		}
	}
}