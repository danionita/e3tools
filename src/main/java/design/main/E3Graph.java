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

import design.main.Info.Actor;
import design.main.Info.Base;
import design.main.Info.EndSignal;
import design.main.Info.LogicBase;
import design.main.Info.LogicDot;
import design.main.Info.MarketSegment;
import design.main.Info.Side;
import design.main.Info.SignalDot;
import design.main.Info.StartSignal;
import design.main.Info.ValueActivity;
import design.main.Info.ValueInterface;
import design.main.Info.ValuePort;

public class E3Graph extends mxGraph {
	public final ArrayList<String> valueObjects = new ArrayList<>(
			Arrays.asList("MONEY", "MONEY-SECURED", "SERVICE")
			);
	
	/**
	 * Returns true if given cell is a fitting drop target for cells. This means the
	 * drop target should be an actor or a value activity.
	 * TODO: What entities can be dropped in what entities? (Market segment into value activities, etc.)
	 */
	@Override
	public boolean isValidDropTarget(Object cell, Object[] cells) {
		if (!(cell instanceof mxCell)) return false;

		Base value = Utils.base(this, cell);
		
		Base droppeeValue = Utils.base(this, cells[0]);
		
		if (droppeeValue instanceof ValueInterface
				|| droppeeValue instanceof StartSignal
				|| droppeeValue instanceof EndSignal
				|| droppeeValue instanceof LogicBase) {
			return value instanceof Actor || value instanceof ValueActivity || value instanceof MarketSegment;
		} else if (droppeeValue instanceof MarketSegment){
			return value instanceof Actor;
		} else if (droppeeValue instanceof Actor) {
			return false;
		} else if (droppeeValue instanceof ValueActivity) {
			return value instanceof Actor || value instanceof MarketSegment;
		} else {
			return value instanceof Actor || value instanceof ValueActivity;
		}
	}

	/**
	 * To enable movement of relative cells within cells.
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
		Base value = Utils.base(this, obj);
		return (value instanceof ValueInterface) || super.isConstrainChild(obj);
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
			
			Base value = Utils.base(this, obj);
			if (!(value instanceof ValueInterface)) return super.getCellContainmentArea(obj);
			
			mxGeometry parentGm = Utils.geometry(this, cell.getParent());
			mxGeometry gm = Utils.geometry(this, cell);
			
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
	 * 
	 * The geometry is not cloned because the function that is overridden does not either, and we
	 * should not break the contract.
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
			// Cloning is not needed here! See javadoc.
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
		if (!(Utils.base(graph, vi) instanceof ValueInterface)) return;
		if (vi.getParent() == null) return;
		
		graph.getModel().beginUpdate();
		try {
			Info.ValueInterface viInfo = (ValueInterface) Utils.base(graph, vi);
			
			mxGeometry viGm = Utils.geometry(graph, vi);
			mxGeometry parentGm = Utils.geometry(graph, vi.getParent());
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
				Base value = Utils.base(graph, child);
				if (value instanceof ValuePort) {
					valuePorts.add(child);
				} else if (value instanceof SignalDot) {
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
				mxGeometry valuePortGm = Utils.geometry(graph, valuePort);

				if (viGm.getWidth() > viGm.getHeight()) {
					valuePortGm.setY(0.5);
					valuePortGm.setX((i + 0.5) * d);
				} else {
					valuePortGm.setX(0.5);
					valuePortGm.setY((i + 0.5) * d);
				}
				
				graph.getModel().setGeometry(valuePort, valuePortGm);
				
				// Rotate valueport in the proper (visual) direction
				ValuePort vpInfo = (ValuePort) Utils.base(graph, valuePort);
				graph.getModel().setStyle(valuePort, "ValuePort" + vpInfo.getDirection(viInfo));

				i++;
			}
			
			if (dot != null) {
				mxGeometry dotGm = Utils.geometry(graph, dot);
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
				graph.getModel().setGeometry(dot, dotGm);
			}
			
			graph.getModel().setValue(vi, viInfo);
			graph.getModel().setGeometry(vi, viGm);
		} finally {
			graph.getModel().endUpdate();
		}
	}

	/**
	 * Adds a valueport to valueinterface cell vi.
	 * This is a static function because it seems separate functionality from the
	 * actual graph class (non e3fraud-graphs can also use this functionality now.
	 * It could be refactored just fine though.
	 * @param graph
	 * @param vi
	 * @return The added ValuePort 
	 */
	public static Object addValuePort(mxGraph graph, mxICell vi, boolean incoming) {
		assert(Utils.base(graph, vi) instanceof ValueInterface);
		
		graph.getModel().beginUpdate();

		try {
			ValueInterface viInfo = (ValueInterface) vi.getValue();
			ValuePort vpInfo = new ValuePort(incoming);
			mxCell valuePort = (mxCell) graph.insertVertex(vi, null, vpInfo, 0.5, 0.5, 8.66, 10);
			valuePort.setStyle("ValuePort" + vpInfo.getDirection(viInfo));

			mxGeometry vpGm = Utils.geometry(graph, valuePort);
			vpGm.setRelative(true);
			vpGm.setOffset(new mxPoint(-vpGm.getCenterX(), -vpGm.getCenterY()));
			graph.getModel().setGeometry(valuePort, vpGm);
			
			straightenValueInterface(graph, vi);
			
			return valuePort;
		} finally {
			graph.getModel().endUpdate();
		}
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
		Object oldValue = Utils.base(this, cell);
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
		} else {
			return;
		}

