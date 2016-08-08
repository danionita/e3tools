package design.info;

/**
 * Dot for value interfaces & start/end signals
 * @author Bobe
 *
 */
public class SignalDot extends Base {
	private static final long serialVersionUID = 7829429718862402191L;

	public SignalDot(long SUID) {
		super(SUID);
	}

	@Override
	public Base getCopy() {
		SignalDot copy = new SignalDot(SUID);
		Base.setCommons(this, copy);
		return copy;
	}
}