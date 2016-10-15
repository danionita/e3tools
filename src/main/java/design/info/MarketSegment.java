package design.info;

public class MarketSegment extends Base {
	private static final long serialVersionUID = 952747256997418957L;
	
	public boolean colluded = false;
	
	public MarketSegment(long SUID) {
		super(SUID);
		
		formulas.put("COUNT", "1");
		formulas.put("INVESTMENT", "0");
		formulas.put("EXPENSES", "0");
		formulas.put("INTEREST", "0");
	}
	
	@Override
	public Base getCopy() {
		MarketSegment ms = new MarketSegment(SUID);
		Base.setCommons(this, ms);
		
		ms.colluded = colluded;

		return ms;
	}
	
	public String toString() {
		return name;
	}
}