		super.cellLabelChanged(cell, newValue, autoSize);
	}
	
	/**
	 * Make sure contentless nodes are not editable.
	 */
	@Override
	public boolean isCellEditable(Object obj) {
		Base value = Utils.base(this, obj);
		
		return value instanceof Actor
				|| value instanceof MarketSegment
				|| value instanceof ValueActivity
				;
	}

	@Override
	public Object[] cloneCells(Object[] cells, boolean allowInvalidEdges) {
		Object[] clones = super.cloneCells(cells, allowInvalidEdges);
		
		for ( Object obj : clones) {
			if (model.getValue(obj) instanceof Info.Base) {
				Base value = Utils.base(this,  obj);
				value.setSUID(Info.getSUID());
				model.setValue(obj, value);
			}
		}
		
		return clones;
	}
	
	@Override
	public boolean isPort(Object obj) {
		return Utils.isDotValue(Utils.base(this, obj));
	}
	
	@Override
	public boolean isCellConnectable(Object cell) {
		return Utils.isDotValue(Utils.base(this, cell))
				|| Utils.base(this, cell) instanceof ValuePort;
	}
	
	public static void straightenLogicUnit(mxGraph graph, mxCell logicUnit) {
		List<mxICell> dots = new ArrayList<>(); 
		mxICell unitDot = null;
		mxICell bar = null;
		for (int i = 0; i < logicUnit.getChildCount(); i++) {
			mxICell child = logicUnit.getChildAt(i);
			LogicDot dotInfo = (LogicDot) Utils.base(graph, child);
			if (dotInfo == null) {
				bar = child;
				continue;
			}
			if (!dotInfo.isUnit) {
				dots.add(child); 
			} else {
				unitDot = child;  
			}
		}
		
		Side side = ((LogicBase) Utils.base(graph, logicUnit)).direction;
		
		graph.getModel().beginUpdate();
		try {
			for (int i = 0; i < dots.size(); i++) {
				mxICell dot = dots.get(i);
				mxGeometry gm = (mxGeometry) graph.getCellGeometry(dot).clone();
				double horizontal = (i + 0.5) / (double) dots.size();
				
				if (side == Side.TOP) {
					gm.setX(horizontal);
					gm.setY(1);
				} else if (side == Side.RIGHT) {
					gm.setX(0);
					gm.setY(horizontal);
				} else if (side == Side.BOTTOM) {
					gm.setX(horizontal);
					gm.setY(0);
				} else if (side == Side.LEFT) {
					gm.setY(horizontal);
					gm.setX(1);
				} 				

				graph.getModel().setGeometry(dot,  gm);
			}
			
			mxGeometry gm = Utils.geometry(graph, unitDot); 
			gm.setX(0.5);
			gm.setY(0.5);
			
			mxGeometry barGm = Utils.geometry(graph, bar);
			barGm.setX(0.5);
			barGm.setY(0.5);
			barGm.setWidth(1);
			barGm.setHeight(1);

			// TODO: Strictly speaking this should also be a Base instanceof Triangle test of some sort
			// But since it's only a graphical element I'm not sure if we should also give it a value
			// And do an instanceof check here; after all, this one element really depends on its style
			boolean isTriangle = graph.getModel().getStyle(bar).endsWith("Triangle");
			double width = logicUnit.getGeometry().getWidth();
			double height = logicUnit.getGeometry().getHeight();
			
			if (side == Side.TOP) {
				gm.setY(0);
				barGm.setX(0);
				barGm.setWidth(width);

				if (isTriangle) {
					barGm.setY(0);
					barGm.setHeight(height / 2);
					graph.getModel().setStyle(bar, new String("NorthTriangle"));
				}
			} else if (side == Side.RIGHT) {
				gm.setX(1);
				barGm.setY(0);
				barGm.setHeight(height);

				if (isTriangle) {
					barGm.setWidth(width / 2);
					graph.getModel().setStyle(bar, new String("EastTriangle"));
				}
			} else if (side == Side.BOTTOM) {
				gm.setY(1);
				barGm.setX(0);
				barGm.setWidth(width);
				if (isTriangle) {
					barGm.setHeight(height / 2);
					graph.getModel().setStyle(bar, new String("SouthTriangle"));
				}
			} else if (side == Side.LEFT){
				gm.setX(0);
				barGm.setY(0);
				barGm.setHeight(height);
				if (isTriangle) {
					barGm.setX(0);
					barGm.setWidth(width / 2);
					graph.getModel().setStyle(bar, new String("WestTriangle"));
				}
			}
			
			graph.getModel().setGeometry(unitDot, gm);
			graph.getModel().setGeometry(bar, barGm);
		} finally {
			graph.getModel().endUpdate();
		}
	}
	
	public static void addLogicDot(mxGraph graph, mxCell logicUnit) {
		graph.getModel().beginUpdate();
		try {
			Object obj = graph.insertVertex(logicUnit, null, new LogicDot(false), 0, 0, 
					E3Style.DOTRADIUS * 2, E3Style.DOTRADIUS * 2, "Dot");
			mxGeometry gm = (mxGeometry) graph.getCellGeometry(obj).clone();
			gm.setRelative(true);
			gm.setOffset(new mxPoint(-E3Style.DOTRADIUS, -E3Style.DOTRADIUS));
			graph.getModel().setGeometry(obj, gm);
			
			straightenLogicUnit(graph, logicUnit);
		} finally {
			graph.getModel().endUpdate();
		}
	}
	public Object addActor(double x, double y) {
		Object ac = addCell(Main.globalTools.clone(Main.globalTools.actor));
		mxGeometry geom = Utils.geometry(this, ac);
		geom.setX(x);
		geom.setY(y);
		model.setGeometry(ac, geom);
		
		return ac;
	}
	
	public Object addValueInterface(Object parent, double x, double y) {
		Object vi = Main.globalTools.clone(Main.globalTools.valueInterface);
		mxGeometry gm = ((mxCell) vi).getGeometry();
		gm.setX(x);
		gm.setY(y);
		this.addCell(vi, parent);
		
		return vi;
	}
	
	public Object addStartSignal(Object parent, double x, double y) {
		Object ss = Main.globalTools.clone(Main.globalTools.startSignal);
		mxGeometry gm = ((mxCell) ss).getGeometry();
		gm.setX(x);
		gm.setY(y);
		this.addCell(ss, parent);
		
		return ss;
	}
	
	public Object addEndSignal(Object parent, double x, double y) {
		Object es = Main.globalTools.clone(Main.globalTools.endSignal);
		mxGeometry gm = ((mxCell) es).getGeometry();
		gm.setX(x);
		gm.setY(y);
		this.addCell(es, parent);
		
		return es;
	}
	
	public Object connectVE(Object start, Object end) {
		Base startInfo = Utils.base(this, start);
		Base endInfo = Utils.base(this, end);
		
		if (startInfo instanceof ValueInterface 
				&& endInfo instanceof ValueInterface) {
			List<Object> valuePortsStart = Utils.getChildrenWithValue(this, start, ValuePort.class);
			List<Object> valuePortsEnd = Utils.getChildrenWithValue(this, end, ValuePort.class);
			
			Object portStart = null;
			Object portEnd = null;
			
			for (Object port : valuePortsStart) {
				ValuePort vpInfo = (ValuePort) Utils.base(this, port);
				if (model.getEdgeCount(port) == 0 && !vpInfo.incoming) {
					portStart = port;
					break;
				}
			}
			
			for (Object port : valuePortsEnd) {
				ValuePort vpInfo = (ValuePort) Utils.base(this, port);
				if (model.getEdgeCount(port) == 0 && vpInfo.incoming) {
					portEnd = port;
					break;
				}
			}
			
			assert (portStart != null);
			assert (portEnd != null);
			
			return this.insertEdge(
					getDefaultParent(),
					null,
					"",
					portEnd,
					portStart
					);
		}

		return null;
	}

	public Object connectCE(Object start, Object end) {
		Base startInfo = Utils.base(this, start);
		Base endInfo = Utils.base(this, end);
		
		Object startDot = Utils.getChildrenWithValue(this, start, SignalDot.class).get(0);
		Object endDot = Utils.getChildrenWithValue(this, end, SignalDot.class).get(0);
		
		return this.insertEdge(getDefaultParent(), null, "", startDot, endDot);
	}	
}