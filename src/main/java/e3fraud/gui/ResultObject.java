/*
 * Copyright (C) 2016 Dan
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

import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author Dan
 */
public class ResultObject {
    public final int totalResults;
    public final int shownResults;
    public final DefaultMutableTreeNode root;

    public ResultObject(int totalResults, int shownResults, DefaultMutableTreeNode root) {
        this.totalResults = totalResults;
        this.shownResults = shownResults;
        this.root = root;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public int getShownResults() {
        return shownResults;
    }

    public DefaultMutableTreeNode getRoot() {
        return root;
    }
    
}
