package design.main.info;

import design.main.info.Info.Side;

public class ValueInterface extends Base {
	private static final long serialVersionUID = -4820088710010430783L;
	
	public Side side;
	
	public ValueInterface() { }
	
	@Override
	public Base getCopy() {
		ValueInterface vi = new ValueInterface();
		Base.setCommons(this, vi);

		vi.side = side;

		return vi;
	}
}