package design.style;

import java.awt.Color;
import java.awt.Font;

public class E3StyleEvent {
	public Color bgColor;
	public Color strokeColor;
	public Color fontColor;
	public Font font;
	
	public E3StyleEvent(Color bgColor, Color strokeColor, Color fontColor, Font font) {
		this.bgColor = bgColor;
		this.strokeColor = strokeColor;
		this.fontColor = fontColor;
		this.font = font;
	}
}
