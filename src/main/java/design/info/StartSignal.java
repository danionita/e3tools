package design.info;

public class StartSignal extends Base {
	private static final long serialVersionUID = -3440018877858008513L;
	
	public boolean showLabel = false;
	
	public StartSignal() {
		name = "StartSignal" + getSUID();
		formulas.put("OCCURRENCES", "1");
	}

	@Override
	public Base getCopy() {
		StartSignal copy = new StartSignal();
		Base.setCommons(this, copy);
		copy.showLabel = showLabel;

		return copy;
	}
	
	@Override
	public String toString() {
		return showLabel ? name : "";
	}
}