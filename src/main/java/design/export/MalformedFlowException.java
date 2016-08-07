package design.export;

public class MalformedFlowException extends Exception {
	private static final long serialVersionUID = 271250946666843043L;

	public Object subject;

	public MalformedFlowException() {}
	
	public MalformedFlowException(String message) {
		super(message);
		this.subject = null;
	}
	
	public MalformedFlowException(String message, Object subject) {
		super(message);
		this.subject = subject;
	}
	
	public MalformedFlowException(Object subject) {
		this.subject = subject;
	}
}