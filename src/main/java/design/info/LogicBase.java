package design.info;

import design.info.Info.Side;

public class LogicBase extends Base {
	private static final long serialVersionUID = 7083658541375507487L;
	public boolean isOr = false;
	public Side direction = Side.RIGHT;

	@Override
	public Base getCopy() {
		LogicBase lb = new LogicBase();
		Base.setCommons(this, lb);

		lb.isOr = isOr;
		lb.direction = direction;

		return lb;
	}
}