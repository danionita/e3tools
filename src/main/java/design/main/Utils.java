/**
 * *****************************************************************************
 * Copyright (C) 2016 Bob Rubbens
 *
 *
 * This file is part of e3tool.
 *
 * e3tool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * e3tool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with e3tool.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************
 */
package design.main;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;

import design.main.Info.Base;
import design.main.Info.EndSignal;
import design.main.Info.LogicDot;
import design.main.Info.SignalDot;
import design.main.Info.StartSignal;
import design.main.Info.ValueExchange;
import design.main.Info.ValueExchangeLabel;
import design.main.Info.ValueInterface;
import design.main.Info.ValuePort;
import design.vocabulary.E3value;

public class Utils {
    public static boolean overlap(mxRectangle a, mxRectangle b) {
        if (a.getX() > b.getX()) {
            mxRectangle dummy = a;
            a = b;
            b = dummy;
        }

        if (a.getX() + a.getWidth() > b.getX()) {
            // Horizontal overlap
            if (a.getY() > b.getY()) {
                mxRectangle dummy = a;
                a = b;
                b = dummy;
            }

            if (a.getY() + a.getHeight() > b.getY()) {
                // And vertical overlap as well
                return true;
            }
        } else {
            return false;
        }

        return false;
    }

    /**
     * Returns true if inner is within outer
     *
     * @param inner
     * @param outer
     * @return
     */
    public static boolean within(mxRectangle inner, mxRectangle outer) {
        if (inner.getX() < outer.getX()) {
            return false;
        }

        if (inner.getX() + inner.getWidth() > outer.getX() + outer.getWidth()) {
            return false;
        }

        if (inner.getY() < outer.getY()) {
            return false;
        }

        if (inner.getY() + inner.getHeight() > outer.getY() + outer.getHeight()) {
            return false;
        }

        return true;
    }

    public static mxRectangle rect(mxGeometry gm) {
        return new mxRectangle(gm.getX(), gm.getY(), gm.getWidth(), gm.getHeight());
    }

    /**
     * Returns true if the given value interface or value port is situated on a
     * top-level actor. That is, it is not nested.
     *
     * @param cell
     * @return
     */
    public static boolean isToplevelValueInterface(mxGraph graph, mxICell cell) {
        Base value = Utils.base(graph, cell);

        if (value instanceof ValuePort) {
            return isToplevelValueInterface(graph, cell.getParent());
        } else if (value instanceof ValueInterface) {
            mxICell parent = cell.getParent();

            if (parent == null) {
                return false;
            }

            return parent.getParent() == graph.getDefaultParent();
        }

        return false;
    }

    /**
     * Gets the geometry object from a cell, copies it, and returns it.
     *
     * @param graph The graph the cell resides in
     * @param obj The cell the geometry should be copied from
     * @return A copy of the geometry object
     */
    public static mxGeometry geometry(mxGraph graph, Object obj) {
        mxGeometry gm = (mxGeometry) graph.getCellGeometry(obj);
        if (gm != null) {
            return (mxGeometry) gm.clone();
        } else {
            return null;
        }
    }

    /**
     * Gets the base object from a cell, copies it, and returns it.
     *
     * @param graph The graph the cell resides in
     * @param obj The cell the Base should be copied from
     * @return A deep copy of the Base object
     */
    public static Base base(mxGraph graph, Object obj) {
        Object value = graph.getModel().getValue(obj);
        if (value instanceof Base) {
            return ((Base) value).getCopy();
        }
        return null;
    }

    public static List<Object> getAllCells(mxGraph graph) {
        return getAllCells(graph, graph.getDefaultParent());
    }

    public static List<Object> getAllCells(mxGraph graph, Object parent) {
        List<Object> result = new ArrayList<>(Arrays.asList(mxGraphModel.getChildCells(graph.getModel(), parent, true, true)));
        List<Object> aggr = new ArrayList<>();

        for (Object cell : result) {
            aggr.addAll(getAllCells(graph, cell));
        }

        result.addAll(aggr);

        return result;
    }

    /**
     * Returns a copy of the Info.ValueExchange value of cell
     *
     * @param cell The cell of which to get the value
     * @return
     */
    public static ValueExchange getValueExchange(Object cell) {
        return getValueExchange(cell, true);
    }

