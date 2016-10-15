package design.info;

import java.util.Arrays;
import java.util.List;

public class EndSignal extends Base {
	private static final long serialVersionUID = -6483661636370237656L;
	
	public boolean showLabel = false;
	
	public EndSignal(long SUID) {
		super(SUID);
		
		name = "EndSignal" + SUID;
	}

	@Override
	public Base getCopy() {
		EndSignal copy = new EndSignal(SUID);
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
		return Arrays.asList();
	}
}