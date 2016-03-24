package design.main;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;

import design.main.Info.Base;
import design.main.Info.ValueInterface;
import design.main.Info.ValueInterface.Side;
import design.main.Info.ValuePort;

// TODO: Check all graph mutations for begin/end!

class E3Graph extends mxGraph {
	/**
	 * Returns true if given cell is a fitting drop target for cells. This means the
	 * drop target should be an actor or a value activity.
	 * TODO: What entities can be dropped in what entities? (Market segment into value activities, etc.)
	 */
	@Override
	public boolean isValidDropTarget(Object cell, Object[] cells) {
		if (!(cell instanceof mxCell)) return false;
		
		mxCell graphCell = (mxCell) cell;
		String style = graphCell.getStyle();
		
		mxICell dropee = (mxICell) cells[0];
		String droppeeStyle = dropee.getStyle();
		
		if (style == null) return false;
		if (droppeeStyle == null)  return false;
		
		if (droppeeStyle.equals("ValueInterface")) {
			return style.equals("Actor") || style.equals("ValueActivity") || style.equals("MarketSegment");
		} else {
			return style.equals("Actor") || style.equals("ValueActivity");
		}
	}

	/**
	 * To enable movement of relative cells within cells.
	 * TODO: Consider value ports (the triangles in value interfaces here)
	 */
	@Override
	public boolean isCellLocked(Object cell) {
		return isCellsLocked();
	}

	/**
	 * Tells mxGraph that when the cell is a ValueInterface it should be "constrained".
	 * The constrainment behaviour is implemented in getCellContainmentArea.
	 */
	@Override
	public boolean isConstrainChild(Object obj) {
		if (obj instanceof mxICell) {
			String style = ((mxICell) obj).getStyle();
			if (style == null) return false;

			return style.equals("ValueInterface") || super.isConstrainChild(obj);
		}
		
		return super.isConstrainChild(obj);
	}

	/**
	 * If the cell is a valueinterface, returns the closest side that is parallel to
	 * the value interface 
	 *
	 */
	@Override
	public mxRectangle getCellContainmentArea(Object obj) {
		if (obj instanceof mxICell) {
			mxICell cell = (mxICell) obj;
			
			if (cell.getParent() == null || cell.getParent() == getDefaultParent()) {
				return super.getCellContainmentArea(obj);
			}
			
			String style = cell.getStyle();
			
			if (style != null && !style.equals("ValueInterface")) {
				return super.getCellContainmentArea(obj);
			}
			
			mxGeometry parentGm = cell.getParent().getGeometry();
			mxGeometry gm = cell.getGeometry();
			
			if (gm.getWidth() > gm.getHeight()) {
				// Top or bottom
				double top = gm.getCenterY();
				double bottom = parentGm.getHeight() - top;
				if (top < bottom) {
					// Top
					return new mxRectangle(
							0,
							gm.getHeight() / -2,
							parentGm.getWidth(),
							gm.getHeight()
					);
				} else {
					// Bottom
					return new mxRectangle(
						0,
						parentGm.getHeight() - gm.getHeight() / 2,
						parentGm.getWidth(),
						gm.getHeight()
					);
				}
			} else {
				// Left or right
				double left = gm.getCenterX();
				double right = parentGm.getWidth() - left;
				if (left < right) {
					// Left
					return new mxRectangle(
							gm.getWidth() / -2,
							0,
							gm.getWidth(),
							parentGm.getHeight()
					);
				} else {
					// Right
					return new mxRectangle(
							parentGm.getWidth() - gm.getWidth() / 2,
							0,
							gm.getWidth(),
							parentGm.getHeight()
					);
				}
			}
		}
		
		return super.getCellContainmentArea(obj);
	}

	/**
	 * This is a bug in mxGraph. TODO: Consider using a local repo, comitting
	 * the changes there and sending a pull request
	 * 
	 * Applies constrainment when the cell is out of bounds of the containment area. This seems to be
	 * different from the default implemented behaviour, altough this seems more appropriate.
	 */
	@Override
	public void constrainChild(Object cell) {
		if (cell != null)
		{
			// Align ValueInterface with closest border
			E3Graph.straightenValueInterface(this, (mxICell) cell);

			// Align the cell properly with the side it's going to be attached on
			mxRectangle area = (isConstrainChild(cell)) ? getCellContainmentArea(cell)
					: getMaximumGraphBounds();
			mxGeometry geo = model.getGeometry(cell);
			
			if (geo != null && area != null)
			{
				// Keeps child within the content area of the parent
				if (!geo.isRelative()
						&& !Utils.within(Utils.rect(geo), area))
				{
					double overlap = getOverlap(cell);

					if (area.getWidth() > 0)
					{
						geo.setX(Math.min(geo.getX(),
								area.getX() + area.getWidth() - (1 - overlap)
										* geo.getWidth()));
					}

					if (area.getHeight() > 0)
					{
						geo.setY(Math.min(geo.getY(),
								area.getY() + area.getHeight() - (1 - overlap)
										* geo.getHeight()));
					}

					geo.setX(Math.max(geo.getX(), area.getX() - geo.getWidth()
							* overlap));
					geo.setY(Math.max(geo.getY(), area.getY() - geo.getHeight()
							* overlap));
				}
			}
		}
	}
	