    /**
     * Returns the Info.ValueExchange value of the cell.
     *
     * @param cell The cell of which to get the value
     * @param clone If true, a copy of the value is returned.
     * @return
     */
    public static ValueExchange getValueExchange(Object cell, boolean clone) {
        mxICell actualCell = (mxICell) cell;
        ValueExchange value = (ValueExchange) (actualCell.getValue());
        if (clone) {
            value = (ValueExchange) value.getCopy();
        }
        return value;
    }

    public static boolean isDotValue(Base value) {
        return value instanceof SignalDot || value instanceof LogicDot;
    }

    /**
     * Finds the child of edge valueExchangeEdge that is the "name" label.
     * Returns it if it's found, otherwise it returns null
     *
     * @param graph
     * @param valueExchangeEdge
     * @return
     */
    public static Object getValueExchangeNameLabel(mxGraph graph, Object valueExchangeEdge) {
        for (Object cell : Utils.getAllCells(graph, valueExchangeEdge)) {
            if (graph.getModel().getValue(cell) instanceof ValueExchangeLabel) {
                ValueExchangeLabel labelValue = (ValueExchangeLabel) graph.getModel().getValue(cell);
                if (!labelValue.isValueObjectLabel) {
                    return cell;
                }
            }
        }

        return null;
    }

    /**
     * Finds the child of the edge valueExchangeEdge that is the "valueObject"
     * label. Returns it if it's found, otherwise it returns null
     *
     * @param graph
     * @param valueExchangeEdge
     * @return
     */
    public static Object getValueExchangeValueObjectLabel(mxGraph graph, Object valueExchangeEdge) {
        for (Object cell : Utils.getAllCells(graph, valueExchangeEdge)) {
            if (graph.getModel().getValue(cell) instanceof ValueExchangeLabel) {
                ValueExchangeLabel labelValue = (ValueExchangeLabel) graph.getModel().getValue(cell);
                if (labelValue.isValueObjectLabel) {
                    return cell;
                }
            }
        }

        return null;
    }

    /**
     * Sets the visibility of a value exchange edge's name label, based on the
     * value exchange's value.labelHidden. If the name label is empty it is not
     * visible, and otherwise if it is hidden it is not visible.
     *
     * @param graph
     * @param valueExchangeEdge The edge of which the name label needs to be
     * shown/hidden
     */
    public static void setValueExchangeNameLabelVisibility(mxGraph graph, Object valueExchangeEdge) {
        Object nameCell = getValueExchangeNameLabel(graph, valueExchangeEdge);

        ValueExchange veValue = (ValueExchange) Utils.base(graph, valueExchangeEdge);
        ValueExchangeLabel nameLabelValue = (ValueExchangeLabel) Utils.base(graph, nameCell);

        graph.getModel().beginUpdate();
        try {
            if (nameLabelValue.name == null || nameLabelValue.name.trim().isEmpty()) {
                graph.getModel().setVisible(nameCell, false);
            } else {
                graph.getModel().setVisible(nameCell, !veValue.labelHidden);
            }
        } finally {
            graph.getModel().endUpdate();
        }
    }

    /**
     * Sets the visibility of a value exchange edge's valueobject label, based
     * on the value exchange's value.valueObjectHidden. If the valueObject label
     * is empty it is not visible, and otherwise if it is hidden it is not
     * visible.
     *
     * @param graph
     * @param valueExchangeEdge The edge of which the value object label needs
     * to be shown/hidden
     */
    public static void setValueExchangeValueObjectLabelVisibility(mxGraph graph, Object valueExchangeEdge) {
        Object valueObjectCell = getValueExchangeValueObjectLabel(graph, valueExchangeEdge);

        ValueExchange veValue = (ValueExchange) Utils.base(graph, valueExchangeEdge);
        ValueExchangeLabel valueObjectValue = (ValueExchangeLabel) Utils.base(graph, valueObjectCell);

        graph.getModel().beginUpdate();
        try {
            if (valueObjectValue.name == null || valueObjectValue.name.trim().isEmpty()) {
                graph.getModel().setVisible(valueObjectCell, false);
            } else {
                graph.getModel().setVisible(valueObjectCell, !veValue.valueObjectHidden);
            }
        } finally {
            graph.getModel().endUpdate();
        }
    }

