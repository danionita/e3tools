/*
 * Copyright (C) 2015 Dan
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.TreeCellRenderer;

/**
 * Code copied/adapted from BasicTreeUI source code.
 */
class CustomTreeUI extends BasicTreeUI {
    private int lastWidth;
    private boolean leftToRight;
    protected JTree tree;
  
    public CustomTreeUI() {
        super();
    }
  
    public void installUI(JComponent c) {
        if ( c == null ) {
            throw new NullPointerException("null component passed to " +
                                           "BasicTreeUI.installUI()" );
        }
        tree = (JTree)c;
        super.installUI(c);
    }
  
    protected void prepareForUIInstall() {
        super.prepareForUIInstall();
        leftToRight = tree.getComponentOrientation().isLeftToRight();
        lastWidth = tree.getParent().getWidth();
    }
  
    protected TreeCellRenderer createDefaultCellRenderer() {
        return new CustomTreeCellRenderer(tree);
    }
  
    protected AbstractLayoutCache.NodeDimensions createNodeDimensions() {
        return new NodeDimensionsHandler();
    }
  
    public class NodeDimensionsHandler extends AbstractLayoutCache.NodeDimensions {
        public Rectangle getNodeDimensions(Object value, int row, int depth,
                                           boolean expanded, Rectangle size) {
  
            // Return size of editing component, if editing and asking
            // for editing row.
            if(editingComponent != null && editingRow == row) {
                Dimension        prefSize = editingComponent.getPreferredSize();
                int              rh = getRowHeight();
  
                if(rh > 0 && rh != prefSize.height)
                    prefSize.height = rh;
                if(size != null) {
                    size.x = getRowX(row, depth);
                    size.width = prefSize.width;
                    size.height = prefSize.height;
                }
                else {
                    size = new Rectangle(getRowX(row, depth), 0,
                                   prefSize.width, prefSize.height);
                }
  
                if(!leftToRight) {
                    size.x = lastWidth - size.width - size.x - 2;
                }
                return size;
            }
            // Not editing, use renderer.
            if(currentCellRenderer != null) {
                Component        aComponent;
  
                aComponent = currentCellRenderer.getTreeCellRendererComponent
                    (tree, value, tree.isRowSelected(row),
                     expanded, treeModel.isLeaf(value), row,
                     false);
                if(tree != null) {
                    // Only ever removed when UI changes, this is OK!
                    rendererPane.add(aComponent);
                    aComponent.validate();
                }
                Dimension        prefSize = aComponent.getPreferredSize();
  
                if(size != null) {
                    size.x = getRowX(row, depth);
                    size.width = //prefSize.width;
                                 lastWidth - size.x; // <*** the only change
                    size.height = prefSize.height;
                }
                else {
                    size = new Rectangle(getRowX(row, depth), 0,
                                         prefSize.width, prefSize.height);
                }
  
                if(!leftToRight) {
                    size.x = lastWidth - size.width - size.x - 2;
                }
                return size;
            }
            return null;
        }
    }
}