	/**
	 * Needs to be within a beginUpdate and endUpdate.
	 * @param graph
	 * @param vi
	 */
	public static void straightenValueInterface(mxGraph graph, mxICell vi) {
		if (vi.getStyle() == null || !vi.getStyle().equals("ValueInterface")) return;
		if (vi.getParent() == null) return;
		
		Info.ValueInterface viInfo = (ValueInterface) graph.getModel().getValue(vi);
		
		mxGeometry viGm = vi.getGeometry();
		mxGeometry parentGm = vi.getParent().getGeometry();
		if (parentGm != null) {
			double left, top, right, bottom;
			
			left = viGm.getCenterX();
			right = parentGm.getWidth() - viGm.getCenterX();
			top = viGm.getCenterY();
			bottom = parentGm.getHeight() - viGm.getCenterY();
			
			double min = Collections.min(Arrays.asList(left, top, right, bottom));
			
			if (min == top || min == bottom) {
				viGm.setWidth(30);
				viGm.setHeight(20);
				
				if (min == top) viInfo.side = Side.TOP;
				if (min == bottom) viInfo.side = Side.BOTTOM;
			} else {
				viGm.setWidth(20);
				viGm.setHeight(30);
				
				if (min == left) viInfo.side = Side.LEFT;
				if (min == right) viInfo.side = Side.RIGHT;
			}
		}
		
		List<mxCell> valuePorts = new ArrayList<>();
		mxCell dot = null;
		for (int i = 0; i < vi.getChildCount(); i++) {
			mxCell child = (mxCell) vi.getChildAt(i);
			if (child.getStyle().startsWith("ValuePort")) {
				valuePorts.add(child);
			} else if (child.getStyle().equals("Dot")) {
				dot = child;
			}
		}
		
		if (viInfo.side == Side.TOP || viInfo.side == Side.RIGHT) {
			Collections.reverse(valuePorts);
		}

		if (viGm.getWidth() > viGm.getHeight() && valuePorts.size() > 0) {
			viGm.setWidth(2 * 10
					+ valuePorts.size() * 10 // I'd say the width/height of a port here but 10 works fine
					+ (valuePorts.size() - 1) * 5);
		} else if (valuePorts.size() > 0) {
			viGm.setHeight(2 * 10
					+ valuePorts.size() * 10
					+ (valuePorts.size() - 1) * 5);
		}

		float d = 1 / ((float) (valuePorts.size()));
		int i = 0;
		for (mxICell valuePort : valuePorts) {
			mxGeometry valuePortGm = valuePort.getGeometry();

			if (viGm.getWidth() > viGm.getHeight()) {
				valuePortGm.setY(0.5);
				valuePortGm.setX((i + 0.5) * d);
			} else {
				valuePortGm.setX(0.5);
				valuePortGm.setY((i + 0.5) * d);
			}
			
			rotateValuePort(graph, vi, valuePort);

			i++;
		}
		
		if (dot == null) return;
		mxGeometry dotGm = dot.getGeometry();
		if (viInfo.side == Side.TOP) {
			dotGm.setX(viGm.getWidth() / 2 - E3Style.DOTRADIUS);
			dotGm.setY(viGm.getHeight() - 2 * E3Style.DOTRADIUS);
		} else if (viInfo.side == Side.RIGHT) {
			dotGm.setX(0);
			dotGm.setY(viGm.getHeight() / 2 - E3Style.DOTRADIUS);
		} else if (viInfo.side == Side.BOTTOM) {
			dotGm.setX(viGm.getWidth() / 2 - E3Style.DOTRADIUS);
			dotGm.setY(0);
		} else { // viInfo.side == Side.LEFT || viInfo.side == null
			dotGm.setX(viGm.getWidth() - 2 * E3Style.DOTRADIUS);
			dotGm.setY(viGm.getHeight() / 2 - E3Style.DOTRADIUS);
		}
		
	}

