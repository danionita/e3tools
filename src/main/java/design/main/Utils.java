package design.main;

import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;

public class Utils {
	public static boolean overlap(mxRectangle a, mxRectangle b) {
		if (a.getX() > b.getX()) {
			mxRectangle dummy = a;
			a = b;
			b = dummy;
		}
		
		if (a.getX() + a.getWidth() > b.getX()) {
			// Horizontal overlap
			if (a.getY() > b.getY()) {
				mxRectangle dummy = a;
				a = b;
				b = dummy;
			}
			
			if (a.getY() + a.getHeight() > b.getY()) {
				// And vertical overlap as well
				return true;
			}
		} else {
			return false;
		}
		
		return false;
	}
	
	/**
	 * Returns true if inner is within outer
	 * @param inner
	 * @param outer
	 * @return
	 */
	public static boolean within(mxRectangle inner, mxRectangle outer) {
		if (inner.getX() < outer.getX())
			return false;
		
		if (inner.getX() + inner.getWidth() > outer.getX() + outer.getWidth())
			return false;
		
		if (inner.getY() < outer.getY())
			return false;
		
		if (inner.getY() + inner.getHeight() > outer.getY() + outer.getHeight())
			return false;
		
		return true;
	}
	
	public static mxRectangle rect(mxGeometry gm) {
		return new mxRectangle(gm.getX(), gm.getY(), gm.getWidth(), gm.getHeight());
	}
	
	/**
	 * Returns true if the given value interface or value port is situated on a top-level actor.
	 * That is, it is not nested.
	 * @param cell
	 * @return
	 */
	public static boolean isToplevelValueInterface(mxGraph graph, mxICell cell) {
		if (cell == null) return false;
		if (cell.isEdge()) return false;
		
		String style = cell.getStyle();
		if (style.startsWith("ValuePort")) {
			return isToplevelValueInterface(graph, cell.getParent());
		} else if (style.equals("ValueInterface")) {
			mxICell parent = cell.getParent();

			if (parent == null) return false;

			return parent.getParent() == graph.getDefaultParent();
		}
		
		return false;
	}
}
