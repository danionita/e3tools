package design.main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class Info {
	public static int nextSUID = 0;
	public static int getSUID() {
		return nextSUID++;
	}
	
	public static abstract class Base implements Serializable {
		private static final long serialVersionUID = -566615792608025058L;
		
		public int SUID = getSUID();
		public final HashMap<String, String> formulas = new LinkedHashMap<>();
		public String name;
		
		public abstract Base getCopy();
		
		public List<String> getImmutableProperties() {
			return Collections.unmodifiableList(new ArrayList<>());
		}
		
		public String toString() {
			return "";
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
			
			formulas.put("VALUATION", "");
			formulas.put("CADINALITY", "");
			formulas.put("EXPENSES", "");
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
		
		public Actor() {
			formulas.put("INVESTMENT", "0");
			formulas.put("EXPENSES", "0");
			formulas.put("INTEREST", "");
		}
		
		@Override
		public Base getCopy() {
			Actor va = new Actor();
			setCommons(this, va);
			
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
			formulas.put("INTEREST", "");
			formulas.put("MS_TYPE", "");
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
			formulas.put("TOTAL_VARIABLE_EXPENSES", "");
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
	
	public static class SignalDot extends Base {
		private static final long serialVersionUID = 7829429718862402191L;

		@Override
		public Base getCopy() {
			SignalDot copy = new SignalDot();
			setCommons(this, copy);
			return copy;
		}
	}
	
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
		public boolean labelHidden = false;
		
		public ValueExchange() {
			formulas.put("CARDINALITY", "1");
			formulas.put("VALUATION", "");
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
		
		public StartSignal() {
			formulas.put("COUNT", "1");
		}

		@Override
		public Base getCopy() {
			StartSignal copy = new StartSignal();
			setCommons(this, copy);

			return copy;
		}
	}
	
	public static class EndSignal extends Base {
		private static final long serialVersionUID = -6483661636370237656L;
		
		public EndSignal() {
			formulas.put("OCCURRENCES", "");
		}

		@Override
		public Base getCopy() {
			EndSignal copy = new EndSignal();
			setCommons(this, copy);
			
			return copy;
		}
		
		@Override
		public List<String> getImmutableProperties() {
			return Arrays.asList(
					"OCCURRENCES"
			);
		}
	}
}
