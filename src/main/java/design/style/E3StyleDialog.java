package design.style;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.EventListenerList;

import com.mxgraph.util.mxConstants;

import design.E3Graph;
import design.info.Actor;
import design.info.Base;
import design.info.MarketSegment;
import design.info.ValueActivity;
import design.info.ValueExchange;

public class E3StyleDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2663789218894753081L;

	private EventListenerList listeners = new EventListenerList();
	
	public E3StyleDialog(E3Graph graph, Object cell) {
		Base info = (Base) graph.getModel().getValue(cell);
		
		boolean isEntity = info instanceof Actor
				|| info instanceof MarketSegment
				|| info instanceof ValueActivity;
		
		boolean isVE = info instanceof ValueExchange;
		
		if (!isEntity && !isVE) {
			throw new IllegalArgumentException("Cell style to edit is none of the following: Actor, Market Segment, ValueActivity, ValueExchange");
		}
		
		Map<String, Object> style = graph.getCellStyle(cell);

		Color currentBgColor, currentFontColor, currentStrokeColor;
		Font currentFont;

		currentBgColor = Color.decode("#C0C0C0"); 
		if (style.containsKey(mxConstants.STYLE_FILLCOLOR)) {
			currentBgColor = Color.decode((String) style.get(mxConstants.STYLE_FILLCOLOR));
		}
		
		currentStrokeColor = Color.decode("#000000");
		if (style.containsKey(mxConstants.STYLE_STROKECOLOR)) {
			currentStrokeColor = Color.decode(((String) style.get(mxConstants.STYLE_STROKECOLOR)));
		}
		
		currentFontColor = Color.BLACK;
		if (style.containsKey(mxConstants.STYLE_FONTCOLOR)) {
			currentFontColor = Color.decode((String) style.get(mxConstants.STYLE_FONTCOLOR));
		}
		
		int currentFontSize = 12;
		if (style.containsKey(mxConstants.STYLE_FONTSIZE)) {
			Object unknown = style.get(mxConstants.STYLE_FONTSIZE);
			
			if (unknown instanceof Integer) {
				currentFontSize = (Integer) unknown;
			} else if (unknown instanceof String) {
				currentFontSize = Integer.parseInt((String) unknown);
			} else {
				try {
					currentFontSize = Integer.parseInt(unknown.toString());
				} catch (NumberFormatException e) {
					currentFontSize = 12;
				}
			}
		}
		
		String fontFamily = "Dialog";
		if (style.containsKey(mxConstants.STYLE_FONTFAMILY)) {
			fontFamily = (String) style.get(mxConstants.STYLE_FONTFAMILY);
		}
		
		currentFont = new Font(fontFamily, Font.PLAIN, currentFontSize);
		
		initDialog(
				"Editing " + info.getClass().getSimpleName() + " \"" + info.name + "\"",
				isEntity,
				currentBgColor,
				currentStrokeColor,
				currentFontColor,
				currentFont
				);
		
	}
	
	E3StyleDialog(String title, boolean enableBG, Color bgColor, Color strokeColor, Color fontColor, Font initialFont) {
		initDialog(title, enableBG, bgColor, strokeColor, fontColor, initialFont);
	}

	@SuppressWarnings("serial")
	private void initDialog(String title, boolean enableBG, Color bgColor, Color strokeColor, Color fontColor, Font initialFont) {
		getContentPane().setLayout(new BorderLayout());
		
		E3StyleComponent e3sc = new E3StyleComponent(bgColor, strokeColor, fontColor, initialFont);
		add(e3sc, BorderLayout.CENTER);
		
		if (!enableBG) {
			e3sc.disableBG();
		}
		
		JPanel okCancelPanelContainer = new JPanel();
		okCancelPanelContainer.setLayout(new BorderLayout());
		
		JPanel okCancelPanel = new JPanel();
		okCancelPanel.setLayout(new BoxLayout(okCancelPanel, BoxLayout.X_AXIS));
		
		JButton okButton = new JButton(new AbstractAction("Ok") {
			@Override
			public void actionPerformed(ActionEvent e) {
				Color bgColor = e3sc.getSelectedBgColor();
				Color strokeColor = e3sc.getSelectedStrokeColor();
				Color fontColor = e3sc.getSelectedFontColor();
				Font selectedFont = e3sc.getSelectedFont();
				
				E3StyleEvent se = new E3StyleEvent(bgColor, strokeColor, fontColor, selectedFont);
				
				fireEvent(se);
				
				dispatchEvent(new WindowEvent(E3StyleDialog.this, WindowEvent.WINDOW_CLOSING));
			}
		});
		
		JButton cancelButton = new JButton(new AbstractAction("Cancel") {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispatchEvent(new WindowEvent(E3StyleDialog.this, WindowEvent.WINDOW_CLOSING));
			}
		});
		
		okCancelPanel.add(okButton, BorderLayout.EAST);
		okCancelPanel.add(cancelButton, BorderLayout.EAST);
		
		okCancelPanelContainer.add(okCancelPanel, BorderLayout.EAST);
		okCancelPanelContainer.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		add(okCancelPanelContainer, BorderLayout.SOUTH);
		
		setTitle(title);
		setSize(480, 240);
	}

	public void addListener(E3StyleEventListener listener) {
		listeners.add(E3StyleEventListener.class, listener);
	}
	
	public void removeListener(E3StyleEventListener listener) {
		listeners.remove(E3StyleEventListener.class, listener);
	}
	
	public void fireEvent(E3StyleEvent e) {
		for (E3StyleEventListener l : listeners.getListeners(E3StyleEventListener.class)) {
			l.invoke(e);
		}
	}
	
	public static void main(String[] args) {
		E3StyleDialog e3sd = new E3StyleDialog(
				"Test",
				true,
				Color.BLACK,
				Color.GREEN,
				Color.WHITE,
				new Font("Arial", Font.PLAIN, 12)
				);
		
		e3sd.addListener(e -> {
			System.out.println("Bg Color: " + e.bgColor);
			System.out.println("Stroke Color: " + e.strokeColor);
			System.out.println("Font Color: " + e.font);
			System.out.println("Font: " + e.fontColor);
		});
		
		e3sd.setVisible(true);
	}
}
