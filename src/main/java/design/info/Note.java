package design.info;

public class Note extends Base {
	private static final long serialVersionUID = -1834009930230992109L;

	public Note(long SUID) {
		super(SUID);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Base getCopy() {
		Note note = new Note(SUID);
		Base.setCommons(this, note);
		
		return note;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