    /**
     * Sets the name label of the valueExchangeEdge to the right text and gives
     * it the correct visibility based on labelHidden of valueExchangeEdge's
     * value
     *
     * @param graph
     * @param valueExchangeEdge
     */
    public static void updateValueExchangeNameLabel(mxGraph graph, Object valueExchangeEdge) {
        graph.getModel().beginUpdate();
        try {
            Object labelCell = Utils.getValueExchangeNameLabel(graph, valueExchangeEdge);

            assert (labelCell != null);

            ValueExchange veValue = (ValueExchange) Utils.base(graph, valueExchangeEdge);
            ValueExchangeLabel labelValue = (ValueExchangeLabel) Utils.base(graph, labelCell);

            labelValue.name = veValue.name;

            graph.getModel().setValue(labelCell, labelValue);

            Utils.setValueExchangeNameLabelVisibility(graph, valueExchangeEdge);
        } finally {
            graph.getModel().endUpdate();
        }
    }

    /**
     * Sets the valueObject label of the valueExchangeEdge to the right text and
     * gives it the correct visibility based on valueObjectHidden of
     * valueExchangeEdge's value
     *
     * @param graph
     * @param valueExchangeEdge
     */
    public static void updateValueExchangeValueObjectLabel(mxGraph graph, Object valueExchangeEdge) {
        graph.getModel().beginUpdate();
        try {
            Object valueObjectCell = Utils.getValueExchangeValueObjectLabel(graph, valueExchangeEdge);

            assert (valueObjectCell != null);

            ValueExchange veValue = (ValueExchange) Utils.base(graph, valueExchangeEdge);
            ValueExchangeLabel valueObjectValue = (ValueExchangeLabel) Utils.base(graph, valueObjectCell);

            valueObjectValue.name = veValue.valueObject;

            graph.getModel().setValue(valueObjectCell, valueObjectValue);

            Utils.setValueExchangeValueObjectLabelVisibility(graph, valueExchangeEdge);
        } finally {
            graph.getModel().endUpdate();
        }
    }

    public static String concatTail(String[] strings) {
        String result = "";
        for (int i = 1; i < strings.length; i++) {
            result += strings[i];
        }
        return result;
    }

    public static Object getOpposite(mxGraph graph, Object edge, Object terminal) {
        Object source = graph.getModel().getTerminal(edge, true);
        Object target = graph.getModel().getTerminal(edge, false);
        if (source == terminal) {
            return target;
        } else {
            return source;
        }
    }

    /**
     * StartSignals will be placed in left, endsignals will be placed in right.
     *
     * @author Bobe
     *
     */
    public static class EdgeAndSides {

        private EdgeAndSides() {
        }

        public static EdgeAndSides fromEdge(mxGraph graph, Object edge) {
            EdgeAndSides eas = new EdgeAndSides();

            eas.edge = edge;
            eas.edgeValue = (Base) graph.getModel().getValue(edge);

            eas.left = graph.getModel().getTerminal(edge, false);
            eas.left = graph.getModel().getParent(eas.left);
            eas.leftValue = (Base) graph.getModel().getValue(eas.left);

            eas.right = graph.getModel().getTerminal(edge, true);
            eas.right = graph.getModel().getParent(eas.right);
            eas.rightValue = (Base) graph.getModel().getValue(eas.right);

            if ((eas.rightValue instanceof StartSignal)
                    || (eas.leftValue instanceof EndSignal)) {
                Object t1;
                Base t2;

                t1 = eas.left;
                t2 = eas.leftValue;

                eas.left = eas.right;
                eas.leftValue = eas.rightValue;

                eas.right = t1;
                eas.rightValue = t2;
            }

            return eas;
        }

        public static EdgeAndSides fromParentSide(mxGraph graph, Object parent) {
            Object child = graph.getModel().getChildAt(parent, 0);
            Object edge = graph.getModel().getEdgeAt(child, 0);

            return EdgeAndSides.fromEdge(graph, edge);
        }

        public static EdgeAndSides fromDotSide(mxGraph graph, Object dot) {
            Object edge = graph.getModel().getEdgeAt(dot, 0);

            return EdgeAndSides.fromEdge(graph, edge);
        }

