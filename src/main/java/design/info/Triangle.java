package design.info;

public class Triangle extends Base {
	public Triangle(long SUID) {
		super(SUID);
	}

	@Override
	public Base getCopy() {
		// TODO Auto-generated method stub
		return new Triangle(SUID);
	}
}
