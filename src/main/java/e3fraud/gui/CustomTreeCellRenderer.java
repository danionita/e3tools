/*
 * Copyright (C) 2015 Dan Ionita 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package e3fraud.gui;

import e3fraud.model.E3Model;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.text.html.HTMLDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

/**
 *
 * @author Dan
 */
class CustomTreeCellRenderer extends JPanel implements TreeCellRenderer {

    private final Color selectionBackground;
    private final Color background;
    private final JTree tree;

    public CustomTreeCellRenderer(JTree tree) {
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        selectionBackground = renderer.getBackgroundSelectionColor();
        background = renderer.getBackgroundNonSelectionColor();
        this.tree = tree;

        //setPreferredSize(new Dimension(15, 50));
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree,
            Object object,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
        if (node.getUserObject() instanceof E3Model) {
            E3Model model = (E3Model) node.getUserObject();
            JLabel left = new JLabel(Integer.toString(node.getParent().getIndex(node) + 1));
            left.setFont(new Font("Arial", Font.BOLD, 14));
            //JLabel right = new JLabel(model.getDescription());
            //right.setLineWrap(true);
            //right.setWrapStyleWord(true);

            JEditorPane right = new JEditorPane("text/html", model.getDescription());
            right.setEditable(false);

            //set the font of right to system defaul:
            Font font = UIManager.getFont("Label.font");
            String bodyRule = "body { font-family: " + font.getFamily() + "; "
                    + "font-size: " + font.getSize() + "pt; }";
            ((HTMLDocument) right.getDocument()).getStyleSheet().addRule(bodyRule);

            Dimension size = right.getPreferredSize();
            int availableWidth= this.tree.getParent().getWidth() - left.getPreferredSize().width - 20; //( 20 extra to leave room for the scrollbar and border)           
            //if the cell exceeds the available size, when it will be resize, text will have to wraped, thus creating a new line
            if(size.width>availableWidth){
               size.height+=16; // so we need to make room for that new line.
            }            
            size.width=availableWidth;
            right.setPreferredSize(size);

            panel.setBackground(selected ? selectionBackground : background);
            right.setBackground(selected ? selectionBackground : background);
            panel.add(left);
            panel.add(right);

            panel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            return panel;
        } else {
            DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
            Component c = renderer.getTreeCellRendererComponent(tree, object, leaf, leaf, leaf, row, leaf);
            c.setFont(c.getFont().deriveFont(Font.BOLD, 16));
            return c;
        }
    }
}
