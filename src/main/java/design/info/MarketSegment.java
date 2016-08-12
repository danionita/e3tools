package design.info;

public class MarketSegment extends Base {
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
		Base.setCommons(this, va);

		return va;
	}
	
	public String toString() {
		return name;
	}
}