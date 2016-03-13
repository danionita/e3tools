package design.main;

public class Info {
	public static abstract class Base {
		public abstract Base getCopy();
		public String toString() {
			return "";
		}
	}
	
	public static class ValuePort extends Base {
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
	}
	
	public static class ValueInterface extends Base {
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
}
