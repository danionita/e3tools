package design.style;

import design.E3Style;

public enum Element {
	ACTOR("Actor", E3Style.ACTOR),
	MARKET_SEGMENT("Market Segment", E3Style.MARKET_SEGMENT),
	VALUE_ACTIVITY("Value Activity", E3Style.VALUE_ACTIVITY),
	VALUE_EXCHANGE("Value Exchange", E3Style.VALUE_EXCHANGE);
	
	private final String value;
	private final String styleName;
	
	Element(String value, String styleName) {
		this.value = value;
		this.styleName = styleName;
	}
	
	@Override
	public String toString() {
		return value;
	}
	
	public String getStyleName() {
		return styleName;
	}
}