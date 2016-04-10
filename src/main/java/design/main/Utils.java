package design.main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;

import design.main.Info.Base;
import design.main.Info.ValueExchange;

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
	
	public static mxGeometry geometry(mxGraph graph, Object obj) {
		return (mxGeometry) graph.getCellGeometry(obj).clone();
	}
	
	public static List<Object> getAllCells(mxGraph graph) {
		return getAllCells(graph, graph.getDefaultParent());
	}
	
	public static List<Object> getAllCells(mxGraph graph, Object parent) {
		List<Object> result = new ArrayList<>(Arrays.asList(mxGraphModel.getChildCells(graph.getModel(), parent, true, true)));
		List<Object> aggr = new ArrayList<>();
		
		for (Object cell : result) {
			aggr.addAll(getAllCells(graph, cell));
		}
		
		result.addAll(aggr);
		
		return result;
	}
	
	/**
	 * Returns a copy of the Info.ValueExchange value of cell
	 * @param cell The cell of which to get the value
	 * @return
	 */
	public static ValueExchange getValueExchange(Object cell) {
		return getValueExchange(cell, true);
	}
	
	/**
	 * Returns the Info.ValueExchange value of the cell.
	 * @param cell The cell of which to get the value
	 * @param clone If true, a copy of the value is returned.
	 * @return
	 */
	public static ValueExchange getValueExchange(Object cell, boolean clone) {
		mxICell actualCell = (mxICell) cell;
		ValueExchange value = (ValueExchange) (actualCell.getValue());
		if (clone) {
			value = (ValueExchange) value.getCopy();
		}
		return value;
	}
}
