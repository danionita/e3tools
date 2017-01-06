package design.style;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.event.EventListenerList;

import design.E3Graph;
import design.E3Style;

public class E3StyleEditor extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7189585201276275873L;

	private EventListenerList listeners = new EventListenerList();
	private final CardLayout cl;
	private final E3Graph graph;
	private final JPanel styleComponentsPanel;
	private final Map<Element, E3StyleComponent> styleComponents = new HashMap<>();

	public E3StyleEditor(E3Graph graph) {
		this.graph = graph;
		
		setModal(true);
		setTitle("Style editor");
		
		JPanel panel = new JPanel();
		panel.setBorder(new TitledBorder(null, "Element selection", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		getContentPane().add(panel, BorderLayout.NORTH);
		
		JComboBox<Element> elementComboBox = new JComboBox<Element>();
		
		elementComboBox.addItem(Element.ACTOR);
		elementComboBox.addItem(Element.MARKET_SEGMENT);
		elementComboBox.addItem(Element.VALUE_ACTIVITY);
		elementComboBox.addItem(Element.VALUE_EXCHANGE);
		
		panel.add(elementComboBox);
		
		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1, BorderLayout.SOUTH);
		panel_1.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_2 = new JPanel();
		panel_1.add(panel_2, BorderLayout.EAST);
		
		JButton okButton = new JButton("Ok");
		panel_2.add(okButton);
		
		JButton cancelButton = new JButton("Cancel");
		panel_2.add(cancelButton);
		
		styleComponentsPanel = new JPanel();
		styleComponentsPanel.setBorder(new TitledBorder(null, "Style", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		getContentPane().add(styleComponentsPanel, BorderLayout.CENTER);

		cl = new CardLayout(0, 0);
		styleComponentsPanel.setLayout(cl);

		addElement(Element.ACTOR);
		addElement(Element.MARKET_SEGMENT);
		addElement(Element.VALUE_ACTIVITY);
		addElement(Element.VALUE_EXCHANGE);
		
		setSize(400, 300);

		elementComboBox.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				return;
			}
			
			cl.show(styleComponentsPanel, e.getItem().toString());
		});
		
		okButton.addActionListener(e -> {
			Map<Element, E3StyleEvent> msg = new HashMap<>();
			msg.put(Element.ACTOR, styleComponents.get(Element.ACTOR).getEventObj());
			msg.put(Element.MARKET_SEGMENT, styleComponents.get(Element.MARKET_SEGMENT).getEventObj());
			msg.put(Element.VALUE_ACTIVITY, styleComponents.get(Element.VALUE_ACTIVITY).getEventObj());
			msg.put(Element.VALUE_EXCHANGE, styleComponents.get(Element.VALUE_EXCHANGE).getEventObj());
			
			fireEvent(msg);
			
			E3StyleEditor.this.dispatchEvent(new WindowEvent(E3StyleEditor.this, WindowEvent.WINDOW_CLOSING));
		});

		cancelButton.addActionListener(e -> {
			E3StyleEditor.this.dispatchEvent(new WindowEvent(E3StyleEditor.this, WindowEvent.WINDOW_CLOSING));
		});
	}
	
	private void addElement(Element el) {
		String elementName = el.getStyleName();

		E3Style style = graph.style;
		
		Color bgColor = style.getBackgroundColor(elementName).orElse(Color.BLACK);
		Color strokeColor = style.getStrokeColor(elementName).orElse(Color.BLACK);
		Color fontColor = style.getFontColor(elementName).orElse(Color.BLACK);
		Font font = style.getFont(elementName);

		E3StyleComponent e3sc = new E3StyleComponent(bgColor, strokeColor, fontColor, font);
		if (el == Element.VALUE_EXCHANGE) {
			e3sc.disableBG();
		}

		styleComponentsPanel.add(e3sc, el.toString());
		
		styleComponents.put(el, e3sc);
	}
	
	public void addListener(E3ThemeStyleEventListener listener) {
		listeners.add(E3ThemeStyleEventListener.class, listener);
	}
	
	public void removeListener(E3ThemeStyleEventListener listener) {
		listeners.remove(E3ThemeStyleEventListener.class, listener);
	}
	
	public void fireEvent(Map<Element, E3StyleEvent> e) {
		for (E3ThemeStyleEventListener l : listeners.getListeners(E3ThemeStyleEventListener.class)) {
			l.invoke(e);
		}
	}
	
	public static void main(String[] args) {
        E3Graph graph = new E3Graph(E3Style.loadInternal("E3Style").get(), true);
        
        List<Font> fonts = new ArrayList<>();
        
        E3StyleEditor editor = new E3StyleEditor(graph);
        editor.setModal(true);
        editor.addListener(e -> {
        	for (Entry<Element, E3StyleEvent> entry : e.entrySet()) {
        		System.out.println("Changes for element " + entry.getKey().name());
        		E3StyleEvent e3se = entry.getValue();
        		
        		System.out.println("Background color: " + e3se.bgColor);
        		System.out.println("Font color: " + e3se.fontColor);
        		System.out.println("Font: " + e3se.font);
        		System.out.println("Stroke color: " + e3se.strokeColor);
        		
        		fonts.add(e3se.font);
        	}
        	
        	System.out.println("Equality: " + fonts.get(0).equals(fonts.get(1)));
        });
        
        editor.setVisible(true);
	}
}
