/*******************************************************************************
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
 *******************************************************************************/
package design;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.io.mxCodec;
import com.mxgraph.shape.mxStencilShape;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;

import design.info.Actor;
import design.info.Base;
import design.info.MarketSegment;
import design.info.ValueActivity;
import design.info.ValueInterface;

/**
 * Class that contains and can apply a style to a graph.
 * 
 * ----- Format of an E3Editor style
 * A folder with the following files:
 * - style.xml - The mxStyleSheet with a few extensions. See below
 * - marketsegment_template.shape - A shape file for the market segment. It is special as well. See below.
 * - startsignal.shape - Start signal shape file
 * - endsignal.shape - End signal shape file
 * - valueport.shape - Value port shape file
 * - note.shape - Note shape file
 * - northtriangle.shape - Triangle pointing up shape file (For or gate)
 * - easttriangle.shape - Triangle pointing east shape file
 * - southtriangle.shape - Triangle pointing south shape file
 * - westtriangle.shape - Triangle pointing west shape file
 * - bar.shape - Shape file of a black bar. Used for the and gate
 * - dot.shape - Dot shape used for dot-like things (start/end signal, value interface dots)
 * 
 * Each file must be present in the folder or the theme will be invalid.
 * 
 * ----- Format of the style.xml file -----
 * It is  basically an mxStylesheet from jgraphx with a few extras.
 * 
 * - There is a name tag which is the name of the style. This should be the
 *   same as the folder name. It cannot contain spaces.
 * - There is a background tag. This contains the background color of the editor.
 * - There is a grid tag. This contains true or false, indicating whether the
 *   grid should be shown or not.
 * - Throughout the xml file you can use {!name}. This will be search-replaced upon
 *   loading the xml file with the name in the name tag plus a nonce. This is to
 *   prevent name clashes in mxGraph's internal style management, more specifically
 *   w.r.t. stencils. If you want to refer to a stencil within the xml, use:
 * 
 *   <code>{!name}_NameOfYourStencil</code>
 * 
 *   Where NameOfYourStencil is defined in the shape file itself.
 *   
 * ----- Format of the marketSegment_template.shape file -----
 * The marketsegment shape file is mostly a shape file. It has however two special parts:
 * the {!postfix} and {!bg_color} words. Upon loading the marketsegment shape, these two
 * words will be replaced with (most likely) uppercase hexadecimal colors. That way a specialized stencil is
 * loaded for each color needed. This is because it is (afaik) impossible to style a stencil
 * with just style.xml. Probably because fore- and backgroundcolor are defined in the
 * shape file itself. (Sometimes it does seem to be possible to some extent;
 * but this is not reliable enough for our purposes. See: https://github.com/jgraph/jgraphx/issues/60).
 * 
 * ----- General remarks -----
 * If you want something to be egally colored, make sure you set both the
 * fillColor attribute and the gradientColor attribute (both in the xml and the shape).
 * Apparently jgraphx always has gradientColor set to some shade of blue, so if you don't
 * set it the same as your fillColor it'll look messy.
 * 
 * @author Bobe
 *
 */
public class E3Style {
	// Just added an option to toggle the grid. This changes both showGrid & the XML, and does some juggling
	// to keep the vars doc & xml in sync.
	// TODO: Right now this is the easiest way, but I don't like this. Somehow you have to keep both the flags
	// in the XML and the member variables in sync... Everything should just be inferred from the initially parsed
	// node always. If any options are added to the style class all the settings (showing grid and others)
	// should just be derived from the XML.
	// That way the getGrid() functions and the state of e3style are always in sync, and it's easy to serialize
	// the style (just turn the doc var into XML).
		
	
	public static final double DOTRADIUS = 4;
	public static int idCounter = 0;
	// Must be kept in same order as constructor!
	public static List<String> requiredFiles = Arrays.asList(
		"style.xml",
		"marketsegment_template.shape",
		"startsignal.shape",
		"endsignal.shape",
		"valueport.shape",
		"note.shape",
		"northtriangle.shape",
		"easttriangle.shape",
		"southtriangle.shape",
		"westtriangle.shape",
		"bar.shape",
		"dot.shape"
		);

