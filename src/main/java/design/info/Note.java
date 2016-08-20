package design.info;

public class Note extends Base {
	private static final long serialVersionUID = -1834009930230992109L;

	@Override
	public Base getCopy() {
		Note note = new Note();
		Base.setCommons(this, note);
		
		return note;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
