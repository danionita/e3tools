package design.main.info;

/**
 * Dot for on logic units.
 * @author Bobe
 *
 */
public class LogicDot extends Base {
	private static final long serialVersionUID = 6736897501245007019L;
	// Unit dot is dot that is alone on one side of the logic unit
	public Boolean isUnit = false;
	public Integer proportion = 1;
	
	public LogicDot(boolean isUnit) {this.isUnit = isUnit;}
	
	public LogicDot() {}

	@Override
	public Base getCopy() {
		LogicDot dot = new LogicDot(false);
		Base.setCommons(this, dot);
		
		dot.isUnit = isUnit;
		dot.proportion = proportion;

		return dot;
	}

	public Integer getProportion() {
		return proportion;
	}

	public void setProportion(Integer proportion) {
		this.proportion = proportion;
	}
}