	/**
	 * Adds a valueport to valueinterface cell vi.
	 * This is a static function because it seems separate functionality from the
	 * actual graph class (non e3fraud-graphs can also use this functionality now.
	 * It could be refactored just fine though.
	 * @param graph
	 * @param vi
	 */
	public static void addValuePort(mxGraph graph, mxICell vi, boolean incoming) {
		assert(vi.getStyle().equals("ValueInterface"));
		assert(vi.getValue() instanceof Info.ValueInterface);
		
		graph.getModel().beginUpdate();

		try {
			ValueInterface viInfo = (ValueInterface) vi.getValue();
			ValuePort vpInfo = new ValuePort(incoming);
			mxCell valuePort = (mxCell) graph.insertVertex(vi, null, vpInfo, 0.5, 0.5, 8.66, 10);
			valuePort.setStyle("ValuePort" + vpInfo.getDirection(viInfo));

			mxGeometry vpGm = valuePort.getGeometry();
			vpGm.setRelative(true);
			vpGm.setOffset(new mxPoint(-vpGm.getCenterX(), -vpGm.getCenterY()));
			
			straightenValueInterface(graph, vi);
		} finally {
			graph.getModel().endUpdate();
		}
	}
	
	/**
	 * Rotates a valueport in the right direction (visually) according to the constrainment
	 * area of the valueinterface it is in and whether or not it is incoming
	 * or outgoing (as stored in the user object).
	 * @param graph
	 * @param vi
	 * @param vp
	 */
	public static void rotateValuePort(mxGraph graph, mxICell vi, mxICell vp) {
		mxGeometry viGm = vi.getGeometry();
		ValueInterface viInfo = (ValueInterface) vi.getValue();
		ValuePort vpInfo = (ValuePort) vp.getValue();
		
		graph.getModel().setStyle(vp, "ValuePort" + vpInfo.getDirection(viInfo));
	}
	
	/**
	 * Decides whether or not a cell is selectable. 
	 * In practice, anything is selectable, except value ports.
	 */
	@Override
	public boolean isCellSelectable(Object obj) {
		if (!(obj instanceof mxICell)) return false;
		
		mxICell cell = (mxICell) obj;
		
		String style = cell.getStyle();
		
		if (style == null) return true;
		
		return !style.startsWith("ValuePort")
				&& !style.equals("Dot")
				&& !style.equals("Bar");
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
	
	@Override
	public void cellLabelChanged(Object cell, Object newValue, boolean autoSize) {
		Object oldValue = model.getValue(cell);
		if (oldValue instanceof Info.Base) {
			if (newValue instanceof String) {
				String name = (String) newValue;
				if (oldValue instanceof Info.Actor) {
					Info.Actor actor = (Info.Actor) oldValue;
					actor.name = name;
				} else if (oldValue instanceof Info.MarketSegment) {
					Info.MarketSegment marketSegment = (Info.MarketSegment) oldValue;
					marketSegment.name = name;
				} else if (oldValue instanceof Info.ValueActivity) {
					Info.ValueActivity valueActivity = (Info.ValueActivity) oldValue;
					valueActivity.name = name;
				}
			}
			
			newValue = oldValue;
		}
		super.cellLabelChanged(cell, newValue, autoSize);
	}
	
	/**
	 * Make sure contentless nodes are not editable.
	 */
	@Override
	public boolean isCellEditable(Object obj) {
		if (obj instanceof mxICell) {
			mxICell cell = (mxICell) obj;
			
			// TODO: Convert this to checking the user objects if
			// we need the style to be more fine-grained
			String style = cell.getStyle();
			if (style != null) {
				return !style.startsWith("ValuePort")
						&& !style.equals("ValueInterface")
						&& !style.equals("StartSignal")
						&& !style.equals("EndSignal")
						&& !style.equals("Dot");
			}
		}
		
		return super.isCellEditable(obj);
	}

	@Override
	public Object[] cloneCells(Object[] cells, boolean allowInvalidEdges) {
		Object[] clones = super.cloneCells(cells, allowInvalidEdges);
		
		for ( Object obj : clones) {
			Object val = model.getValue(obj);
			if (val instanceof Info.Base) {
				Info.Base info = (Base) val;
				model.setValue(obj, info.getCopy());
			}
		}
		
		return clones;
	}
	
	@Override
	public boolean isPort(Object obj) {
		String style = model.getStyle(obj);
			
		if (style == null) return false;
		
		return style.equals("Dot");
	}
	
	@Override
	public boolean isCellConnectable(Object cell) {
		String style = model.getStyle(cell);
		if (style == null) return false;
		return style.equals("Dot")
				|| style.startsWith("ValuePort");
	}
}