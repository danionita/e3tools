package design.checker;

import java.util.ArrayList;
import java.util.List;

public class ModelError {
	public String message;
	public List<Object> subjects;
	
	public ModelError(String message) {
		this.message = message;
	}
	
	public ModelError(String message, List<Object> subjects) {
		this.message = message;
		this.subjects = new ArrayList<>(subjects);
	}
}
