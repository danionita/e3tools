package design;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

/**
 * A tab that can only contain a model, but also has neat closing
 * and title editing capabilities. The title of the tab and of
 * the graph are always in sync. Only add tabs with the addClosableTab
 * function. This is because you cannot only actually "create" your own tab,
 * you can only indicate what a tab should contain and what its tab heading
 * should be. This class is effectivly the tab's heading, but it also manages
 * its contents (i.e. the title of the graph) to some extent.
 * @author Bobe
 *
 */
public class ModelTab extends JPanel {

    private ImageIcon icon;
    private JTabbedPane container;
    private JSplitPane component;
    private JLabel label;
	private JTextField inputBox;
	private JPanel headingText;

	/**
	 * Constructs a new ModelTab. The actual ModelTab instance
	 * is actually the thing that is added to the tab's "tab" part.
	 * @param container The JTabbedPane to which the ModelTab is added as a heading
	 * @param icon the icon to use
	 * @param component_ The model contained by the tab
	 */
    private ModelTab(JTabbedPane container, ImageIcon icon, Component component_) {
        this.icon = icon;
        this.component = (JSplitPane) component_;
        this.container = container;

        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        // Add an icon if it's not null
        if (icon != null) {
            add(new JLabel(icon));
        }

        // Horizontal spacing
        add(Box.createHorizontalStrut(5));

        // Create the "switchable" heading text
        label = new JLabel(getTitleOfModel());
        inputBox = new JTextField();
        headingText = new JPanel(new CardLayout());
        headingText.add(label, "label");
        headingText.add(inputBox, "input");
        headingText.setOpaque(false);
        add(headingText);
        
        // When focus is lost, the new title is extracted
        // And the textField will disappear to be replaced
        // with a label.
        inputBox.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				applyNewTitle();
			}
		});
        
        // Same as above happens when enter is pressed.
        inputBox.addKeyListener(new KeyAdapter() {
        	@Override
        	public void keyPressed(KeyEvent e) {
        		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
        			applyNewTitle();
        		}
        	}
		});

        // On double click, enter title editing mode.
        addMouseListener(new MouseAdapter() {
        	@Override
        	public void mouseClicked(MouseEvent e) {
        		if (e.getClickCount() == 2) {
        			inputBox.setText(getTitleOfModel());
        			((CardLayout) headingText.getLayout()).show(headingText, "input");
        			inputBox.requestFocusInWindow();
        		}
        	}
		});

        // Constructs a close button with a nice border.
        JLabel close = new JLabel("✖");
        Border border = close.getBorder();
        Border insideMargin = new EmptyBorder(2, 2, 2, 2);
        Border outsideMargin = new EmptyBorder(2, 6, 2, 0);
        Border noLineBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        Border lowerLineBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        Border raisedLineBorder = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

        Border normalBorder = new CompoundBorder(border, new CompoundBorder(outsideMargin, new CompoundBorder(noLineBorder, insideMargin)));
        Border hoverBorder = new CompoundBorder(border, new CompoundBorder(outsideMargin, new CompoundBorder(raisedLineBorder, insideMargin)));
        Border pressBorder = new CompoundBorder(border, new CompoundBorder(outsideMargin, new CompoundBorder(lowerLineBorder, insideMargin)));

        close.setBorder(normalBorder);

        // When close is clicked, remove the tab from the pane.
        // Otherwise, change the border when the mouse hovers over.
        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                container.remove(component);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                close.setBorder(pressBorder);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                close.setBorder(normalBorder);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                close.setBorder(hoverBorder);
            }
        });

        add(close);
    }
    
    /**
     * Uses a special Abstraction-Resistant shovel to dig through three layers
     * of abstraction and extract the title of the graph.
     * @return The title of the graph contained in this tab
     */
    private String getTitleOfModel() {
    	E3GraphComponent graphComponent = (E3GraphComponent) component.getRightComponent();
		E3Graph graph = (E3Graph) graphComponent.getGraph();
		return graph.title;
    }
    
    /**
     * Extracts the new title, sets it, and switches back to the label.
     */
    private void applyNewTitle() {
		setTitle(inputBox.getText());
		
		CardLayout cl = (CardLayout) headingText.getLayout();
		cl.show(headingText, "label");
    }

    /**
     * Sets the title of the heading and the title of the graph to title.
     * This action cannot be undone (meaning ctr+z won't undo this).
     * @param title
     */
    public void setTitle(String title) {
        label.setText(title);

    	E3GraphComponent graphComponent = (E3GraphComponent) component.getRightComponent();
		E3Graph graph = (E3Graph) graphComponent.getGraph();
		graph.title = title;
    }

	/**
	 * Adds a closable tab to views containing the component.
	 * @param views The JTabbedPane to add the tab to
	 * @param component The contained component. Can only be a JSplitPane
	 * 		  containing a toolcomponent and a graph component at the moment.
	 * @param icon Null if no icon is desired
	 */
	public static void addClosableTab(JTabbedPane views, JSplitPane component, ImageIcon icon) {
		views.add(component);
		JPanel heading = new ModelTab(views, icon, component);
		views.setTabComponentAt(views.indexOfComponent(component), heading);
	}
}