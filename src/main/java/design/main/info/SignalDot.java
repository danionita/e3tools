package design.main.info;

/**
 * Dot for value interfaces & start/end signals
 * @author Bobe
 *
 */
public class SignalDot extends Base {
	private static final long serialVersionUID = 7829429718862402191L;

	@Override
	public Base getCopy() {
		SignalDot copy = new SignalDot();
		Base.setCommons(this, copy);
		return copy;
	}
}