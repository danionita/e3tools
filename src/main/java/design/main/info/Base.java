package design.main.info;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public abstract class Base implements Serializable {
	private static final long serialVersionUID = -566615792608025058L;
	
	long SUID = Info.getSUID();
	public final HashMap<String, String> formulas = new LinkedHashMap<>();
	public String name = "";
	
	public abstract Base getCopy();
	
	public List<String> getImmutableProperties() {
		return Collections.unmodifiableList(new ArrayList<>());
	}
	
	public String toString() {
		return "";
	}
	
	public void setSUID(long newSUID) {
		if (Info.nextSUID <= newSUID) Info.nextSUID = newSUID + 1;

		SUID = newSUID;
	}
	
	public long getSUID() {
		return SUID;
	}

	public static final void setCommons(Base source, Base target) {
		target.SUID = source.SUID;
		// If non-null, copy it
		target.name = source.name == null ? null : new String(source.name);
		target.formulas.clear();
		target.formulas.putAll(source.formulas);
	}
}