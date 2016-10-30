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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
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
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxGraph;

import design.export.RDFExport;
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
    	Set<Long> usedIDs = getAllCells(graph)
    		.stream()
    		.map(c -> graph.getModel().getValue(c))
    		.filter(Objects::nonNull)
    		.filter(v -> v instanceof Base)
    		.map(v -> (Base) v)
    		.map(v -> v.SUID)
    		.collect(Collectors.toSet())
    		;
    	
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
        
        System.out.println("Unused id: " + candidate);

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
    
    public static String getStyle(Object cell) {
    	return ((mxCell) cell).getStyle();
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
    public static boolean doValueAnalysis(E3Graph graph, File dstFile) {
		try {
			RDFExport export = new RDFExport(graph, true, true);
			String result = export.getResult().get();
			result = "<?xml version='1.0' encoding='ISO-8859-1'?>\n" + result;
			result = result.replaceAll("http://www\\.w3\\.org/1999/02/22_rdf_syntax_ns#", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
					
			System.out.println("\n\n\n\n\nInputting:\n" + result);
					
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
}
