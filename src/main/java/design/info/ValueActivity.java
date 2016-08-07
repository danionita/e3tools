package design.info;

public class ValueActivity extends Base {
	private static final long serialVersionUID = 6344879576710522969L;
	
	public ValueActivity() {
		formulas.put("TOTAL_VARIABLE_EXPENSES", "0");
		formulas.put("INVESTMENT", "0");
		formulas.put("EXPENSES", "0");
	}
	
	@Override
	public Base getCopy() {
		ValueActivity va = new ValueActivity();
		Base.setCommons(this, va);

		return va;
	}
	
	@Override
	public String toString() {
		return name;
	}
}