	public String xml;
	public String marketSegment_template;
	public String startSignal;
	public String endSignal;
	public String valuePort;
	public String note;
	public String northTriangle;
	public String eastTriangle;
	public String southTriangle;
	public String westTriangle;
	public String bar;
	public String dot;

	private Document doc;
	private String name;
	private Color backgroundColor;
	private boolean showGrid;
	private final int ID = idCounter++;
	
	/**
	 * Loads a style from local resources. Given a name, tries to
	 * look for /styles/name/style.xml. Returns empty upon error.
	 */
	public static Optional<E3Style> loadInternal(String name) {
		// Get all the files
		List<String> files = requiredFiles
			.stream()
			.map(f -> Utils.readInternal("/styles/" + name + "/" + f))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList());
		
		// If one if them failed return empty
		if (files.size() != 12) {
			return Optional.empty();
		}
		
		// TODO: This looks ugly!
		// Construct and return the style
		return Optional.of(new E3Style(
				files.get(0),
				files.get(1),
				files.get(2),
				files.get(3),
				files.get(4),
				files.get(5),
				files.get(6),
				files.get(7),
				files.get(8),
				files.get(9),
				files.get(10),
				files.get(11)
				));
	}
	
	/**
	 * Loads a style located in folder indicated by file. If an error
	 * occurs, returns empty.
	 */
	public static Optional<E3Style> load(File file) {
		List<String> files = E3Style.requiredFiles.stream()
			.map(entry -> Utils.readExternal(Utils.makePath(file.getPath(), entry)))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList());
		
		// If one if them failed return empty
		if (files.size() != 12) {
			return Optional.empty();
		}
		
		// Construct and return the style
		return Optional.of(new E3Style(
				files.get(0),
				files.get(1),
				files.get(2),
				files.get(3),
				files.get(4),
				files.get(5),
				files.get(6),
				files.get(7),
				files.get(8),
				files.get(9),
				files.get(10),
				files.get(11)
				));
	}
	
	public static Optional<E3Style> load(String name) {
		File file = new File(Utils.makePath(Main.e3styleDir.getPath(), name));
		//System.out.println("Checking: " + file.getPath());
		if (file.exists() && file.isDirectory()) {
			return load(file);
		} else {
			return loadInternal(name);
		}
	}
	
	private void parseXmlWithNameSubstitution(String xml) {
		// Parse the xml
		doc = mxXmlUtils.parseXml(xml);
		
		// Get the name of the style
		// TODO: Error handling here as well
		name = doc
				.getDocumentElement()
				.getElementsByTagName("name")
				.item(0)
				.getTextContent();
		name = name + "_" + ID;
		
		// Apply the naming subsitution in the XML
		doc = mxXmlUtils.parseXml(xml.replace("{!name}", name));
	}
	
	private void parseXmlWithNameSubstitution(String xml) {
		// Parse the xml
		doc = mxXmlUtils.parseXml(xml);
		
		// Get the name of the style
		// TODO: Error handling here as well
		name = doc
				.getDocumentElement()
				.getElementsByTagName("name")
				.item(0)
				.getTextContent();
		name = name + "_" + ID;
		
		// Apply the naming subsitution in the XML
		doc = mxXmlUtils.parseXml(xml.replace("{!name}", name));
	}
	
	public E3Style(
			String xml,
			String marketSegment_template,
			String startSignal,
			String endSignal,
			String valuePort,
			String note,
			String northTriangle,
			String eastTriangle,
			String southTriangle,
			String westTriangle,
			String bar,
			String dot) {
		// Store all the strings
		// Need them for saving the theme
		this.xml = xml;
		this.marketSegment_template = marketSegment_template;
		this.startSignal = startSignal;
		this.endSignal = endSignal;
		this.valuePort = valuePort;
		this.note = note;
		this.northTriangle = northTriangle;
		this.eastTriangle = eastTriangle;
		this.southTriangle = southTriangle;
		this.westTriangle = westTriangle;
		this.bar = bar;
		this.dot = dot;
		
//		// Parse the xml
//		doc = mxXmlUtils.parseXml(xml);
//		
//		// Get the name of the style
//		// TODO: Error handling here as well
//		name = doc
//				.getDocumentElement()
//				.getElementsByTagName("name")
//				.item(0)
//				.getTextContent();
//		name = name + "_" + ID;
//		
//		// Apply the naming subsitution in the XML
//		doc = mxXmlUtils.parseXml(xml.replace("{!name}", name));
		
		parseXmlWithNameSubstitution(xml);

		// Get the rest of the info
		backgroundColor = Color.decode(doc
				.getDocumentElement()
				.getElementsByTagName("background")
				.item(0)
				.getTextContent());
		
		showGrid = doc
				.getDocumentElement()
				.getElementsByTagName("grid")
				.item(0)
				.getTextContent()
				.equals("true");
		
		// Get the market segment color from the xml
		// TODO: Maybe factor this into a function?
		String marketSegmentColor = "#C0C0C0";
		{
			NodeList nl = doc.getDocumentElement().getChildNodes();
			
			Node n = null;
			for (int i = 0; i < nl.getLength(); i++) {
				Node candidate = nl.item(i);
				if (!candidate.getNodeName().equals("add")) continue;
				
				String as = candidate
						.getAttributes()
						.getNamedItem("as")
						.getTextContent();
				
				if (as.equals("MarketSegment")) {
					n = nl.item(i);
					break;
				}
			}
			
			if (n != null) {
				nl = n.getChildNodes();
				
				for (int i = 0; i < nl.getLength(); i++) {
					Node candidate = nl.item(i);
					
					if (!candidate.getNodeName().equals("add")) continue;
					
					String as = candidate
							.getAttributes()
							.getNamedItem("as")
							.getTextContent();
					
					if (as.equals("fillColor")) {
						n = nl.item(i);
					}
				}
				
				if (n != null) {
					marketSegmentColor = n.getAttributes().getNamedItem("value").getTextContent();
				}
			}
		}
		
		// TODO: Fall back to default style somehow here and show an error box
		// (if there is a space in the name - spaces are a recipe for disaster
		if (name.contains(" ")) {
			//System.out.println("Error: name of style contains spaces!");
			return;
		}
		
		// Add all the stencils
		addStringStencil(name + "_", startSignal);
		addStringStencil(name + "_", endSignal);
		addStringStencil(name + "_", valuePort);
		addStringStencil(name + "_", note);
		addStringStencil(name + "_", northTriangle);
		addStringStencil(name + "_", eastTriangle);
		addStringStencil(name + "_", southTriangle);
		addStringStencil(name + "_", westTriangle);
		addStringStencil(name + "_", bar);
		addStringStencil(name + "_", dot);
		addMarketSegmentColor("", marketSegmentColor); 
	}

	/**
	 * Adds a market segment stencil with color hexColot to mxGraph's stencil registry.
	 * @param hexColor
	 */
	public void addMarketSegmentColor(String hexColor) {
		addMarketSegmentColor(hexColor.toUpperCase(), hexColor.toUpperCase());
	}

	public void addMarketSegmentColor(String postfix, String hexColor) {
		// Cast every hex color to upper case, that way the names are consistent
		hexColor = hexColor.toUpperCase();
		
		// Put the postfix and hexcolor in there
		String marketSegmentXML = marketSegment_template
				.replace("{!postfix}", postfix)
				.replace("{!bg_color}", hexColor);
		
		// Add it
		addStringStencil(name + "_", marketSegmentXML);
	}
	
	/**
	 * Styles a graph component with this style, and removes all actor/marketsegment/
	 * valueactivity specific styling.
	 * @param graphComponent
	 */
	public void styleGraphComponent(mxGraphComponent graphComponent) {
		//System.out.println("Styling graph with " + name);
		
		mxGraph graph = graphComponent.getGraph();
		
		// Apply the style
		mxCodec codec = new mxCodec();
		graph.setStylesheet(new mxStylesheet());

		if (doc != null) {
			codec.decode(doc.getDocumentElement(), graph.getStylesheet());
		} else {
			//System.out.println("Failed loading style");
			return;
		}
		
		// Set the editor-specific settings
		graphComponent.getViewport().setOpaque(true);
		graphComponent.getViewport().setBackground(backgroundColor);
		graphComponent.setGridVisible(showGrid);
		
		// To get rid of the folding icon
		graphComponent.setFoldingEnabled(false);
		
		// Refresh to propagate changes
		graph.refresh();
	}
	
	// Keeps track of all the names of the stencils that are loaded
	public static Set<String> loadedStencils = new HashSet<>();
	
	/**
	 * Adds the stencil in xmlString. The name of the stencil
	 * will be identical to the name in the xml.
	 * @param xmlString
	 */
	public static void addStringStencil(String xmlString) {
		// Find first occurrence of < to avoid unicode BOM
		int lessThanIndex = xmlString.indexOf("<");
		if (lessThanIndex != -1) {
			xmlString = xmlString.substring(lessThanIndex);
		}
		mxStencilShape newShape = new mxStencilShape(xmlString);

		addStringStencil("", xmlString);
	}
	
	/**
	 * Add the stencil in the xml string. The in-editor
	 * name of the stencil will be identical to the
	 * concatenation of prefix and the name in xmlString.
	 * @param prefix
	 * @param xmlString
	 */
	public static void addStringStencil(String prefix, String xmlString) {
		// Find first occurrence of < to avoid unicode BOM
		int lessThanIndex = xmlString.indexOf("<");
		if (lessThanIndex != -1) {
			xmlString = xmlString.substring(lessThanIndex);
		}
		mxStencilShape newShape = new mxStencilShape(xmlString);
		
		String name = prefix + newShape.getName();
		
		// Don't add it if we already know that name
		if (loadedStencils.contains(name)) return;

		//System.out.println("Adding " + name);

		// Add it to the known names list
		loadedStencils.add(name);
		
		// Add it to mxGraph
		mxGraphics2DCanvas.putShape(name, newShape);
	}
	
	/**
	 * This returns a shape name to be used with mxConstants.STYLE_SHAPE. Example:
	 * 
	 * <code>
	 * model.beginUpdate();
	 * try {
	 * 		String backgroundColor = "#FF0000";
	 * 		graphComponent.style.addMarketSegmentColor(backgroundColor);
	 * 		graph.setCellStyles(mxConstants.STYLE_SHAPE, graphComponent.style.getMarketSegmentShapeName(backgroundColor), new Object[]{aCell});
	 * } finally {
	 * 		model.endUpdate();
	 * }
	 * </code>
	 * 
	 * Should "Just Work (TM)".
	 * 
	 * @param hexColor
	 * @return
	 */
	public String getMarketSegmentShapeName(String hexColor) {
		return name + "_MarketSegmentStencil" + hexColor.toUpperCase();
	}
	
	/**
	 * Returns a list of all themes available in both local resources and
	 * whatever's located in the e3editor settings folder on the current pc.
	 */
	public static List<String> getAvailableThemes() {
		// Included with editor
		List<String> candidates = new ArrayList<String>(Arrays.asList(
				"E3Style",
				"E3Mono"
				));
		
		File[] files = Main.e3styleDir.listFiles();
		for (File file : files) {
			if (!file.isDirectory()) continue;
			
			File[] dirFiles = file.listFiles();
			if (Arrays.asList(dirFiles).stream()
				.filter(dirFile -> dirFile.getName().equals("style.xml"))
				.count() == 1) {
				candidates.add(file.getName());
			}
		}
		
		return candidates;
	}
	
	/**
	 * True if the grid should be shown according to this style, false if not.
	 * @return
	 */
	public boolean getGrid() {
		return showGrid;
	}
	
	/**
	 * Sets whether or not the grid should be shown according to this style.
	 * Does not apply the new style to any graph. Use {@link #styleGraphComponent(mxGraphComponent)}.
	 * @param newGrid
	 */
	public void setGrid(boolean newGrid) {
		showGrid = newGrid;
		
		Document doc = mxXmlUtils.parseXml(xml);

		Node gridNode = doc
			.getDocumentElement()
			.getElementsByTagName("grid")
			.item(0);

		if (newGrid) {
			gridNode.setTextContent("true");
		} else {
			gridNode.setTextContent("false");
		}
		
		// Apply changes and make sure they are saved properly
		xml = mxXmlUtils.getXml(doc.getDocumentElement());
		parseXmlWithNameSubstitution(xml);
	}
	
	/**
	 * Toggles whether or not the grid should be shown according to the current style.
	 * Does not apply the new style to any graph. Use {@link #styleGraphComponent(mxGraphComponent)}.
	 */
	public void toggleGrid() {
		setGrid(!getGrid());
	}
}
