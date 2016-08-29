package design.info;

public class MarketSegment extends Base {
	private static final long serialVersionUID = 952747256997418957L;
	
	public MarketSegment(long SUID) {
		super(SUID);
		
		formulas.put("COUNT", "1");
		formulas.put("INVESTMENT", "0");
		formulas.put("EXPENSES", "0");
		formulas.put("INTEREST", "0");
	}
	
	@Override
	public Base getCopy() {
		MarketSegment va = new MarketSegment(SUID);
		Base.setCommons(this, va);

		return va;
	}
	
	public String toString() {
		return name;
	}
}