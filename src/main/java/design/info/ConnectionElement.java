package design.info;

public class ConnectionElement extends Base {
	private static final long serialVersionUID = -6449976397261432365L;
	
	public ConnectionElement() {
		name = "ConnectionElement" + getSUID();
	}

	@Override
	public Base getCopy() {
		ConnectionElement ce = new ConnectionElement();
		Base.setCommons(this, ce);
		
		return ce;
	}
}