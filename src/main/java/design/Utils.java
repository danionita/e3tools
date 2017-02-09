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
 * *****************************************************************************
 */
package design;

import java.awt.Color;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.e3value.eval.ncf.E3ParseException;
import com.e3value.eval.ncf.ProfGenerator;
import com.e3value.eval.ncf.ontology.model;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;

import design.checker.E3Checker;
import design.checker.ModelError;
import design.export.RDFExport;
import design.export.RDFExport.VTMode;
import design.info.Actor;
import design.info.Base;
import design.info.ConnectionElement;
import design.info.EndSignal;
import design.info.LogicDot;
import design.info.MarketSegment;
import design.info.SignalDot;
import design.info.StartSignal;
import design.info.ValueActivity;
import design.info.ValueExchange;
import design.info.ValueInterface;
import design.info.ValuePort;
import design.info.ValueTransaction;
import e3fraud.tools.currentTime;
import e3fraud.vocabulary.E3value;

public class Utils {
    private static JFileChooser previousFc;
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
     * @param cell The cell object to check. Can be either value interface or value port.
     * @return
     */
    public static boolean isToplevelValueInterface(mxGraph graph, Object cell) {
        Base value = Utils.base(graph, cell);

        if (value instanceof ValuePort) {
            return isToplevelValueInterface(graph, graph.getModel().getParent(cell));
        } else if (value instanceof ValueInterface) {
            Object parent = graph.getModel().getParent(cell);

            if (parent == null) {
                return false;
            }

            return graph.getModel().getParent(parent) == graph.getDefaultParent();
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
     * Returns null if the value of the cell is not instance of Base.
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
    	if (graph == null) return new ArrayList<>();
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

    public static String concatTail(String[] strings) {
        String result = "";
        for (int i = 1; i < strings.length; i++) {
            result += strings[i];
        }
        return result;
    }

    /**
     * Returns the vertex from edge that is not equal to terminal.
     * @param graph
     * @param edge
     * @param terminal
     * @return vertex from edge =/= terminal
     */
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
    
    public static List<Object> getChildren(mxGraph graph, Object parent) {
    	return getChildrenWithValue(graph, parent, Object.class);
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

    public static class GraphDelta {

        public List<Long> nonOccurringTransactions = new ArrayList<>();
        public List<long[]> hiddenTransactions = new ArrayList<>();
        public List<Long> colludedActors = new ArrayList<>();        
        public List<Double> hiddenTransferValues = new ArrayList<>();

        public GraphDelta(GraphDelta oldGraphDelta) {
            if (oldGraphDelta != null) {
                if (oldGraphDelta.nonOccurringTransactions != null) {
                    this.nonOccurringTransactions.addAll(oldGraphDelta.nonOccurringTransactions);
                }
                for (long[] item : oldGraphDelta.hiddenTransactions) {
                    this.hiddenTransactions.add(Arrays.copyOf(item, item.length));
                }
                if (oldGraphDelta.colludedActors != null) {
                    this.colludedActors.addAll(oldGraphDelta.colludedActors);
                }                
                if (oldGraphDelta.hiddenTransferValues != null) {
                    this.hiddenTransferValues .addAll(oldGraphDelta.hiddenTransferValues );
                }
            }
        }

        public GraphDelta() {
            nonOccurringTransactions = new ArrayList<>();
            hiddenTransactions = new ArrayList<>();
            colludedActors = new ArrayList<>();
        }

        public void addNonOccurringTransaction(long id) {
            this.nonOccurringTransactions.add(id);
        }

        public void addHiddenTransaction(long from, long to) {
            this.hiddenTransactions.add(new long[]{from, to});
        }        
        public void addHiddenTransaction(long from, long to, double value) {
            this.hiddenTransactions.add(new long[]{from, to});
            this.hiddenTransferValues.add(value);
        }        

        public void addColludedActor(long id) {
            this.colludedActors.add(id);
            //System.out.println("Adding collusion to changes");
        }
        
        /**
         * 
         * @return a simple measure of complexity. Non-occuring transactions are weighted more and collusion is weighted the least.
         */
        public int getComplexity(){
            return nonOccurringTransactions.size()*3+hiddenTransactions.size()*2+colludedActors.size();
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
     * Finds the next unused ID in a Jena/E3Value rdf model. Does not claim the
     * ID by creating a resource with the unused ID.
     *
     * @param URIbase The base uri of the model
     * @param m The model to look in for used ID's
     * @return An unused ID.
     */
    public static long getUnusedID(String URIbase, Model m) {
        long candidate = 1;
        Resource candidateResource = ResourceFactory.createResource(URIbase + "#" + candidate);

        while (m.contains(candidateResource, E3value.e3_has_uid, "" + candidate)) {
            candidate++;
            candidateResource = ResourceFactory.createResource(URIbase + "#" + candidate);
        }

        return candidate;
    }
    
    public static long getUnusedID(mxGraph graph) {
    	// @Optimize this can be optimized. It's basically
    	// a fold over all ID's with the max operator.
    	Set<Long> usedIDs = getAllCells(graph)
    		.stream()
    		.map(c -> graph.getModel().getValue(c))
    		.filter(Objects::nonNull)
    		.filter(v -> v instanceof Base)
    		.map(v -> (Base) v)
    		.map(v -> v.SUID)
    		.collect(Collectors.toSet())
    		;
    	
    	if (graph instanceof E3Graph) {
    		E3Graph e3graph = (E3Graph) graph;
    		
    		Set<Long> vtIDs = e3graph.valueTransactions.stream()
    				.map(vtInfo -> vtInfo.SUID)
    				.collect(Collectors.toSet());
    		
    		usedIDs.addAll(vtIDs);
    	}
    	
    	long nextID = 0;
    	while (usedIDs.contains(nextID)) nextID++;
    	
    	return nextID;
    }
    
    public static long getUnusedID(mxGraph graph, String URIbase, Model m) {
    	Set<Long> usedIDs = getAllCells(graph)
    		.stream()
    		.map(c -> graph.getModel().getValue(c))
    		.filter(Objects::nonNull)
    		.filter(v -> v instanceof Base)
    		.map(v -> (Base) v)
    		.map(v -> v.SUID)
    		.collect(Collectors.toSet())
    		;
    	
        long candidate = 0;
        Resource candidateResource = ResourceFactory.createResource(URIbase + candidate);

        while (m.contains(candidateResource, E3value.e3_has_uid, "" + candidate)
        		|| usedIDs.contains(candidate)) {
            candidate++;
            candidateResource = ResourceFactory.createResource(URIbase + candidate);
        }
        
        //System.out.println("Unused id: " + candidate);

        return candidate;
    }
    
    public static long getMaxID(mxGraph graph) {
    	Optional<Long> maxID = getAllCells(graph)
    		.stream()
    		.map(c -> graph.getModel().getValue(c))
    		.filter(Objects::nonNull)
    		.filter(v -> v instanceof Base)
    		.map(v -> (Base) v)
    		.map(v -> v.SUID)
    		.reduce(Math::max)
    		;
    	
    	if (maxID.isPresent()) {
    		return maxID.get();
    	} else {
    		return 0;
    	}
    }

    /**
     * Replaces the id's of all the cells
     * in cells with id's starting from maxID + 1
     * @author Bobe
     *
     */
	public static class IDReplacer {
		private long maxID;
		mxGraph graph;
		
		public IDReplacer(mxGraph graph) {
			this.graph = graph;
			this.maxID = Utils.getMaxID(graph);
		}
		
		public void renewBases(mxCell cell) {
			// Make sure the copied cell has a unique SUID and name
			// based on its type
			if (cell.getValue() instanceof Base) {
				Base newValue = (Base) cell.getValue();
				newValue = newValue.getCopy();
				maxID++;
				newValue.SUID = maxID;
				newValue.name = newValue.getClass().getSimpleName() + maxID;
				
				// To make sure the generated name is actually unique
				int i = 1;
				Set<String> usedNames = new HashSet<String>(Utils.getAllNames(graph));
				while (usedNames.contains(newValue.name)) {
					newValue.name = newValue.getClass().getSimpleName() + (maxID + i);
					i++;
				}
				
				cell.setValue(newValue); 
			}
			
			// Make sure this holds for its children too
			for (int i = 0; i < cell.getChildCount(); i++) {
				renewBases((mxCell) cell.getChildAt(i));
			}
		}
		
		public void renewBases(Object obj) {
			renewBases((mxCell) obj);
		}
		
		public void renewBases(Object[] cells) {
			for (Object cell : cells) {
				renewBases((mxCell) cell);
			}
		}
	}

    public static Optional<E3Graph> openFile(JFrame mainFrame, JFileChooser fc) {
        int returnVal = fc.showOpenDialog(mainFrame);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();

            Optional<E3Graph> result = GraphIO.loadGraph(file.getAbsolutePath());

            if (!result.isPresent()) {
                JOptionPane.showMessageDialog(
                        mainFrame,
                        "Error during file loading. Please make sure the file destination is accesible.",
                        "Loading error",
                        JOptionPane.ERROR_MESSAGE);
                
                return Optional.empty();
            }

            // Set original file
            result.get().file = file;
            
            // Make sure it shows up in the open recent list
            Utils.addRecentlyOpened(file);

            return result;
        } else {
            System.out.println(currentTime.currentTime() + " Open command cancelled by user.");
        }

        return Optional.empty();
    }

    /**
     * Saves the graph to the file selected by the user. If something goes wrong
     * it shows an error or confirmation message.
     * @param mainFrame
     * @param graph
     * @return True on success, false on failure.
     */
    public static boolean saveAs(JFrame mainFrame, E3Graph graph) {
    	JFileChooser fc = getE3FileChooser();
    	
        int returnVal = fc.showSaveDialog(mainFrame);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();

            String extension = "";
            int i = file.getName().lastIndexOf('.');
            if (i > 0) {
                extension = file.getName().substring(i + 1);
            }

            if (!extension.equals("e3")) {
                String fileWithExtension = file + ".e3";
                file = new File(fileWithExtension);
            }

            if (file.exists()) {
                int result = JOptionPane.showConfirmDialog(
                        mainFrame,
                        fc.getSelectedFile() + " already exists. Would you like to overwrite it?",
                        "File already exists",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (result != JOptionPane.OK_OPTION) {
                    System.out.println(currentTime.currentTime() + " Save command cancelled by user.");
                    return false;
                }

                file.delete();
            }

            return saveToFile(mainFrame, graph, file);
        } else {
            System.out.println(currentTime.currentTime() + " Save command cancelled by user.");
        }
        
        return false;
    }

    /**
     * Saves a graph to the file specified by file. If an error occurs
     * it shows an error dialog.
     *
     * @param mainFrame
     * @param graph
     * @param file
     * @return True on success, false on failure.
     */
    public static boolean saveToFile(JFrame mainFrame, E3Graph graph, File file) {
        try {
            GraphIO.saveGraph(graph, file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Error during file saving. Please make sure the file destination is accesible.",
                    "Saving error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        graph.file = file;
        System.out.println(currentTime.currentTime() + " Saved: " + file.getName() + ".");

        return true;
    }
    
    public static JFileChooser getE3FileChooser() {
        if (previousFc == null){
			JFileChooser fc = new JFileChooser();
			FileFilter e3Filter = new FileNameExtensionFilter("e3tool file", "e3");
			fc.addChoosableFileFilter(e3Filter);
			fc.setFileFilter(e3Filter);
			previousFc = fc;
        }     

		
		return previousFc;
    }
    
    public static String colorToHex(Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
    
    public static Optional<String> readInternal(String path) {
		try {
			String contents = mxUtils.readInputStream(E3Style.class.getResourceAsStream(path));
			return Optional.of(contents);
		} catch (IOException e) {
			e.printStackTrace();
			return Optional.empty();
		}
    }
    
    public static Optional<String> readExternal(String path) {
    	try {
    		String contents = mxUtils.readInputStream(new FileInputStream(new File(path)));
    		return Optional.of(contents);
    	} catch (IOException e) {
    		e.printStackTrace();
    		return Optional.empty();
    	}
    }
    
    public static void update(mxGraph graph, Runnable runnable) {
    	graph.getModel().beginUpdate();
    	try {
    		runnable.run();
    	} finally {
    		graph.getModel().endUpdate();
    	}
    }
    
    /**
     * Concatenates every element of strings with the systems default path separator 
     * inbetween.
     * @param strings
     * @return
     */
    public static String makePath(String...strings) {
    	if (strings.length < 1) return "";
    	
    	// I would use Stream::reduce here but it's actually not commutative
    	// nor associative!
    	
    	String result = strings[0];
    	for (int i = 1; i < strings.length; i++) {
    		result += FileSystems.getDefault().getSeparator() + strings[i];
    	}
    	
    	return result;
    }
    
    public static class IsEntityFilter {
    	public static Predicate<Object> forGraph(E3Graph graph) {
    		return (obj -> {
					Object value = graph.getModel().getValue(obj);
					return value instanceof MarketSegment
							|| value instanceof Actor
							|| value instanceof ValueActivity;
				});
    	}

		private Object graph;
    	
    	private IsEntityFilter(E3Graph graph) {
    		this.graph = graph;
    	}
    }
    
    public static void setCellsDefaultStyles(E3Graph graph) {
		Utils.update(graph, () -> {
			Utils.getAllCells(graph)
				.stream()
				.forEach(o -> {
					Object val = graph.getModel().getValue(o);
					
					Object[] cells = new Object[]{o};
					
					if (val instanceof MarketSegment) {
						graph.setCellStyle("MarketSegment", cells);
					} else if (val instanceof Actor) {
						Actor actor = (Actor) val;
						if (actor.colluded) {
							graph.setCellStyle("ColludedActor", cells);
						} else {
							graph.setCellStyle("Actor", cells);
						}
					} else if (val instanceof ValueActivity) {
						graph.setCellStyle("ValueActivity", cells);
					} else if (val instanceof ValueExchange) {
						graph.setCellStyle("ValueExchange");
					} else if (val instanceof ConnectionElement) {
						graph.setCellStyle("ConnectionElement");
					}
				});
		}); 
    }
    
    /**
     * Gets all the names from a graph
     */
    public static List<String> getAllNames(mxGraph graph) {
    	return Utils.getAllCells(graph).stream()
    		.map(graph.getModel()::getValue)
    		.filter(o -> o instanceof Base)
    		.map(o -> (Base) o)
    		.map(o -> o.name)
    		.collect(Collectors.toList())
    		;
    }
    
    /**
     * Returns whether or not a random cell from an e3graph is colluding.
     */
    public static boolean isEntityColluding(E3Graph graph, Object cell) {
    	Object info = graph.getModel().getValue(cell);
    	
    	if (info instanceof Actor) {
			Actor actor = (Actor) info;
    		return actor.colluded;
    	} else if (info instanceof MarketSegment) {
			MarketSegment marketSegment = (MarketSegment) info;
    		return marketSegment.colluded;
    	}
    	
    	return false;
    }
    
    /**
     * Does an NCF analysis and writes the resulting excel file to
     * dstfile. Returns true if successful, false if *anything* went
     * wrong.
     * @param graph
     * @param dstFile
     * @return
     */
    public static boolean doNCFAnalysis(E3Graph graph, File dstFile) {
		try {
			RDFExport export = new RDFExport(graph, false, VTMode.DERIVE_ORPHANED, false);
			String result = export.getResult().get();
					
			InputStream stream = new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8));

			ProfGenerator p = new ProfGenerator();
			p.loadRDFStream(stream);

			Iterator i = p.getMapObjects().values().iterator();
			int found_models = 0;

			while (i.hasNext()) {
				Object o = i.next();
				if (o instanceof model) {
					found_models++;
					if (found_models > 1) {
						throw new E3ParseException("RDF file should contain exactly one 'model'");
					}
					p.setMymodel((model) o);
				}
			}

			String destinationFileName = dstFile.getAbsolutePath();
			p.storeXLS(destinationFileName, true, true, true, true, true, true,
					true, true, true, true, true);
			
			return true;
		} catch (Throwable t) {
			System.err.println(t);
			t.printStackTrace();

			return false;
		}	
	}
    
    /**
     * Return all recently opened files that exist.
     * @return
     */
    public static List<String> getRecentlyOpenedFiles() {
    	List<String> files = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(Main.e3RecentFilesFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				File recentFile = new File(line);
				if (!recentFile.exists()) continue;
				files.add(line);
			}
		} catch (FileNotFoundException e) {
			// That's ok - just an empty list then
		} catch (IOException e) {
			// That's ok - just an empty list then
		}
		
		return files;
    }
    
    /**
     * Adds an absolute path to the list of recently opened files.
     * Makes sure only unique entries end up in the list, and that the list
     * doesn't grow beyond ten.
     * @param absolutePath
     */
    public static void addRecentlyOpened(File recentFile) {
    	// We use linkedhashset here because it preserves ordering
    	String absolutePath = recentFile.getAbsolutePath();

    	LinkedHashSet<String> filenames = new LinkedHashSet<>(getRecentlyOpenedFiles());
    	filenames.remove(absolutePath);
    	List<String> filenamesFinal = new ArrayList<>(filenames);
    	filenamesFinal.add(0, absolutePath);
    	if (filenamesFinal.size() >= 10) {
			filenamesFinal.subList(0, 10);
    	}

    	try {
			Files.write(
					Main.e3RecentFilesFile.toPath(),
					filenamesFinal, 
					StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING
					);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    }
    
    /**
     * Three cases:
     * - Containers of VP's (the MS/AC/VA to which the VP belongs) are both top level.
     *   In this case:
     *   	- outgoing for VI = incoming for ve
     *   	- incoming for VI = outgoing for ve
     * - Either of the containers is the child of the other container.
     *   In this case:
     *   	- If both VP's are outgoing the VP in the child container is incoming 
     *   	- If both VP's are incoming the VP in the parent container is incoming
     * - Both containers are contained in the same MS/AC/VE.
     *   This case is the same as case 1.
     *   
     * This class performs this logic on a VE, and puts the VP's in the right inVP/outVP vars.
     * The "in" vp is always the port which looks like it's going "into" the ve. Conversely, the "out"
     * vp is coming "out" of the vp.
     * 
     * @author bobe
     *
     */
    public static class VEConnection {
    	public final E3Graph graph;
    	public final Object ve;
    	private Object inVP;
    	private Object outVP;

    	public VEConnection(E3Graph graph, Object ve) {
    		this.graph = graph;
    		this.ve = ve;
    		this.inVP = null;
    		this.outVP = null;

    		Object vp1 = graph.getModel().getTerminal(ve, true);
    		Object vp2 = graph.getModel().getTerminal(ve, false);
    		
    		Object vp1Container = graph.getContainerOfChild(vp1);
    		Object vp2Container = graph.getContainerOfChild(vp2);
    		
    		ValuePort vp1Info = (ValuePort) graph.getModel().getValue(vp1);
    		
    		if (vp1Container == vp2Container) {
    			// Case 1 & 3
    			if (!vp1Info.incoming) {
    				inVP = vp1;
    				outVP = vp2;
    			} else {
    				outVP = vp1;
    				inVP = vp2;
    			}
    		} else {
    			// Case 2
    			if (graph.isParentOf(vp1Container, vp2Container)) {
    				if (!vp1Info.incoming) {
    					inVP = vp2;
    					outVP = vp1;
    				} else {
    					inVP = vp1;
    					outVP = vp2;
    				}
    			} else {
    				if (!vp1Info.incoming) {
    					inVP = vp1;
    					outVP = vp2;
    				} else {
    					inVP = vp2;
    					outVP = vp1;
    				}
    			}
    		}
    	}
    	
    	public Object getInVP() {
    		return inVP;
    	}
    	
    	public Object getOutVP() {
    		return outVP;
    	}
    }
    
    /**
     * Parses the cell style string from jgraphx into a map. A stylename is a key value pair where
     * the key is the style name, and the val
     * 
     * @param cellStyle A string of form [(stylename|key=value);] . Commonly used
     * by jgraphx for internal cell styles.
     * @return 
     */
    public static class CellStyle {
    	public static class Entry {
    		/**
    		 * 	If true the entry is a style name. If false, it is a key value pair.
    		 */
    		private boolean isStyleName;
    		
    		private String styleName;
    		private String key;
    		private String value;
    		
    		Entry(String key, String value) {
    			this.key = key;
    			this.value = value;
    			
    			isStyleName = false;
    		}
    		
    		Entry (String styleName) {
    			this.styleName = styleName;
    			
    			isStyleName = true;
    		}
    		
    		String getStyleName() {
    			if (!isStyleName) {
    				throw new IllegalStateException("StyleName of entry is requested, while entry is a key-value pair.");
    			}
    			
    			return styleName;
    		}
    		
    		String getKey() {
    			if (isStyleName) {
    				throw new IllegalStateException("Key of entry is requested, while entry is a stylename");
    			}
    			
    			return key;
    		}
    		
    		String getValue() {
    			if (isStyleName) {
    				throw new IllegalStateException("Value of entry is requested, while entry is a stylename");
    			}

    			return value;
    		}
    		
    		boolean isStyleName() {
    			return isStyleName;
    		}
    		
    		boolean isKeyValuePair() {
    			return !isStyleName;
    		}
    		
    		public void setKeyValue(String key, String value) {
    			this.key = key;
    			this.value = value;
    			
    			isStyleName = false;
    		}
    		
    		public void setStyleName(String styleName) {
    			this.styleName = styleName;
    			
    			isStyleName = true;
    		}
    		
    		@Override
    		public String toString() {
    			if (isStyleName) {
    				return styleName;
    			} else {
    				return key + "=" + value;
    			}
    		}
    	}
    	
    	private List<Entry> data;
    	
    	CellStyle(String style) {
    		data = new ArrayList<>();
    		
    		String[] parts = style.split(";");
    		for (String part : parts) {
    			if (part.contains(";")) continue;
    			
    			if (part.contains("=")) {
    				String[] pair = part.split("=");
    				data.add(new Entry(pair[0], pair[1]));
    			} else {
    				data.add(new Entry(part));
    			}
    		}
    	}
    	
    	/**
    	 * See {@link List#size()}
    	 * @return
    	 */
    	public int size() {
    		return data.size();
    	}
    	
    	/**
    	 * See {@link List#add(Object)}
    	 * @param entry
    	 * @return
    	 */
    	public boolean add(Entry entry) {
    		return data.add(entry);
    	}
    	
    	/**
    	 * See {@link List#add(int, Object)}
    	 * @param entry
    	 * @param index
    	 */
    	public void add(Entry entry, int index) {
    		data.add(index, entry);
    	}
    	
    	/**
    	 * See {@link List#get(int)}
    	 * @param i
    	 * @return
    	 */
    	public Entry get(int i) {
    		return data.get(i);
    	}
    	
    	/**
    	 * See {@link List#remove(int)}
    	 * @param i
    	 * @return
    	 */
    	public Entry remove(int i) {
    		return data.remove(i);
    	}
    	
    	/**
    	 * @param styleName The stylename to look for
    	 * @return True if the style is present in the cell style
    	 */
    	public boolean containsStyle(String styleName) {
    		return data.stream()
    				.filter(Entry::isStyleName)
    				.map(Entry::getStyleName)
    				.anyMatch(styleName::equals);
    	}
    	
    	/**
    	 * Removes every occurrence of stylename from the cellstyle
    	 * @param styleName
    	 */
    	public void removeStyle(String styleName) {
    		data = data.stream()
    				.filter(entry -> {
    					// If entry is a stylename...
    					if (entry.isStyleName) {
    						// Keep it if it is not equal to stylename
    						return !entry.getStyleName().equals(styleName);
    					}
    					
    					// Else keep it
    					return true;
    				})
    				.collect(Collectors.toList());
    	}
    	
    	public void applyStyle(E3Graph graph, Object cell) {
    		Utils.update(graph, () -> {
				graph.getModel().setStyle(cell, toString());
    		});
    	}
    	
    	/**
    	 * Prints the jgraphx textual representation of a cell style
    	 */
    	@Override
    	public String toString() {
    		if (data.size() > 0) {
				return data.stream()
					.map(Entry::toString)
					.reduce((l, r) -> l + ";" + r)
					.get();
    		} else {
    			return "";
    		}
    	}
    }
    
    public static Optional<Object> getTopLevelParent(E3Graph graph, Object cell) {
    	if (cell == graph.getDefaultParent()) return Optional.empty();
    	if (graph.getModel().getParent(cell) == graph.getDefaultParent()) return Optional.empty();
    	
    	Object possibleTopLevelParent = graph.getModel().getParent(cell);
    	while (graph.getModel().getParent(possibleTopLevelParent) != graph.getDefaultParent()) {
    		possibleTopLevelParent = graph.getModel().getParent(possibleTopLevelParent);
    	}
    	
    	return Optional.of(possibleTopLevelParent);
    }
    
    public static boolean resetCellStateProperty(E3Graph graph, Object cell, String prop) {
		// Set it to its original style
    	Map<String, Object> originalCellStyle = graph.getCellStyle(cell);
    	
    	// If null, the cell does not exist anymore
    	if (originalCellStyle == null) return false;

		Object originalValue = originalCellStyle.get(prop);

    	// If null, the cell does not exist anymore
		mxCellState cs = graph.getView().getState(cell);
		if (cs == null) return false;

		if (originalValue == null) {
			// There was not a value before, so we remove it
			cs.getStyle().remove(prop);
		} else {
			cs.getStyle().put(prop, originalValue);
		}
		
		return true;
    }
    
    public static boolean setCellStateProperty(E3Graph graph, Object cell, String prop, Object value) {
    	mxCellState state = graph.getView().getState(cell);
    	
    	if (state == null) return false;
    		
    	if (state.getStyle() == null) return false;
    	
    	Map<String, Object> style = state.getStyle();
    	
    	style.put(prop, value);
    	
    	return true;
    }
    
    /**
     * Checks if the model contains any errors. If so, asks the user to either:
     * - Ignore them
     * - Stop
     * - Stop and go to the model checker
     * @param graph The graph to check
     * @return true if the caller can continue, false if the caller should abort
     */
    public static boolean doModelCheck(E3Graph graph, Main main) {
    	List<ModelError> errors = E3Checker.checkForErrors(graph);
    	
    	if (errors.size() > 0) {
    		// Ask the user
    		// The captions of the buttons in the dialog. Left to right
    		String[] opts = new String[]{"Open model checker", "Cancel", "Ignore"};

    		int choice = JOptionPane.showOptionDialog(
    				Main.mainFrame,
    				"The model contains " + errors.size() + " error"
    						+ (errors.size() > 1 ? "s" : "") + ". "
    						+ "Would you like to go to the model checker to inspect them, "
    						+ "stop, or ignore them? Ignoring the errors can cause erratic behavior.", 
    				"Errors detected in model", 
    				JOptionPane.YES_NO_CANCEL_OPTION, 
    				JOptionPane.WARNING_MESSAGE, 
    				null, 
    				opts,
    				// Model checker is selected by default
    				opts[0]
    				);

    		// If the user picked model checker, go to the model checker
    		if (choice == 0) {
    			new EditorActions.ModelCheck(main).actionPerformed(graph);
    			return false;
    		} else if (choice == 2) {
    			// if the user picked ignore, carry on!
    			return true;
    		} else { // Choice == 1 || Choice == -1. Cancel!
    			//  If the user picked cancel, abort!
    			return false;
    		}
    	}
    	
    	return true;
    }
    
    public static void addChangeListener(JTextField field, Consumer<DocumentEvent> c) {
		field.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				exec(e);
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				exec(e);
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				exec(e);
			}
			
			public void exec(DocumentEvent e) {
				c.accept(e);
			}
		});
    }

	public static void removeHighlight(E3Graph graph) {
		Utils.getAllCells(graph).stream()
			.forEach(ve -> {
				Utils.resetCellStateProperty(graph, ve, mxConstants.STYLE_STROKECOLOR);
				Utils.resetCellStateProperty(graph, ve, mxConstants.STYLE_STROKEWIDTH);
			});

		graph.repaint();
	}

	public static void highlight(E3Graph graph, ValueTransaction vt, String highlightColor, int width) {
		Utils.getAllCells(graph).parallelStream()
			.filter(cell -> {
				Base info = (Base) graph.getModel().getValue(cell);
				if (info == null) return false;

				return vt.exchanges.contains(info.SUID);
			})
			.forEach(ve -> {
				Utils.setCellStateProperty(graph, ve, mxConstants.STYLE_STROKECOLOR, highlightColor);
				Utils.setCellStateProperty(graph, ve, mxConstants.STYLE_STROKEWIDTH, width);
			});
		
		graph.repaint();
	}
	
	public static void highlight(E3Graph graph, Base info, String highlightColor, int width) {
		Utils.getAllCells(graph).parallelStream()
			.filter(cell -> graph.getModel().getValue(cell) == info)
			.findFirst()
			.ifPresent(cell -> {
				Utils.setCellStateProperty(graph, cell, mxConstants.STYLE_STROKECOLOR, highlightColor);
				Utils.setCellStateProperty(graph, cell, mxConstants.STYLE_STROKEWIDTH, width);
			});
	}
	
	public static void highlight(E3Graph graph, long id, String highlightColor, int width) {
		Utils.getAllCells(graph).parallelStream()
			.filter(cell -> {
				Object val = graph.getModel().getValue(cell);
				if (val instanceof Base) {
					return ((Base) val).SUID == id;
				}
				
				return false;
			})
			.findFirst()
			.ifPresent(cell -> {
				Utils.setCellStateProperty(graph, cell, mxConstants.STYLE_STROKECOLOR, highlightColor);
				Utils.setCellStateProperty(graph, cell, mxConstants.STYLE_STROKEWIDTH, width);
			});
	}
	
}
