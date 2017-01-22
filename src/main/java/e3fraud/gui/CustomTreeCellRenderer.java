/*
 * Copyright (C) 2015, 2016 Dan Ionita 
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
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

/**
 *
 * @author Dan
 */
class CustomTreeCellRenderer extends JPanel implements TreeCellRenderer {

    private final Color selectionBackground, background, text, selectedText,foreground;
    private final JTree tree;
    private int avaiableWidth,requiredHeight;
    private JLabel left;
    private JEditorPane right;
    E3Model model;
    public CustomTreeCellRenderer(JTree tree) {
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        selectionBackground = renderer.getBackgroundSelectionColor();
        background = renderer.getBackgroundNonSelectionColor();
        text = renderer.getTextNonSelectionColor();
        foreground = renderer.getForeground();
        selectedText = renderer.getTextSelectionColor();
        this.tree = tree;

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
             model = (E3Model) node.getUserObject();
            
             if(node.getLevel()==3){
                 left = new JLabel("");
             }
             else{
                 int nephews = 0;
                 DefaultMutableTreeNode sibling=node;
                 while(sibling.getPreviousSibling()!=null){
                 sibling = sibling.getPreviousSibling();
                 if(sibling.getChildCount()>0){
                 nephews += sibling.getChildAt(0).getChildCount();
                 }
                 }
             left = new JLabel(Integer.toString(node.getParent().getIndex(node) + nephews + 1));
             }

            left.setFont(new Font("Arial", Font.BOLD, 14));

            right = new JEditorPane();
            right.setContentType("text/html");
            right.setEditable(false);
            
            //set a fixed witdh 
           //JScrollPane parent = (JScrollPane)tree.getParent();
            avaiableWidth = tree.getParent().getWidth() - left.getPreferredSize().width-2; 
            right.setSize(avaiableWidth,Short.MAX_VALUE);
            right.setText(model.getPrefix()+"\n"+model.getDescription());

            
            //set the font of right to system default:
            Font font = UIManager.getFont("Tree.font");
            String bodyRule = "body { font-family: " + font.getFamily() + "; "
                    + "font-size: " + font.getSize() + "pt; color: green;}";
            ((HTMLDocument) right.getDocument()).getStyleSheet().addRule(bodyRule);

            //make the cells adopt the System L&F colors
            right.setBackground(selected ? selectionBackground : background);
            panel.setBackground(selected ? selectionBackground : background);
            left.setForeground(selected ? selectedText : text);
           
            //Do the same for the HTML box (right)
            StyledDocument doc = (StyledDocument) right.getDocument();
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setForeground(attrs, selected ? selectedText : text);
            StyleConstants.setBackground(attrs, selected ? selectionBackground : background);
            doc.setParagraphAttributes(0, doc.getLength(), attrs, false);
            right.setDocument(doc);
            
            //calculate right pane's required height based on its content and how wide the tree is
            requiredHeight = right.getPreferredSize().height;            
            right.setPreferredSize(new Dimension(avaiableWidth,requiredHeight));
            
            panel.add(left);
            panel.add(right);

            panel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            
            return panel;
        } else {
            DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
            Component c = renderer.getTreeCellRendererComponent(tree, object, false, expanded, leaf, row, hasFocus);
            if(!(node.getLevel()==2)){c.setFont(c.getFont().deriveFont(Font.BOLD, 16));}
            else{c.setFont(c.getFont().deriveFont(Font.ITALIC));}
            renderer.setLeafIcon(null);
            renderer.setClosedIcon(null);
            renderer.setIcon(null);
            renderer.setOpenIcon(null);
            return c;
        }
    }
    
    
    
    
}