        /**
         * Checks if the first child of a node has an edge (corresponds to a
         * StartSignal having a connection-element.
         *
         * @param graph
         * @param obj
         * @return
         */
        public static boolean hasDotChildEdge(mxGraph graph, Object obj) {
            Object child = graph.getModel().getChildAt(obj, 0);
            assert (graph.getModel().getEdgeCount(child) < 2);
            return graph.getModel().getEdgeCount(child) == 1;
        }

        Object edge;
        Base edgeValue;

        Object left;
        Base leftValue;

        Object right;
        Base rightValue;
    }

    public static List<Object> getChildrenWithValue(mxGraph graph, Object parent, Class<?> c) {
        ArrayList<Object> children = new ArrayList<>();
        for (int i = 0; i < graph.getModel().getChildCount(parent); i++) {
            Object child = graph.getModel().getChildAt(parent, i);
            if (c.isInstance(graph.getModel().getValue(child))) {
                children.add(child);
            }
        }
        return children;
    }

    public static class ClosableTabHeading extends JPanel {

        public final String title;

        ClosableTabHeading(String title) {
            this.title = title;
        }
    }

    /**
     *
     * @param panes
     * @param title
     * @param component
     * @param icon Null if no icon is desired
     * @return
     */
    public static Component addClosableTab(JTabbedPane panes, String title, Component component, ImageIcon icon) {
        Component thisTab = panes.add(component);

        JPanel heading = new ClosableTabHeading(title);
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.X_AXIS));

        if (icon != null) {
            heading.add(new JLabel(icon));
        }

        heading.add(Box.createHorizontalStrut(5));

        JLabel label = new JLabel(title);
        heading.add(label);

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

        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                panes.remove(thisTab);
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

        heading.add(close);

        panes.setTabComponentAt(panes.indexOfComponent(thisTab), heading);

        return thisTab;
    }

    public static class GraphDelta {

        public List<Long> nonOccurringTransactions = new ArrayList<>();
        public List<long[]> hiddenTransactions = new ArrayList<>();
        public List<Long> colludedActors = new ArrayList<>();

        public GraphDelta(GraphDelta oldGraphDelta) {
            this.nonOccurringTransactions.addAll(nonOccurringTransactions);
            for (long[] item : hiddenTransactions) {
                this.hiddenTransactions.add(Arrays.copyOf(item, item.length));
            }
            this.colludedActors.addAll(colludedActors);
        }

        public void addNonOccurringTransaction(long id) {
            nonOccurringTransactions.add(id);
        }

        public void addHiddenTransaction(long from, long to) {
            hiddenTransactions.add(new long[]{from, to});
        }

        public void addColludedActor(long id) {
            colludedActors.add(id);
        }
    }

    public static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void openWebpage(URL url) {
        try {
            openWebpage(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Finds the next unused ID in a Jena/E3Value rdf model. Does not claim the ID by creating
     * a resource with the unused ID.
     * @param URIbase The base uri of the model
     * @param m The model to look in for used ID's
     * @return An unused ID.
     */
    public static long getUnusedID(String URIbase, Model m) {
    	long candidate = 1;
    	Resource candidateResource = ResourceFactory.createResource(URIbase + "#" + candidate);
    	System.out.println("Start looking");
    	while (m.contains(candidateResource, E3value.e3_has_uid, "" + candidate)) {
    		System.out.println("Contains " + candidate);
    		candidate++;
			candidateResource = ResourceFactory.createResource(URIbase + "#" + candidate);
    	}
    	
    	System.out.println("Result: " + candidate);
    	
    	return candidate;
    }
    
    public static void renewBasesAndIncreaseSUIDs(mxCell cell) {
		if (cell.getValue() instanceof Base) {
			cell.setValue(((Base) cell.getValue()).getCopy()); 
			((Base) cell.getValue()).setSUID(Info.getSUID());
		}
		
		for (int i = 0; i < cell.getChildCount(); i++) {
			renewBasesAndIncreaseSUIDs((mxCell) cell.getChildAt(i));
		}
    }
    
    public static void renewBasesAndIncreaseSUIDs(mxCell[] cells) {
    	for (mxCell cell : cells) {
    		if (cell.getValue() instanceof Base) {
    			renewBasesAndIncreaseSUIDs(cell);
    		}
    	}
    }
    
    public static void renewBasesAndIncreaseSUIDs(Object[] cells) {
    	renewBasesAndIncreaseSUIDs(Arrays.copyOf(cells, cells.length, mxCell[].class));
    }
}
