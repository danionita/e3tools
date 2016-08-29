package design.info;

import design.info.Info.Side;

public class ValuePort extends Base {
	private static final long serialVersionUID = 9212361683143336826L;
	public boolean incoming;
	
	public ValuePort(long SUID, boolean incoming) {
		super(SUID);
		
		this.incoming = incoming;
		
		formulas.put("VALUATION", "0");
		formulas.put("CADINALITY", "1");
		formulas.put("EXPENSES", "0");
	}
	
	@Override
	public Base getCopy() {
		ValuePort vp = new ValuePort(SUID, false);
		Base.setCommons(this, vp);

		vp.incoming = incoming;
		return vp;
	}

	public String getDirection(ValueInterface vi) {
		Side side = vi.side;
		assert(side != null);
		
		if (side == Side.TOP) {
			if (incoming) {
				return "South";
			} else {
				return "North";
			}
		} else if (side == Side.RIGHT) {
			if (incoming) {
				return "West";
			} else {
				return "East";
			}
		} else if (side == Side.BOTTOM) {
			if (incoming) {
				return "North";
			} else {
				return "South";
			}
		} else if (side == Side.LEFT){
			if (incoming) {
				return "East";
			} else {
				return "West";
			}
		} 

		return null;
	}
}