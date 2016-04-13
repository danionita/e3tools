package design.main.properties;

import java.util.EventObject;

import design.main.Info;

public class E3PropertiesEvent extends EventObject {
	public Info.Base resultObject;
	
	public E3PropertiesEvent(Object source, Info.Base resultObject_) {
		super(source);
		resultObject = resultObject_;
	}
}
