package design.info;

public class Actor extends Base {
	private static final long serialVersionUID = -5569247045409511931L;
	
	public boolean colluded = false;
	
	public Actor(long SUID) {
		super(SUID);
		
		formulas.put("INVESTMENT", "0");
		formulas.put("EXPENSES", "0");
		formulas.put("INTEREST", "0");
	}
	
	@Override
	public Base getCopy() {
		Actor va = new Actor(SUID);
		Base.setCommons(this, va);
		
		va.colluded = colluded;
		
		return va;
	}
	
	public String toString() {
		return name;
	}
}