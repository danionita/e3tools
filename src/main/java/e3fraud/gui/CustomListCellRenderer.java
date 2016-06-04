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
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;

/**
 *
 * @author Dan
 */
class CustomListCellRenderer extends JPanel implements ListCellRenderer {

    private final Color selectionBackground;
    private final Color background;

    public CustomListCellRenderer(JList list) {
        selectionBackground = list.getSelectionBackground();
        background = list.getBackground();

        //setPreferredSize(new Dimension(15, 50));
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object object,
            int index, boolean isSelected, boolean cellHasFocus) {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
            
//        if (object instanceof String) {
//            JLabel label = new JLabel();
//            String separator = (String) object;
//            JLabel left = new JLabel("+");
//            //left.setFont(new Font("Arial", Font.BOLD, 14));
//            JTextArea right = new JTextArea(separator);
//            right.setFont(label.getFont().deriveFont(Font.BOLD,16));
//            
//            panel.add(left);
//            panel.add(right);      
//           
//        } else {
            E3Model model = (E3Model) object;            
            JLabel left = new JLabel(Integer.toString(index + 1));
            left.setFont(new Font("Arial", Font.BOLD, 14));
            JTextArea right = new JTextArea(model.getDescription());
            right.setLineWrap(true);
            right.setWrapStyleWord(true);
            panel.setBackground(isSelected ? selectionBackground : background);
            right.setBackground(isSelected ? selectionBackground : background);

            panel.add(left);
            panel.add(right);
            
            panel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY)); //OVERRIDEN (FIX!)
            panel.setBorder(BorderFactory.createEmptyBorder(0,15,0,0));
            
//        }
        return panel;
    }
    
    public static class ListSeparator {
    
}
}
