package design.info;

public class ConnectionElement extends Base {
	private static final long serialVersionUID = -6449976397261432365L;
	
	public ConnectionElement(long SUID) {
		super(SUID);
		
		name = "ConnectionElement" + SUID;
	}

	@Override
	public Base getCopy() {
		ConnectionElement ce = new ConnectionElement(SUID);
		Base.setCommons(this, ce);
		
		return ce;
	}
}