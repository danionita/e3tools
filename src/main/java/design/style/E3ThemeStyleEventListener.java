package design.style;

import java.util.EventListener;
import java.util.Map;

public interface E3ThemeStyleEventListener extends EventListener {
	public void invoke(Map<Element, E3StyleEvent> event);
}
