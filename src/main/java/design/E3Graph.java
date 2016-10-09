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
package design;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.w3c.dom.Document;

import com.mxgraph.io.mxCodec;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;

import design.Utils.GraphDelta;
import design.Utils.IDReplacer;
import design.info.Actor;
import design.info.Base;
import design.info.ConnectionElement;
import design.info.EndSignal;
import design.info.Info.Side;
import design.info.LogicBase;
import design.info.LogicDot;
import design.info.MarketSegment;
import design.info.Note;
import design.info.SignalDot;
import design.info.StartSignal;
import design.info.ValueActivity;
import design.info.ValueExchange;
import design.info.ValueInterface;
import design.info.ValuePort;

// TODO: Use mxRubberband for multi select actors and stuff (see Validiation.java in mxGraph examples)
public class E3Graph extends mxGraph implements Serializable{
    public static int newGraphCounter = 1;

	public final ArrayList<String> valueObjects = new ArrayList<>(
			Arrays.asList("MONEY", "MONEY-SECURED", "SERVICE")
			);
	public boolean isFraud;
	public GraphDelta delta;
	public String title = "";
	public File file;
	public E3Style style;
	
	public E3Graph(E3Style style, boolean isFraud) {
		this(style, isFraud, null);
	}
	
	public E3Graph(E3Style style, boolean isFraud, String title) {
		this.style = style;
		this.isFraud = isFraud;
		this.title = title;
		
		if (this.title == null) {
			if (isFraud) {
				this.title = "Fraud model " + newGraphCounter++;
			} else {
				this.title = "Value model " + newGraphCounter++;
			}
		}
		
		addStandardEventListeners();
	}

	public E3Graph(E3Graph original, boolean duplicate) {
		style = original.style;

		isFraud = original.isFraud;
		if (duplicate) {title = "Copy of " + original.title;}
                else{title = original.title;}
		
		getModel().beginUpdate();
		try {
			addCells(original.cloneCellsKeepSUIDs(original.getChildCells(original.getDefaultParent())));
			valueObjects.clear();
			valueObjects.addAll(original.valueObjects);
		} finally {
			getModel().endUpdate();
		}
		
		addStandardEventListeners();
	}
	
	public E3Graph(E3Graph original, GraphDelta delta) {
		this(original, false);
		
		this.isFraud = true;
		this.title = "Fraud instance of " + original.title;
		
		// TODO: Remove possible fraud changes in original
		// This line will have to merge deltas or something
		this.delta = delta;
		
		getModel().beginUpdate();
		try {
			for (long id : delta.nonOccurringTransactions) {
				Object ve = getCellFromId(id);
				setValueExchangeNonOcurring(ve, true);
				
				//System.out.println("Set to non-occurring: " + id);
			}
                        
                        int i=0;
			for (long[] valueInterfaces : delta.hiddenTransactions) {
				Object leftValueInterface = getCellFromId(valueInterfaces[0]);
				Object rightValueInterface = getCellFromId(valueInterfaces[1]);


				Object leftVP = addValuePort(this, (mxICell) leftValueInterface, false);
				Object rightVP = addValuePort(this, (mxICell) rightValueInterface, true);

				Object newVE = connectVP(leftVP, rightVP);
                                

				setValueExchangeHidden(newVE, true);
				setFormula(newVE, "VALUATION", String.valueOf(delta.hiddenTransferValues.get(i)));
				//System.out.println("Added hidden transaction: " + valueInterfaces[0]);
                                i++;
			}
			
			for (long id : delta.colludedActors) {
				Object ac = getCellFromId(id);
				setColludingActor(ac, true);
				
				//System.out.println("Colluding: " + id);
			} 
		} finally {
			getModel().endUpdate();
		}
	}
	
