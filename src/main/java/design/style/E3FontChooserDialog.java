package design.style;

import java.awt.Font;

import com.connectina.swing.FontChooser;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;

/**
 * A dialog containing a {@code FontChooser} as well as OK and
 * Cancel buttons. Copied in verbatim from Connectina's FontChooserDialog, except with
 * an extra constructor to set the initial font.
 * TODO: Pull request for this feature?
 *
 * @author Christos Bohoris
 */
public class E3FontChooserDialog extends JDialog {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3604371332093579813L;

	public E3FontChooserDialog(Font font) {
		initDialog(font);
	}
	
    public E3FontChooserDialog() {
        initDialog();
    }

    public E3FontChooserDialog(Frame owner) {
        super(owner);
        initDialog();
    }

    public E3FontChooserDialog(Frame owner, boolean modal) {
        super(owner, modal);
        initDialog();
    }

    public E3FontChooserDialog(Frame owner, String title) {
        super(owner, title);
        initDialog();
    }

    public E3FontChooserDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        initDialog();
    }

    public E3FontChooserDialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
        initDialog();
    }

    public E3FontChooserDialog(Dialog owner) {
        super(owner);
        initDialog();
    }

    public E3FontChooserDialog(Dialog owner, boolean modal) {
        super(owner, modal);
        initDialog();
    }

    public E3FontChooserDialog(Dialog owner, String title) {
        super(owner, title);
        initDialog();
    }

    public E3FontChooserDialog(Dialog owner, String title, boolean modal) {
        super(owner, title, modal);
        initDialog();
    }

    public E3FontChooserDialog(Dialog owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
        initDialog();
    }

    public E3FontChooserDialog(Window owner) {
        super(owner);
        initDialog();
    }

    public E3FontChooserDialog(Window owner, ModalityType modalityType) {
        super(owner, modalityType);
        initDialog();
    }

    public E3FontChooserDialog(Window owner, String title) {
        super(owner, title);
        initDialog();
    }

    public E3FontChooserDialog(Window owner, String title, ModalityType modalityType) {
        super(owner, title, modalityType);
        initDialog();
    }

    public E3FontChooserDialog(Window owner, String title, ModalityType modalityType, GraphicsConfiguration gc) {
        super(owner, title, modalityType, gc);
        initDialog();
    }

    private FontChooser chooser;
    private JButton cancelButton = new JButton();
    private JButton okButton = new JButton();
    private ResourceBundle bundle = ResourceBundle.getBundle("FontChooserDialog");
    private boolean cancelSelected;
    
    private void initDialog() {
    	initDialog(null);
    }

    private void initDialog(Font font) {
    	if (font == null) {
    		chooser = new FontChooser();
    	} else {
    		chooser = new FontChooser(font);
    	}
    	
        initComponents();
        getRootPane().setDefaultButton(okButton);
        okButton.requestFocusInWindow();

        cancelButton.addActionListener(event -> cancelSelected = true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelSelected = true;
            }
        });
    }

    private void initComponents() {

        JPanel chooserPanel = new JPanel();
        chooserPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 11));
        chooserPanel.setLayout(new BorderLayout(0, 12));
        chooserPanel.add(chooser, BorderLayout.CENTER);

        JPanel basePanel = new JPanel();
        basePanel.setLayout(new BorderLayout());
        basePanel.add(chooserPanel, BorderLayout.CENTER);
        getContentPane().add(basePanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setBorder(BorderFactory.createEmptyBorder(7, 7, 6, 6));
        controlPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
        basePanel.add(controlPanel, BorderLayout.PAGE_END);

        okButton.setMnemonic(bundle.getString("action.ok.mnemonic").charAt(0));
        okButton.setText(bundle.getString("action.ok"));
        okButton.addActionListener(event -> {
            dispose();
        });
        controlPanel.add(okButton);

        cancelButton.setMnemonic(bundle.getString("action.cancel.mnemonic").charAt(0));
        cancelButton.setText(bundle.getString("action.cancel"));
        cancelButton.addActionListener(event -> {
            cancelSelected = true;
            dispose();
        });
        controlPanel.add(cancelButton);

        pack();
    }

    public Font getSelectedFont() {
        return chooser.getSelectedFont();
    }

    public boolean isCancelSelected() {
        return cancelSelected;
    }
}
