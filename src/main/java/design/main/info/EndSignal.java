package design.main.info;

import java.util.Arrays;
import java.util.List;

public class EndSignal extends Base {
	private static final long serialVersionUID = -6483661636370237656L;
	
	public Boolean showLabel = false;
	
	public EndSignal() {
		name = "EndSignal" + getSUID();
		formulas.put("OCCURRENCES", "0");
	}

	@Override
	public Base getCopy() {
		EndSignal copy = new EndSignal();
		Base.setCommons(this, copy);
		
		copy.showLabel = showLabel;
		
		return copy;
	}
	
	@Override
	public String toString() {
		return showLabel ? name : "";
	}
	
	@Override
	public List<String> getImmutableProperties() {
		return Arrays.asList(
				"OCCURRENCES"
		);
	}
}