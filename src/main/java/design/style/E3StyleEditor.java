package design.style;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.border.EmptyBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.EventListenerList;

import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;

import design.Main;
import design.info.Actor;
import design.info.Base;
import design.info.MarketSegment;
import design.info.ValueActivity;
import design.info.ValueExchange;

/**
 * This class was made with window editor pro or something
 * (the most default eclipse swing gui editor plugin for eclipse)
 * So you should be able to open this file in that and edit it graphically.
 * If you don't have access to the plugin, editing this by hand in the text
 * itself is also fine, but that might break the plugin support at some point.
 * The class will keep working though.
 * @author Bob
 *
 */
public class E3StyleEditor extends JDialog {

	private final JPanel contentPanel = new JPanel();
	
	EventListenerList listeners = new EventListenerList();

	/**
	 * Create the dialog.
	 */
	public E3StyleEditor(mxGraph graph, Object cell) {
		Base info = (Base) graph.getModel().getValue(cell);
		
		boolean isEntity = info instanceof Actor
				|| info instanceof MarketSegment
				|| info instanceof ValueActivity;
		
		boolean isVE = info instanceof ValueExchange;
		
		Map<String, Object> style = graph.getCellStyle(cell);

		Color currentFillColor;
		if (isEntity) {
			currentFillColor = Color.decode("#C0C0C0"); 
			if (style.containsKey(mxConstants.STYLE_FILLCOLOR)) {
				currentFillColor = Color.decode((String) style.get(mxConstants.STYLE_FILLCOLOR));
			}
		} else if (isVE) {
			currentFillColor = Color.decode("#0000FF");
			if (style.containsKey(mxConstants.STYLE_STROKECOLOR)) {
				currentFillColor = Color.decode((String) style.get(mxConstants.STYLE_STROKECOLOR));
			}
		} else {
			throw new RuntimeException("Unsupported cell type passed to editor");
		}
		
		Color currentFontColor = Color.BLACK;
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

		JLabel backgroundColorLabel;
		JLabel fontColorLabel;
		JSpinner spinner;
		String backgroundColorLabelContents = "";
		
		if (isEntity) backgroundColorLabelContents = "Background color";
		if (isVE) backgroundColorLabelContents = "Color";
		
		setTitle("Style editor");
		setBounds(100, 100, 309, 204);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));
		{
			Component horizontalStrut = Box.createHorizontalStrut(20);
			contentPanel.add(horizontalStrut);
		}
		{
			JPanel panel = new JPanel();
			contentPanel.add(panel);
			panel.setLayout(new GridLayout(3, 2, 0, 20));
			{
				JLabel lblNewLabel = new JLabel(backgroundColorLabelContents);
				panel.add(lblNewLabel);
			}
			{
				{
					JPanel panel_1 = new JPanel();
					panel.add(panel_1);
					panel_1.setLayout(new BorderLayout(0, 0));

					backgroundColorLabel = new JLabel("");
					backgroundColorLabel.setBackground(currentFillColor);
					backgroundColorLabel.setOpaque(true);
					backgroundColorLabel.setBorder(BorderFactory.createLineBorder(Color.decode("#111111")));
					panel_1.add(backgroundColorLabel, BorderLayout.CENTER);

					JButton btnNewButton = new JButton("Pick color");
					panel_1.add(btnNewButton, BorderLayout.EAST);

					makeButtonMimicColorDialog(btnNewButton, backgroundColorLabel);
				}
			}
			{
				JLabel lblNewLabel_1 = new JLabel("Font color");
				panel.add(lblNewLabel_1);
			}
			{
				JPanel panel_1 = new JPanel();
				panel.add(panel_1);
				panel_1.setLayout(new BorderLayout(0, 0));

				fontColorLabel = new JLabel("");
				fontColorLabel.setBackground(currentFontColor);
				fontColorLabel.setOpaque(true);
				fontColorLabel.setBorder(BorderFactory.createLineBorder(Color.decode("#111111")));
				panel_1.add(fontColorLabel, BorderLayout.CENTER);

				JButton btnNewButton_1 = new JButton("Pick color");
				panel_1.add(btnNewButton_1, BorderLayout.EAST);
				btnNewButton_1.setBackground(currentFontColor);

				makeButtonMimicColorDialog(btnNewButton_1, fontColorLabel);
			}
			{
				JLabel lblFontSize = new JLabel("Font size");
				panel.add(lblFontSize);
			}
			{
				JPanel panel_1 = new JPanel();
				panel.add(panel_1);
				panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));
				{
					spinner = new JSpinner();
					spinner.setValue(currentFontSize);
					panel_1.add(spinner);
				}
			}
		}
		{
			Component horizontalStrut = Box.createHorizontalStrut(20);
			contentPanel.add(horizontalStrut);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
				
				okButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						fireEvent(new E3StyleEvent(
								backgroundColorLabel.getBackground(),
								fontColorLabel.getBackground(),
								(Integer) spinner.getValue()
								));
						
						E3StyleEditor.this.dispose();
					}
				});
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
				
				cancelButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						E3StyleEditor.this.dispose();
					}
				});
			}
		}
	}
	
	public static JColorChooser getColorChooser(Color initialColor) {
		JColorChooser cc = new JColorChooser(initialColor);
		// Disables preview panel
		cc.setPreviewPanel(new JPanel());
		for (AbstractColorChooserPanel acc : cc.getChooserPanels()) {
			if (!acc.getDisplayName().equals("RGB")) {
				cc.removeChooserPanel(acc);
			}
		}

		// return JColorChooser.createDialog(Main.mainFrame, "Pick a color", true, cc, null, null);
		return cc;
	}
	
	public static void makeButtonMimicColorDialog(JButton button, JLabel label) {
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				//System.out.println(label.getBackground());
				JColorChooser cc = getColorChooser(label.getBackground());
				// cc.setVisible(true);
				
				JColorChooser.createDialog(Main.mainFrame, "Pick a color", true, cc, new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						label.setBackground(cc.getColor());
					}
				}, null).setVisible(true);
			}
		});
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
}
