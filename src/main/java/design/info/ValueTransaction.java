package design.info;

import java.util.ArrayList;
import java.util.List;

public class ValueTransaction extends Base {
	private static final long serialVersionUID = 8694277899772783482L;
	
	public List<Long> exchanges;

	public ValueTransaction(long SUID) {
		super(SUID);
		name = "vt" + SUID;
		
		formulas.put("FRACTION", "1");
		
		exchanges = new ArrayList<>();
	}

	@Override
	public Base getCopy() {
		ValueTransaction vt = new ValueTransaction(SUID);
		Base.setCommons(this, vt);

		vt.exchanges = new ArrayList<>(exchanges);
		
		return vt;
	}
	
	@Override
	public String toString() {
		return SUID + ": " + name + " consists of " + exchanges.size() + " elements";
	}
}
