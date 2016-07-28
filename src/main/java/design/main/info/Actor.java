package design.main.info;

public class Actor extends Base {
	private static final long serialVersionUID = -5569247045409511931L;
	
	public Boolean colluded = false;
	
	public Actor() {
		formulas.put("INVESTMENT", "0");
		formulas.put("EXPENSES", "0");
		formulas.put("INTEREST", "0");
	}
	
	@Override
	public Base getCopy() {
		Actor va = new Actor();
		Base.setCommons(this, va);
		
		va.colluded = colluded;
		
		return va;
	}
	
	public String toString() {
		return name;
	}
}