	private void addStandardEventListeners() {
		mxGraph graph = this;
		
		addListener(mxEvent.CELLS_ADDED, new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				Object[] cells = ((Object[]) evt.getProperty("cells"));
				mxCell cell = (mxCell) cells[0];
				
				graph.getModel().beginUpdate();
				try {
					Base value = Utils.base(graph, cell);
					
					if (value instanceof ValueInterface) {
						mxICell parent = (mxICell) cell.getParent();
						if (parent == graph.getDefaultParent()) {
							graph.removeCells(new Object[]{cell});
						}
						
						graph.constrainChild(cell);
					} else if (value instanceof StartSignal || value instanceof EndSignal) {
						Object parent = graph.getModel().getParent(cell);
						if (parent == graph.getDefaultParent()) {
							graph.removeCells(new Object[]{cell});
						}
					} else if (value instanceof LogicBase) {
						Object parent = graph.getModel().getParent(cell);
						if (parent == graph.getDefaultParent()) {
							graph.removeCells(new Object[]{cell});
						}
					}
				} finally {
					graph.getModel().endUpdate();
				}
				
				// Maybe it's an edge being added
				mxICell source = (mxICell) evt.getProperty("source");
				mxICell target = (mxICell) evt.getProperty("target");
				
				if (source != null && target != null) {
					Base sourceValue = Utils.base(graph, source);
					Base targetValue = Utils.base(graph, target);

					if (Utils.isDotValue(sourceValue) && Utils.isDotValue(targetValue)) {
						graph.getModel().beginUpdate();
						try {
							graph.getModel().setStyle(cell, "ConnectionElement");
							ConnectionElement value = new ConnectionElement(Utils.getUnusedID(E3Graph.this));
							value.name = "ConnectionElement" + value.SUID;
							graph.getModel().setValue(cell, value);
						} finally {
							graph.getModel().endUpdate();
						}
					} else if (sourceValue instanceof ValuePort && targetValue instanceof ValuePort) {
						graph.getModel().beginUpdate();
						try {
							// Set ValueExchange edge properties
							graph.getModel().setStyle(cell, new String("ValueExchange"));
							ValueExchange value = new ValueExchange(Utils.getUnusedID(E3Graph.this));
							value.name = "ValueExchange" + value.SUID;
							graph.getModel().setValue(cell, value);
						} finally {
							graph.getModel().endUpdate();
						}
					} 
				}
			}
		});

		// When an object is resized
		graph.addListener(mxEvent.RESIZE_CELLS, new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				// We ignore all other hypothetical cells
				Object[] cells = ((Object[]) evt.getProperty("cells"));
				mxCell cell = (mxCell) cells[0];
				
				// If it's a logic unit we rearrange its logic dots appropriately
				if (Utils.base(graph, cell) instanceof LogicBase) {
					E3Graph.straightenLogicUnit(graph, cell);
				} else {
					// Otherwise we check for all its children if they need to be constrained
					graph.getModel().beginUpdate();
					try {
						for (int i = 0; i < cell.getChildCount(); i++) {
							Object child = cell.getChildAt(i);
							Base info = Utils.base(E3Graph.this, child);
							
							// If something is a value interface, make sure it sticks
							// to the side it was assigned to.
							if (info instanceof ValueInterface) {
								mxGeometry parentGeom = getModel().getGeometry(cell);
								ValueInterface viInfo = (ValueInterface) info;
								mxGeometry geom = Utils.geometry(E3Graph.this, child);
								
								switch (viInfo.side) {
								case LEFT:
									geom.setX(-geom.getWidth() / 2);
									break;
								case TOP:
									geom.setY(-geom.getHeight() / 2);
									break;
								case RIGHT:
									geom.setX(parentGeom.getWidth() - geom.getWidth() / 2);
									break;
								case BOTTOM:
									geom.setY(parentGeom.getHeight() - geom.getHeight() / 2);
								}
								
								getModel().setGeometry(child, geom);
							}
							
							// Straighten ports & constrain cell if needed
							graph.constrainChild(cell.getChildAt(i));
						}
					} finally {
						graph.getModel().endUpdate();
					}
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
	
	/**
	 * Returns true if given cell is a fitting drop target for cells.
	 * Hierarchy:
	 * - Actor can contain market segments and actors, or ONLY value activities
	 * - Market segment can contain value activities
	 * - Value activities cannot contain anything
	 * As of 2016-8-16. This function is only called if a node is dropped
	 * inside another node. The top level filtering (that makes sure
	 * you cannot have a top level start signal) happens in
	 * {@link #addStandardEventListeners()}.
	 */
	@Override
	public boolean isValidDropTarget(Object cell, Object[] cells) {
		if (!(cell instanceof mxCell)) return false;

		Base value = Utils.base(this, cell);
		
		Base droppeeValue = Utils.base(this, cells[0]);
		
		Function<Object, Boolean> isEmptyOrContainsNoValueActivities = obj -> {
			List<Object> objChildren = Utils.getChildren(this, cell);

			objChildren = objChildren.stream().filter(child -> {
				Base info = (Base) getModel().getValue(child);
				return info instanceof ValueActivity
						|| info instanceof Actor
						|| info instanceof MarketSegment;
			}).collect(Collectors.toList());

			boolean allValueActivities = objChildren.stream().anyMatch(o -> getModel().getValue(o) instanceof ValueActivity);
			return objChildren.size() == 0 || !allValueActivities;
		};
		
		Function<Object, Boolean> isEmptyOrContainsOnlyValueActivities = obj -> {
			List<Object> objChildren = Utils.getChildren(this, cell);

			objChildren = objChildren.stream().filter(child -> {
				Base info = (Base) getModel().getValue(child);
				return info instanceof ValueActivity
						|| info instanceof Actor
						|| info instanceof MarketSegment;
			}).collect(Collectors.toList());

			boolean allValueActivities = objChildren.stream().allMatch(o -> getModel().getValue(o) instanceof ValueActivity);
			return objChildren.size() == 0 || allValueActivities;
		};
		
		if (droppeeValue instanceof ValueInterface
				|| droppeeValue instanceof StartSignal
				|| droppeeValue instanceof EndSignal
				|| droppeeValue instanceof LogicBase) {
			return value instanceof Actor || value instanceof ValueActivity || value instanceof MarketSegment;
		} else if (droppeeValue instanceof MarketSegment){
			if (value instanceof Actor) {
				// Only allow a marketsegment to be a child of an actor
				// When the actor is empty or only contains other things than value interfaces
				return isEmptyOrContainsNoValueActivities.apply(cell);
			}

			return false;
		} else if (droppeeValue instanceof Actor) {
			if (value instanceof Actor) {
				return isEmptyOrContainsNoValueActivities.apply(cell);
			}

			return false;
		} else if (droppeeValue instanceof ValueActivity) {
			return isEmptyOrContainsOnlyValueActivities.apply(cell);
		} else if (droppeeValue instanceof Note) {
			return false;
		} else {
			// It's a start signal, and port, or something.
			return value instanceof Actor 
					|| value instanceof ValueActivity 
					|| value instanceof MarketSegment;
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
			ValueInterface viInfo = (ValueInterface) Utils.base(graph, vi);
			
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
			ValuePort vpInfo = new ValuePort(Utils.getUnusedID(graph), incoming);
			mxCell valuePort = (mxCell) graph.insertVertex(vi, null, vpInfo, 0.5, 0.5, 8.66, 10);
			valuePort.setStyle("ValuePort" + vpInfo.getDirection(viInfo));
			
			// If the vi is on top or the side, move the vp to the beginning of the mxCell's (internal)
			// child array. This is to make sure that it looks nice when ports are added (so the edges
			// are straight instead of crossed between linked vi's)
			if (viInfo.side == Side.TOP || viInfo.side == Side.RIGHT) {
				vi.remove(valuePort);
				vi.insert(valuePort, 0);
			}
			
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
	 * Returns the toString() method of Info objects. These override their
	 * toString() methods to return a nice name for a label. Everything else should
	 * use the default behaviour.
	 */
	@Override
	public String convertValueToString(Object obj) {
		mxICell cell = (mxICell) obj;
		
		if (cell.getValue() instanceof Base) {
			return ((Base) cell.getValue()).toString();
		}

		return super.convertValueToString(cell);
	}
	
	/**
	 * If it is a base, and the new label is a string, the base is cloned and
	 * the name is set to the given string. Everything else does the default
	 * behaviour.
	 */
	@Override
	public void cellLabelChanged(Object cell, Object newValue, boolean autoSize) {
		Object oldValue = Utils.base(this, cell);

		if (oldValue instanceof Base) {
			if (newValue instanceof String) {
				String name = (String) newValue;
				Base base = ((Base) oldValue).getCopy();
				
				base.name = name;
				
				newValue = base;
			}			
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
				|| value instanceof ValueExchange
				|| value instanceof ConnectionElement
				|| value instanceof Note
				;
	}

	@Override
	public Object[] cloneCells(Object[] cells, boolean allowInvalidEdges) {
		System.out.println("Cloning cells in e3graph");
		
		Object[] clones = super.cloneCells(cells, allowInvalidEdges);
		
		new IDReplacer(this).renewBases(clones);

		return clones;
	}
	
	public Object[] cloneCellsKeepSUIDs(Object[] cells) {
		Object[] clones = super.cloneCells(cells, true);

		class H {
			public void renewBases(mxCell cell) {
				if (cell.getValue() instanceof Base) {
					cell.setValue(((Base) cell.getValue()).getCopy()); 
				}
				
				for (int i = 0; i < cell.getChildCount(); i++) {
					renewBases((mxCell) cell.getChildAt(i));
				}
			}
		} H h = new H();
		
		for ( Object obj : clones) {
			if (model.getValue(obj) instanceof Base) {
				mxCell cell = (mxCell) obj;
				h.renewBases(cell);
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
			
			// TODO: Do something with E3Style here (below mostly)
			
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
			Object obj = graph.insertVertex(
					logicUnit,
					null,
					new LogicDot(Utils.getUnusedID(graph), false),
					0,
					0, 
					E3Style.DOTRADIUS * 2,
					E3Style.DOTRADIUS * 2,
					"Dot"
					);
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
		getModel().beginUpdate();
		try {
			Object ac = addCell(Main.globalTools.clone(Main.globalTools.actor, this));
			mxGeometry geom = Utils.geometry(this, ac);
			geom.setX(x);
			geom.setY(y);
			model.setGeometry(ac, geom);
			return ac;
		} finally {
			getModel().endUpdate();
		}
	}
	
	public Object addValueInterface(Object parent, double x, double y) {
		getModel().beginUpdate();
		try {
			Object vi = Main.globalTools.clone(Main.globalTools.valueInterface, this);
			mxGeometry gm = ((mxCell) vi).getGeometry();
			gm.setX(x);
			gm.setY(y);
			this.addCell(vi, parent);
			return vi;
		} finally {
			getModel().endUpdate();
		}
	}
	
	public Object addStartSignal(Object parent, double x, double y) {
		Object ss = Main.globalTools.clone(Main.globalTools.startSignal, this);
		mxGeometry gm = ((mxCell) ss).getGeometry();
		gm.setX(x);
		gm.setY(y);

		getModel().beginUpdate();
		try {
			this.addCell(ss, parent);
		} finally {
			getModel().endUpdate();
		}
		
		return ss;
	}
	
	public Object addEndSignal(Object parent, double x, double y) {
		Object es = Main.globalTools.clone(Main.globalTools.endSignal, this);
		mxGeometry gm = ((mxCell) es).getGeometry();
		gm.setX(x);
		gm.setY(y);
		
		getModel().beginUpdate();
		try {
			this.addCell(es, parent);
		} finally {
			getModel().endUpdate();
		}
		
		return es;
	}
	
	/**
	 * Connects the first outgoing valueport in start with the first incoming value port
	 * on end.
	 * @param start Start value interface
	 * @param end End value interface
	 * @return
	 */
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
			
			getModel().beginUpdate();
			try {
				return this.insertEdge(getDefaultParent(), null, "", portEnd, portStart);
			} finally {
				getModel().endUpdate();
			}
		}

		throw new IllegalArgumentException("Start or end object is not a value interface");
	}
	
	public Object connectVP(Object start, Object end) {
		Base startInfo = Utils.base(this, start);
		Base endInfo = Utils.base(this, end);
		
		if (startInfo instanceof ValuePort && endInfo instanceof ValuePort) {
			getModel().beginUpdate();
			try {
				return this.insertEdge(getDefaultParent(), null, "", start, end);
			} finally {
				getModel().endUpdate();
			}
		}
		
		throw new IllegalArgumentException("Start or end cells are not value ports");
	}

	/**
	 * Connects pairs of signaldots, or pairs of start signal/end signal/value interfaces
	 * So the allowed combinations are:
	 * (SignalDot/LogicDot) -> (SignalDot/LogicDot)
	 * (start signal/end signal/value interface) -> (start signal/end signal/value interface)
	 * @param start
	 * @param end
	 * @return
	 */
	public Object connectCE(Object start, Object end) {
		Base startInfo = Utils.base(this, start);
		Base endInfo = Utils.base(this, end);
		
		if ((startInfo instanceof SignalDot || startInfo instanceof LogicDot)
				&& (endInfo instanceof SignalDot || endInfo instanceof LogicDot)){
			getModel().beginUpdate();
			try {
				return this.insertEdge(getDefaultParent(), null, "", start, end);
			} finally {
				getModel().endUpdate();
			}
		}
		
		//System.out.println(startInfo.getClass().getSimpleName());
		//System.out.println(endInfo.getClass().getSimpleName());
		
		Object startDot = Utils.getChildrenWithValue(this, start, SignalDot.class).get(0);
		Object endDot = Utils.getChildrenWithValue(this, end, SignalDot.class).get(0);
		
		getModel().beginUpdate();
		try {
			return this.insertEdge(getDefaultParent(), null, "", startDot, endDot);
		} finally {
			getModel().endUpdate();
		}
	}	
	
	public void setColludingActor(Object ac, boolean b) {
		Actor acInfo = (Actor) Utils.base(this, ac);
		acInfo.colluded = b;

		getModel().beginUpdate();
		try {
			getModel().setValue(ac, acInfo);

			if (acInfo.colluded) {
				getModel().setStyle(ac, "ColludedActor");
			} else {
				getModel().setStyle(ac, "Actor");
			}
		} finally {
			getModel().endUpdate();
		}
	}
	
	public void setValueExchangeNonOcurring(Object ve, boolean on) {
		ValueExchange veInfo = (ValueExchange) Utils.base(this, ve);
		veInfo.setNonOccurring(on);
		
		getModel().beginUpdate();
		try {
			getModel().setValue(ve, veInfo);
			
			if (on) {
				getModel().setStyle(ve, new String("NonOccurringValueExchange"));
			} else {
				getModel().setStyle(ve, new String("ValueExchange"));
			}
		} finally {
			getModel().endUpdate();
		}
	}
	
	public void setValueExchangeHidden(Object ve, boolean on) {
		ValueExchange veInfo = (ValueExchange) Utils.base(this, ve);
		veInfo.setHidden(on);
		
		getModel().beginUpdate();
		try {
			getModel().setValue(ve, veInfo);
			
			if (on) {
				getModel().setStyle(ve, new String("HiddenValueExchange"));
			} else {
				getModel().setStyle(ve, new String("ValueExchange"));
			}
		} finally {
			getModel().endUpdate();
		}
	}
	
	public Object getCellFromId(long id) {
		for (Object cell : Utils.getAllCells(this)) {
			Object val = getModel().getValue(cell);
			if (val instanceof Base) {
				Base info = (Base) val;
				if (info.SUID == id) {
					return cell;
				}
			}
		}
		
		throw new IllegalArgumentException("Id " + id + " does not exist");
	}

	public void setValueObject(Object ve, String obj) {
		ValueExchange veInfo = (ValueExchange) Utils.base(this, ve);
		veInfo.valueObject = obj;
		
		getModel().beginUpdate();
		try {
			getModel().setValue(ve, veInfo);
//			Utils.updateValueExchangeValueObjectLabel(this, ve);
		} finally {
			getModel().endUpdate();
		}
	}

	public void setName(Object l, String name) {
		Base info = Utils.base(this, l);
		info.name = name;
		
		getModel().beginUpdate();
		try {
			getModel().setValue(l, info);
		} finally {
			getModel().endUpdate();
		}
	}

	public void setFormula(Object subject, String formula, String value) {
		Base info = Utils.base(this, subject);
		info.formulas.put(formula, value);
				
		getModel().beginUpdate();
		try {
			getModel().setValue(subject, info);
		} finally {
			getModel().endUpdate();
		}
	}
	
	public void setFormulaOnEdgeAndPorts(Object valueExchange, String formula, String value) {
		Object vpLeft = getModel().getTerminal(valueExchange, true);
		Object vpRight = getModel().getTerminal(valueExchange, false);
		
		getModel().beginUpdate();
		try {
			setFormula(valueExchange, formula, value);
			setFormula(vpLeft, formula, value);
			setFormula(vpRight, formula, value);
		} finally {
			getModel().endUpdate();
		}
	}

	public void setCellSize(Object userA, double x, double y) {
		mxGeometry gm = Utils.geometry(this, userA);
		gm.setWidth(x);
		gm.setHeight(y);
		
		getModel().beginUpdate();
		try {
			getModel().setGeometry(userA, gm);
		} finally {
			getModel().endUpdate();
		}
	}

	public void setValueExchangeLabel(Object ve, String name) {
		getModel().beginUpdate();
		try {
			setName(ve, name);
		} finally {
			getModel().endUpdate();
		}
	}

	public void setValueExchangeLabelVisible(Object ve, boolean b) {
		getModel().beginUpdate();
		try {
			ValueExchange veInfo = (ValueExchange) Utils.base(this, ve);
			veInfo.labelHidden = !b;
			getModel().setValue(ve, veInfo);
		} finally {
			getModel().endUpdate();
		}
	}

	public void setValueExchangeLabelPosition(Object ve, double x, double y) {
		mxGeometry gm = Utils.geometry(this, ve);
		gm.setX(x);
		gm.setY(y);
		
		getModel().beginUpdate();
		try {
			getModel().setGeometry(ve, gm);
		} finally {
			getModel().endUpdate();
		}
	}

	public Object addAnd(Object parent, int x, int y, Side targetDir) {
		getModel().beginUpdate();
		try {
			Object logic = Main.globalTools.clone(Main.globalTools.andGate, this);
			mxGeometry gm = ((mxCell) logic).getGeometry();
			gm.setX(x);
			gm.setY(y);
			
			addCell(logic, parent);
			
			Side currDir = Side.RIGHT;
			while (currDir != targetDir) {
				currDir = currDir.rotateRight();
				rotateLogicRight(logic);
			}
			
			return logic;
		} finally {
			getModel().endUpdate();
		}
	}

	public Object addOr(Object parent, int x, int y, Side targetDir) {
		getModel().beginUpdate();
		try {
			Object logic = Main.globalTools.clone(Main.globalTools.orGate, this);
			mxGeometry gm = ((mxCell) logic).getGeometry();
			gm.setX(x);
			gm.setY(y);
			
			addCell(logic, parent);
			
			Side currDir = Side.RIGHT;
			while (currDir != targetDir) {
				currDir = currDir.rotateRight();
				rotateLogicRight(logic);
			}
			
			return logic;
		} finally {
			getModel().endUpdate();
		}
	}
	
	public Object rotateLogicRight(Object logic) {
		Base value = Utils.base(this, logic);
		
		if (value instanceof LogicDot) {
			logic = getModel().getParent(logic);
		}

		LogicBase lb = (LogicBase) Utils.base(this, logic);
		lb.direction = lb.direction.rotateRight();

		mxGeometry gm = (mxGeometry) getCellGeometry(logic).clone();
		double width = gm.getWidth();
		double height = gm.getHeight();
		gm.setWidth(height);
		gm.setHeight(width);
		getModel().beginUpdate();
		try {
			getModel().setGeometry(logic, gm);
			getModel().setValue(logic, lb);
		} finally {
			getModel().endUpdate();
		}

		E3Graph.straightenLogicUnit(this, (mxCell) logic);
		
		return logic;
	}
	
	/**
	 * Connects something with a signal dot to something with a logic dot
	 * @param signal The thing with a signal dot
	 * @param logic The thing with a logic dot
	 * @param unit Whether or not to hook the signal to the unit dot or a normal logic dot
	 * @return The created edge
	 */
	public Object connectSignalToLogic(Object signal, Object logic, boolean unit) {
		Object signalDot = Utils.getChildrenWithValue(this, signal, SignalDot.class).get(0);
		List<Object> logicDots = Utils.getChildrenWithValue(this, logic, LogicDot.class);
		logicDots = logicDots.stream().filter(o -> {
			LogicDot dotInfo = (LogicDot) Utils.base(E3Graph.this, o);
			return dotInfo.isUnit == unit;
		}).collect(Collectors.toList());
		
		for (Object dot : logicDots) {
			if (getModel().getEdgeCount(dot) == 0) {
				//System.out.println("Connecting " + dot + " and " + signalDot);
				return connectCE(signalDot, dot);
			}
		}
		
		return null;
	}
	
	public Object connectLogicToLogic(Object l, Object r, boolean unitL, boolean unitR) {
		List<Object> logicDotsL = Utils.getChildrenWithValue(this, l, LogicDot.class);
		List<Object> logicDotsR = Utils.getChildrenWithValue(this, r, LogicDot.class);

		logicDotsL = logicDotsL.stream().filter(o -> {
			LogicDot dotInfo = (LogicDot) Utils.base(E3Graph.this, o);
			return dotInfo.isUnit == unitL;
		}).collect(Collectors.toList());
		
		
		logicDotsR = logicDotsR.stream().filter(o -> {
			LogicDot dotInfo = (LogicDot) Utils.base(E3Graph.this, o);
			return dotInfo.isUnit == unitR;
		}).collect(Collectors.toList());
		
		for (Object dotL : logicDotsL) {
			if (getModel().getEdgeCount(dotL) > 0) continue;
			for (Object dotR : logicDotsR) {
				if (getModel().getEdgeCount(dotR) > 0) continue;
				return connectCE(dotL, dotR);
			}
		}
		
		return null;
	}
	
	/**
	 * Turns a value model into a fraud model, or simply copies the model
	 * if it is already a fraud model. If it is a value model also prepends
	 * "Fraud model of " to the title. Lossless conversion.
	 * @return
	 */
	public E3Graph toFraud() {
		if (isFraud) {
			return new E3Graph(this, true);
		} 
		
		E3Graph fraud = new E3Graph(this, false);
		fraud.title = "Fraud model of " + title;
		fraud.isFraud = true;
		
		return fraud;
	}
	
	/**
	 * Converts the graph to a value graph if it is a fraud graph,
	 * and simply copies it otherwise. Also prepends "Value model of " 
	 * if it is a fraud model to the title. Lossy conversion (colluded
	 * actors are turned off, hidden transactions are deleted, non-occurring transactions
	 * are turned off).
	 * @return
	 */
	public E3Graph toValue() {
		if (!isFraud) {
			return new E3Graph(this, true);
		}
		
		E3Graph valueModel = new E3Graph(this, false);
		valueModel.title = "Value model of " + title;
		valueModel.isFraud = false;
		
		valueModel.getModel().beginUpdate();
		try {
			Utils.getAllCells(valueModel).stream().forEach(obj -> {
				Object value = valueModel.getModel().getValue(obj);
				
				if (value instanceof Actor) {
					valueModel.setColludingActor(obj, false);
				} else if (value instanceof ValueExchange) {
					valueModel.setValueExchangeNonOcurring(obj, false);
					
					ValueExchange veInfo = (ValueExchange) value;
					if (veInfo.isHidden()) {
						valueModel.removeCells(new Object[]{obj});
					}
				}
			});
		} finally {
			valueModel.getModel().endUpdate();
		}
		
		return valueModel;
	}
	
	public String toXML() {
		GraphIO.assureRegistered();

		mxCodec codec = new mxCodec();
		return mxXmlUtils.getXml(codec.encode(getModel()));
	}
	
	/**
	 * Expects the user to fill in the style afterwards!
	 * @param xml
	 * @return
	 */
	public static E3Graph fromXML(String xml, E3Style style) {
		GraphIO.assureRegistered();
		
		Document document = mxXmlUtils.parseXml(xml);
		
		mxCodec codec = new mxCodec(document);
		
		E3Graph graph = new E3Graph(style, false);
		
		codec.decode(document.getDocumentElement(), graph.getModel());
		
		return graph;
	}
	
	@Override
	public boolean isLabelMovable(Object cell) {
		if (getModel().getValue(cell) instanceof StartSignal
				|| getModel().getValue(cell) instanceof EndSignal) {
			return true;
		}
		
		return super.isLabelMovable(cell);
	}
	
	/**
	 * Returns the container (actor/market segment/value activity) of a
	 * value port.
	 * @param vp
	 * @return
	 */
	public Object getContainerOfValuePort(Object vp) {
		return model.getParent(model.getParent(vp));
	}
	
	@Override
	public boolean isValidConnection(Object source, Object target) {
		Base sourceVal = Utils.base(this, source);
		Base targetVal = Utils.base(this, target);
		
		if (sourceVal instanceof ValuePort && targetVal instanceof ValuePort) {
			ValuePort sourceInfo = (ValuePort) sourceVal;
			ValuePort targetInfo = (ValuePort) targetVal;
			
			// If the source and target are in the same VI do not allow an edge
			if (model.getParent(source) == model.getParent(target)) {
				return false;
			}
			
			Object sourceContainer = getContainerOfValuePort(source);
			Object targetContainer = getContainerOfValuePort(target);
			Base sourceContainerValue = (Base) model.getValue(sourceContainer);
			Base targetContainerValue = (Base) model.getValue(targetContainer);
			
			if ((model.getParent(sourceContainer) != getDefaultParent() && sourceContainerValue instanceof ValueActivity)
				|| (model.getParent(targetContainer) != getDefaultParent() && targetContainerValue instanceof ValueActivity)) {
				// If either one is a non-toplevel value activity it can only connect to its containing actor or market segment.

				boolean areAncestors = model.isAncestor(sourceContainer, targetContainer) || model.isAncestor(targetContainer, sourceContainer);
				boolean bothActorOrMarketSegmentAndValueActivity = (sourceContainerValue instanceof ValueActivity && (targetContainerValue instanceof Actor || targetContainerValue instanceof MarketSegment))
								|| ((sourceContainerValue instanceof Actor || targetContainerValue instanceof MarketSegment) && targetContainerValue instanceof ValueActivity);
				
				if (areAncestors && bothActorOrMarketSegmentAndValueActivity) {
					// Only if the ports point the same way
					return sourceInfo.incoming == targetInfo.incoming;
				}
			
				// Else it's not allowed
				return false;
			}
			
			// If the "containers" of the value ports have the same parent the
			// directions have to be different (one incoming, one outgoing)
			if (model.getParent(sourceContainer) == model.getParent(targetContainer)) {
				return sourceInfo.incoming != targetInfo.incoming;
			} 
			
			// Otherwise everything is probably fine
			return true;
		} else if (Utils.isDotValue(sourceVal) && Utils.isDotValue(targetVal)) {
			// If source and target are in the same logic element (and/or gate)
			// Do not allow an edge
			if (sourceVal instanceof LogicDot && targetVal instanceof LogicDot) {
				if (model.getParent(source) == model.getParent(target)) {
					return false;
				}
			}

			// Otherwise everything is probably fine
			return true;
		}
		
		return false;
	}
	
	@Override
	public String getCellValidationError(Object cell) {
		Object value = model.getValue(cell);
		
		Base info = (Base) value;
		String error = "";
		
		if (info instanceof ValueInterface) {
			if (Utils.getChildrenWithValue(this, cell, ValuePort.class)
				.stream()
				.map(model::getEdgeCount)
				.anyMatch(c -> c == 0)) {
				error += "Every Value Port should be connected to another ValuePort.\n";
			};

			if (Utils.getChildren(this, cell).stream()
				.filter(obj -> Utils.isDotValue((Base) getModel().getValue(obj)))
				.map(model::getEdgeCount)
				.anyMatch(c -> c == 0)) {
				error += "Every Signal Dot should be connected to another Signal Dot.\n";
			};
		}
		
		return error; 
	}
	
	/**
	 * Returns true if the graph is valid, false if not. At the moment only
	 * checks vertices for correctness.
	 */
	public boolean isValid() {
		return Utils.getAllCells(this).stream()
			.map(obj -> {
				if (E3Graph.this.getModel().isVertex(obj)) {
					return E3Graph.this.getCellValidationError(obj);		
				}

				return null;
			})
			.noneMatch(s -> {
				if (s == null) {
					return false;
				} else if (s.length() > 0) {
					return true;
				}

				return false;
			});
	}
	
    /**
	 * Returns true if the graph contains at least two actors actors false if not. 
	 */
	public long countActors() {
		return Utils.getAllCells(this).stream()
			.map(getModel()::getValue)
			.filter(cellValue -> cellValue instanceof Actor)
			.count();
	}
        
	/**
	 * Never extend something when something is added to it.
	 */
	@Override
	public boolean isExtendParent(Object cell) {
		return false;
	}

	/**
	 * Returns whether or not a value port is incoming.
	 * @param vp The value port to check
	 * @return True if incoming, false if not.
	 */
	public boolean getValuePortDirection(Object vp) {
		ValuePort vpInfo = (ValuePort) Utils.base(this, vp);
		return vpInfo.incoming;
	}

	public void setValuePortDirection(Object vp, boolean incoming) {
		ValuePort vpInfo = (ValuePort) Utils.base(this, vp);
		ValueInterface viInfo = (ValueInterface) Utils.base(this, getModel().getParent(vp));
		
		getModel().beginUpdate();
		try {
			vpInfo.incoming = incoming;
			
			getModel().setStyle(vp, "ValuePort" + vpInfo.getDirection(viInfo));
			getModel().setValue(vp, vpInfo);
		} finally {
			getModel().endUpdate();
		}
	}
	
	public void doUpdate(Runnable runnable) {
		model.beginUpdate();
		try {
			runnable.run();
		} finally {
			model.endUpdate();
		}
	}
}