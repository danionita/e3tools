package design.info;

public class Bar extends Base {
	public Bar(long SUID) {
		super(SUID);
	}

	@Override
	public Base getCopy() {
		// TODO Auto-generated method stub
		return new Bar(SUID);
	}
}
