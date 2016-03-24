package design.main;

import java.io.Serializable;

import design.main.Info.ValueInterface.Side;

public class Info {
	public static abstract class Base implements Serializable {
		private static final long serialVersionUID = -566615792608025058L;
		public abstract Base getCopy();
		public String toString() {
			return "";
		}
	}
	
	public static class ValuePort extends Base {
		private static final long serialVersionUID = 9212361683143336826L;
		public boolean incoming;
		
		ValuePort(boolean incoming_) {
			incoming = incoming_;
		}
		
		@Override
		public Base getCopy() {
			ValuePort vp = new ValuePort(false);
			vp.incoming = incoming;
			return vp;
		}

		String getDirection(ValueInterface vi) {
			ValueInterface.Side side = vi.side;
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
			} else {
				System.out.println("No match! It is: " + side);
			}

			return null;
		}
	}
	
	public static class ValueInterface extends Base {
		private static final long serialVersionUID = -4820088710010430783L;

		public static enum Side {TOP, RIGHT, BOTTOM, LEFT}
		
		public Side side;
		
		@Override
		public Base getCopy() {
			ValueInterface vi = new ValueInterface();
			vi.side = side;
			return vi;
		}
	}
	
	public static class Actor extends Base {
		private static final long serialVersionUID = -5569247045409511931L;
		public String name;
		
		@Override
		public Base getCopy() {
			Actor va = new Actor();
			va.name = name;
			return va;
		}
		
		public String toString() {
			return name;
		}
	}

	public static class MarketSegment extends Base {
		private static final long serialVersionUID = 952747256997418957L;
		public String name;
		
		@Override
		public Base getCopy() {
			MarketSegment va = new MarketSegment();
			va.name = name;
			return va;
		}
		
		public String toString() {
			return name;
		}
	}

	public static class ValueActivity extends Base {
		private static final long serialVersionUID = 6344879576710522969L;
		public String name;
		
		@Override
		public Base getCopy() {
			ValueActivity va = new ValueActivity();
			va.name = name;
			return va;
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
	
	public static class Dot extends Base {
		private static final long serialVersionUID = 6736897501245007019L;

		@Override
		public Base getCopy() {
			return new Dot();
		}
	}
	
	public static class LogicBase extends Base {
		private static final long serialVersionUID = 7083658541375507487L;
		public boolean isOr = false;

		@Override
		public Base getCopy() {
			LogicBase lb = new LogicBase();
			lb.isOr = isOr;
			return lb;
		}
	}
}
