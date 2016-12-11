package design.style;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.EventListenerList;

import com.connectina.swing.FontChooserDialog;

import design.Main;

/**
 * This class was made with window builder/editor pro or something
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
	private JLabel lblBackgroundColor;

	/**
	 * Create the dialog.
	 */
	public E3StyleEditor() {
//		Base info = (Base) graph.getModel().getValue(cell);
		
//		boolean isEntity = info instanceof Actor
//				|| info instanceof MarketSegment
//				|| info instanceof ValueActivity;
//		
//		boolean isVE = info instanceof ValueExchange;
		
//		Map<String, Object> style = graph.getCellStyle(cell);
//
		// TODO: Take this from conmstructor argument or something!
		Color currentFillColor = Color.decode("#C0C0C0");
//		if (isEntity) {
//			currentFillColor = Color.decode("#C0C0C0"); 
//			if (style.containsKey(mxConstants.STYLE_FILLCOLOR)) {
//				currentFillColor = Color.decode((String) style.get(mxConstants.STYLE_FILLCOLOR));
//			}
//		} else if (isVE) {
//			currentFillColor = Color.decode("#0000FF");
//			if (style.containsKey(mxConstants.STYLE_STROKECOLOR)) {
//				currentFillColor = Color.decode((String) style.get(mxConstants.STYLE_STROKECOLOR));
//			}
//		} else {
//			throw new RuntimeException("Unsupported cell type passed to editor");
//		}
		
		Color currentFontColor = Color.BLACK;
//		if (style.containsKey(mxConstants.STYLE_FONTCOLOR)) {
//			currentFontColor = Color.decode((String) style.get(mxConstants.STYLE_FONTCOLOR));
//		}
//		
//		int currentFontSize = 12;
//		if (style.containsKey(mxConstants.STYLE_FONTSIZE)) {
//			Object unknown = style.get(mxConstants.STYLE_FONTSIZE);
//			
//			if (unknown instanceof Integer) {
//				currentFontSize = (Integer) unknown;
//			} else if (unknown instanceof String) {
//				currentFontSize = Integer.parseInt((String) unknown);
//			} else {
//				try {
//					currentFontSize = Integer.parseInt(unknown.toString());
//				} catch (NumberFormatException e) {
//					currentFontSize = 12;
//				}
//			}
//		}

		JLabel bgColorLabel;
		JLabel strokeColorLabel;
		JLabel fontColorLabel;
		JLabel fontLabel;
		
		setTitle("Style editor");
		setBounds(100, 100, 375, 333);
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
			panel.setLayout(new BorderLayout(0, 0));
			{
				JPanel panel_1 = new JPanel();
				panel_1.setBorder(new EmptyBorder(0, 0, 0, 20));
				panel.add(panel_1, BorderLayout.WEST);
				panel_1.setLayout(new GridLayout(4, 1, 0, 10));
				{
					lblBackgroundColor = new JLabel("Background color");
					lblBackgroundColor.setLabelFor(lblBackgroundColor);
					panel_1.add(lblBackgroundColor);
				}
				{
					JLabel lblStrokeColor = new JLabel("Stroke color");
					panel_1.add(lblStrokeColor);
				}
				{
					JLabel lblFontColor = new JLabel("Font color");
					panel_1.add(lblFontColor);
				}
				{
					JLabel lblFont = new JLabel("Font");
					panel_1.add(lblFont);
				}
			}
			{
				JPanel panel_1_1 = new JPanel();
				panel.add(panel_1_1, BorderLayout.CENTER);
				panel_1_1.setLayout(new GridLayout(4, 1, 0, 10));
				bgColorLabel = new JLabel("");
				panel_1_1.add(bgColorLabel);
				bgColorLabel.setOpaque(true);
				bgColorLabel.setBorder(BorderFactory.createLineBorder(Color.decode("#111111")));
				bgColorLabel.setBackground((Color) null);
				
										
				strokeColorLabel = new JLabel("");
				panel_1_1.add(strokeColorLabel);
				strokeColorLabel.setBackground(currentFillColor);
				strokeColorLabel.setOpaque(true);
				strokeColorLabel.setBorder(BorderFactory.createLineBorder(Color.decode("#111111")));
															
//				makeButtonMimicColorDialog(btnNewButton, strokeColorLabel);
																				
				fontColorLabel = new JLabel("");
				panel_1_1.add(fontColorLabel);
				fontColorLabel.setBackground(currentFontColor);
				fontColorLabel.setOpaque(true);
				fontColorLabel.setBorder(BorderFactory.createLineBorder(Color.decode("#111111")));
																									
//				makeButtonMimicColorDialog(btnNewButton_1, fontColorLabel);
				
				{
					JPanel panel_2 = new JPanel();
					panel_1_1.add(panel_2);
					panel_2.setBorder(null);
					panel_2.setLayout(new BorderLayout(0, 0));
					{
						fontLabel = new JLabel("XxYyZz");
						fontLabel.setHorizontalAlignment(SwingConstants.CENTER);
						fontLabel.setMinimumSize(fontLabel.getSize());
						fontLabel.setMaximumSize(fontLabel.getSize());
						fontLabel.setPreferredSize(fontLabel.getSize());
						panel_2.add(fontLabel, BorderLayout.CENTER);
					}
				}
			}
			{
				JPanel panel_1 = new JPanel();
				panel_1.setBorder(new EmptyBorder(0, 20, 0, 0));
				panel.add(panel_1, BorderLayout.EAST);
				panel_1.setLayout(new GridLayout(4, 1, 0, 10));
				{
					JPanel panel_1_1 = new JPanel();
					panel_1.add(panel_1_1);
					panel_1_1.setLayout(new BorderLayout(0, 0));
					{

						JButton bgColorButton = new JButton("Pick color");
						makeButtonMimicColorDialog(bgColorButton, bgColorLabel);
						panel_1_1.add(bgColorButton, BorderLayout.CENTER);
					}
				}
				{
					JPanel panel_1_1 = new JPanel();
					panel_1.add(panel_1_1);
					panel_1_1.setLayout(new BorderLayout(0, 0));

					JButton strokeColorButton = new JButton("Pick color");
					makeButtonMimicColorDialog(strokeColorButton, strokeColorLabel);
					panel_1_1.add(strokeColorButton, BorderLayout.CENTER);
				}
				{
					JPanel panel_1_1 = new JPanel();
					panel_1.add(panel_1_1);
					panel_1_1.setLayout(new BorderLayout(0, 0));

					JButton fontColorButton = new JButton("Pick color");
					makeButtonMimicColorDialog(fontColorButton, fontColorLabel);
					panel_1_1.add(fontColorButton, BorderLayout.CENTER);
				}
				JPanel panel_1_1 = new JPanel();
				panel_1.add(panel_1_1);
				panel_1_1.setLayout(new BorderLayout(0, 0));

				{
					JButton changeFontButton = new JButton("Change font");
					panel_1_1.add(changeFontButton, BorderLayout.CENTER);
					
					changeFontButton.addActionListener(e -> {
						FontChooserDialog fcd = new FontChooserDialog();
						fcd.setModal(true);
						fcd.setVisible(true);

						Font font = fcd.getSelectedFont();
						System.out.println("Selected font: " + font.getFontName());
						fontLabel.setFont(font);
						fontLabel.setText("XxYyZz");
					});
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
						// TODO: Fix this
//						fireEvent(new E3StyleEvent(
//								backgroundColorLabel.getBackground(),
//								fontColorLabel.getBackground(),
//								(Integer) spinner.getValue()
//								));
						
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
				System.out.println(label.getBackground());
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
	
	public static void main(String[] args) {
		E3StyleEditor se = new E3StyleEditor();
		se.setVisible(true);
	}
}
