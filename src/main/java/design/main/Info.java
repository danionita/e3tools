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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class Info {
	public static long nextSUID = 0;
	public static long getSUID() {
		return nextSUID++;
	}
	
	public static abstract class Base implements Serializable {
		private static final long serialVersionUID = -566615792608025058L;
		
		private long SUID = Info.getSUID();
		public final HashMap<String, String> formulas = new LinkedHashMap<>();
		public String name = "";
		
		public abstract Base getCopy();
		
		public List<String> getImmutableProperties() {
			return Collections.unmodifiableList(new ArrayList<>());
		}
		
		public String toString() {
			return "";
		}
		
		public void setSUID(long newSUID) {
			if (nextSUID <= newSUID) nextSUID = newSUID + 1;

			SUID = newSUID;
		}
		
		public long getSUID() {
			return SUID;
		}
	}
	
	public static final void setCommons(Base source, Base target) {
		target.SUID = source.SUID;
		// If non-null, copy it
		target.name = source.name == null ? null : new String(source.name);
		target.formulas.clear();
		target.formulas.putAll(source.formulas);
	}

	public static enum Side {
		TOP, RIGHT, BOTTOM, LEFT;
		public Side rotateRight() {
			if (this == TOP) {
				return RIGHT;
			} else if (this == RIGHT) {
				return BOTTOM;
			} else if (this == BOTTOM) {
				return LEFT;
			} else { // (this == LEFT)
				return TOP;
			}
		}
		public Side rotateLeft() {
			return this.rotateRight().rotateRight().rotateRight();
		}
	}
	
	public static class ValuePort extends Base {
		private static final long serialVersionUID = 9212361683143336826L;
		public boolean incoming;
		
		ValuePort(boolean incoming_) {
			incoming = incoming_;
			
			formulas.put("VALUATION", "0");
			formulas.put("CADINALITY", "0");
			formulas.put("EXPENSES", "0");
		}
		
		@Override
		public Base getCopy() {
			ValuePort vp = new ValuePort(false);
			setCommons(this, vp);

			vp.incoming = incoming;
			return vp;
		}

		String getDirection(ValueInterface vi) {
			Side side = vi.side;
			assert(side != null);
			
			if (side == Side.TOP) {
				if (incoming) {
					return "South";
				} else {
					return "North";
				}
			} else if (side == Side.RIGHT) {
				if (incoming) {
					return "West";
				} else {
					return "East";
				}
			} else if (side == Side.BOTTOM) {
				if (incoming) {
					return "North";
				} else {
					return "South";
				}
			} else if (side == Side.LEFT){
				if (incoming) {
					return "East";
				} else {
					return "West";
				}
			} 

			return null;
		}
	}
	
	public static class ValueInterface extends Base {
		private static final long serialVersionUID = -4820088710010430783L;
		
		public Side side;
		
		@Override
		public Base getCopy() {
			ValueInterface vi = new ValueInterface();
			setCommons(this, vi);

			vi.side = side;

			return vi;
		}
	}
	
	public static class Actor extends Base {
		private static final long serialVersionUID = -5569247045409511931L;
		
		public boolean colluded;
		
		public Actor() {
			formulas.put("INVESTMENT", "0");
			formulas.put("EXPENSES", "0");
			formulas.put("INTEREST", "0");
		}
		
		@Override
		public Base getCopy() {
			Actor va = new Actor();
			setCommons(this, va);
			
			va.colluded = colluded;
			
			return va;
		}
		
		public String toString() {
			return name;
		}
	}

	public static class MarketSegment extends Base {
		private static final long serialVersionUID = 952747256997418957L;
		
		public MarketSegment() {
			formulas.put("COUNT", "1");
			formulas.put("INVESTMENT", "0");
			formulas.put("EXPENSES", "0");
			formulas.put("INTEREST", "0");
			formulas.put("MS_TYPE", "0");
		}
		
		@Override
		public Base getCopy() {
			MarketSegment va = new MarketSegment();
			setCommons(this, va);

			return va;
		}
		
		public String toString() {
			return name;
		}
	}

	public static class ValueActivity extends Base {
		private static final long serialVersionUID = 6344879576710522969L;
		
		public ValueActivity() {
			formulas.put("TOTAL_VARIABLE_EXPENSES", "0");
			formulas.put("INVESTMENT", "0");
			formulas.put("EXPENSES", "0");
		}
		
		@Override
		public Base getCopy() {
			ValueActivity va = new ValueActivity();
			setCommons(this, va);

			return va;
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
	
	/**
	 * Dot for value interfaces & start/end signals
	 * @author Bobe
	 *
	 */
	public static class SignalDot extends Base {
		private static final long serialVersionUID = 7829429718862402191L;

		@Override
		public Base getCopy() {
			SignalDot copy = new SignalDot();
			setCommons(this, copy);
			return copy;
		}
	}
	
	/**
	 * Dot for on logic units.
	 * @author Bobe
	 *
	 */
	public static class LogicDot extends Base {
		private static final long serialVersionUID = 6736897501245007019L;
		// Unit dot is dot that is alone on one side of the logic unit
		public boolean isUnit = false;
		public int proportion = 1;
		
		public LogicDot(boolean isUnit_) {isUnit = isUnit_;}

		@Override
		public Base getCopy() {
			LogicDot dot = new LogicDot(false);
			setCommons(this, dot);
			
			dot.isUnit = isUnit;
			dot.proportion = proportion;

			return dot;
		}
	}
	
	public static class LogicBase extends Base {
		private static final long serialVersionUID = 7083658541375507487L;
		public boolean isOr = false;
		public Side direction = Side.RIGHT;

		@Override
		public Base getCopy() {
			LogicBase lb = new LogicBase();
			setCommons(this, lb);

			lb.isOr = isOr;
			lb.direction = direction;

			return lb;
		}
	}
	
	public static class ValueExchange extends Base {
		private static final long serialVersionUID = -7607653966138790703L;
		public String valueObject = null;
		public boolean valueObjectHidden = false;
		public boolean labelHidden = true;
		
		public ValueExchange() {
			formulas.put("CARDINALITY", "1");
			formulas.put("VALUATION", "0");
		}

		@Override
		public Base getCopy() {
			ValueExchange copy = new ValueExchange();
			setCommons(this, copy);
			
			copy.valueObject = valueObject;
			copy.labelHidden = labelHidden;
			copy.valueObjectHidden = valueObjectHidden;

			return copy;
		}
		
		/**
		 * Labels are managed manually by opening/closing E3Properties etc.
		 */
		@Override
		public String toString() {
			return "";
		}
		
		@Override
		public List<String> getImmutableProperties() {
			return Arrays.asList(
					"dotted",
					"dashed"
			);
		}
	}
	
	public static class ValueExchangeLabel extends Base {
		private static final long serialVersionUID = -8263020130640344457L;

		public boolean isValueObjectLabel = false;

		@Override
		public Base getCopy() {
			ValueExchangeLabel copy = new ValueExchangeLabel();
			setCommons(this, copy);

			copy.isValueObjectLabel = isValueObjectLabel;

			return copy;
		}
		
		public String toString() {
			return name;
		}
	}
	
	public static class StartSignal extends Base {
		private static final long serialVersionUID = -3440018877858008513L;
		
		public boolean showLabel = false;
		
		public StartSignal() {
			name = "StartSignal" + getSUID();
			formulas.put("COUNT", "1");
		}

		@Override
		public Base getCopy() {
			StartSignal copy = new StartSignal();
			setCommons(this, copy);
			copy.showLabel = showLabel;

			return copy;
		}
		
		@Override
		public String toString() {
			return showLabel ? name : "";
		}
	}
	
	public static class EndSignal extends Base {
		private static final long serialVersionUID = -6483661636370237656L;
		
		public boolean showLabel = false;
		
		public EndSignal() {
			name = "EndSignal" + getSUID();
			formulas.put("OCCURRENCES", "0");
		}

		@Override
		public Base getCopy() {
			EndSignal copy = new EndSignal();
			setCommons(this, copy);
			
			copy.showLabel = showLabel;
			
			return copy;
		}
		
		@Override
		public String toString() {
			return showLabel ? name : "";
		}
		
		@Override
		public List<String> getImmutableProperties() {
			return Arrays.asList(
					"OCCURRENCES"
			);
		}
	}
	
	public static class ConnectionElement extends Base {
		private static final long serialVersionUID = -6449976397261432365L;
		
		public ConnectionElement() {
			name = "ConnectionElement" + getSUID();
		}

		@Override
		public Base getCopy() {
			ConnectionElement ce = new ConnectionElement();
			setCommons(this, ce);
			
			return ce;
		}
	}
}
