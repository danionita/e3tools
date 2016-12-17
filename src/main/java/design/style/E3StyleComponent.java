package design.style;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;

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
public class E3StyleComponent extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4812349709377569939L;

	private JLabel bgColorLabel;
	private JLabel strokeColorLabel;
	private JLabel fontColorLabel;
	private JLabel fontLabel;
	private JButton bgColorButton;

	/**
	 * Create the dialog.
	 */
	public E3StyleComponent(Color bgColor, Color strokeColor, Color fontColor, Font initialFont) {
		setBorder(new EmptyBorder(5, 5, 5, 5));

		setLayout(new BorderLayout(0, 0));
		{
			{

				{
					{
						JPanel panel_1_2 = new JPanel();
						panel_1_2.setBorder(new EmptyBorder(0, 0, 0, 20));
						add(panel_1_2, BorderLayout.WEST);
						panel_1_2.setLayout(new GridLayout(4, 1, 0, 10));
						{
							JLabel lblBackgroundColor = new JLabel("Background color");
							lblBackgroundColor.setLabelFor(lblBackgroundColor);
							panel_1_2.add(lblBackgroundColor);
						}
						{
							JLabel lblStrokeColor = new JLabel("Stroke color");
							panel_1_2.add(lblStrokeColor);
						}
						{
							JLabel lblFontColor = new JLabel("Font color");
							panel_1_2.add(lblFontColor);
						}
						{
							JLabel lblFont = new JLabel("Font");
							panel_1_2.add(lblFont);
						}
					}
					{
						JPanel panel_1_1_1 = new JPanel();
						add(panel_1_1_1);
						panel_1_1_1.setLayout(new GridLayout(4, 1, 0, 10));
						bgColorLabel = new JLabel("");
						panel_1_1_1.add(bgColorLabel);
						bgColorLabel.setOpaque(true);
						bgColorLabel.setBorder(BorderFactory.createLineBorder(Color.decode("#111111")));
						bgColorLabel.setBackground(bgColor);
						
												
						strokeColorLabel = new JLabel("");
						panel_1_1_1.add(strokeColorLabel);
						strokeColorLabel.setBackground(strokeColor);
						strokeColorLabel.setOpaque(true);
						strokeColorLabel.setBorder(BorderFactory.createLineBorder(Color.decode("#111111")));
																	
						fontColorLabel = new JLabel("");
						panel_1_1_1.add(fontColorLabel);
						fontColorLabel.setBackground(fontColor);
						fontColorLabel.setOpaque(true);
						fontColorLabel.setBorder(BorderFactory.createLineBorder(Color.decode("#111111")));
																											
						{
							JPanel panel_2 = new JPanel();
							panel_1_1_1.add(panel_2);
							panel_2.setBorder(null);
							panel_2.setLayout(new BorderLayout(0, 0));
							{
								fontLabel = new JLabel("XxYyZz");
								fontLabel.setHorizontalAlignment(SwingConstants.CENTER);
								fontLabel.setMinimumSize(fontLabel.getSize());
								fontLabel.setMaximumSize(fontLabel.getSize());
								fontLabel.setPreferredSize(fontLabel.getSize());
								fontLabel.setFont(initialFont);
								panel_2.add(fontLabel, BorderLayout.CENTER);
							}
						}
					}
					JPanel panel_1 = new JPanel();
					add(panel_1, BorderLayout.EAST);
					panel_1.setBorder(new EmptyBorder(0, 20, 0, 0));
					panel_1.setLayout(new GridLayout(4, 1, 0, 10));
					{
						JPanel panel_1_1_1 = new JPanel();
						panel_1.add(panel_1_1_1);
						panel_1_1_1.setLayout(new BorderLayout(0, 0));
						{

							bgColorButton = new JButton("Pick color");
							panel_1_1_1.add(bgColorButton, BorderLayout.CENTER);
							makeButtonMimicColorDialog(bgColorButton, bgColorLabel);
						}
					}
					{
						JPanel panel_1_1_1 = new JPanel();
						panel_1.add(panel_1_1_1);
						panel_1_1_1.setLayout(new BorderLayout(0, 0));

						JButton strokeColorButton = new JButton("Pick color");
						panel_1_1_1.add(strokeColorButton, BorderLayout.CENTER);
						makeButtonMimicColorDialog(strokeColorButton, strokeColorLabel);
					}
					{
						JPanel panel_1_1_1 = new JPanel();
						panel_1.add(panel_1_1_1);
						panel_1_1_1.setLayout(new BorderLayout(0, 0));

						JButton fontColorButton = new JButton("Pick color");
						panel_1_1_1.add(fontColorButton, BorderLayout.CENTER);
						makeButtonMimicColorDialog(fontColorButton, fontColorLabel);
					}
					JPanel panel_1_1 = new JPanel();
					panel_1.add(panel_1_1);
					panel_1_1.setLayout(new BorderLayout(0, 0));
					JButton changeFontButton = new JButton("Change font");
					panel_1_1.add(changeFontButton, BorderLayout.CENTER);
					
					changeFontButton.addActionListener(e -> {
						E3FontChooserDialog fcd = new E3FontChooserDialog(initialFont);
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
	}
	
	public Color getSelectedBgColor() {
		return bgColorLabel.getBackground();
	}
	
	public Color getSelectedStrokeColor() {
		return strokeColorLabel.getBackground();
	}
	
	public Color getSelectedFontColor() {
		return fontColorLabel.getBackground();
	}
	
	public Font getSelectedFont() {
		return fontLabel.getFont();
	}
	
	public void disableBG() {
		bgColorButton.setEnabled(false);
		bgColorLabel.setBackground(this.getBackground());
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
	
	public static void main(String[] args) {
		JFrame jf = new JFrame();
		E3StyleComponent se = new E3StyleComponent(
				new Color(255, 255, 255),
				new Color(0, 255, 0),
				new Color(0, 0, 255),
				new Font("Calibri", Font.PLAIN, 12)
				);
		jf.getContentPane().add(se, BorderLayout.CENTER);
		jf.setSize(800, 600);
		se.disableBG();
		jf.setVisible(true);
		
	}
}
