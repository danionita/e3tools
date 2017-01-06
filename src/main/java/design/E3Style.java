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
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;

import design.style.E3StyleEvent;
import design.style.Element;

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
	public static final double DOTRADIUS 						= 4;

	public static final String BASE_STYLE 						= "baseStyle";
	public static final String VALUE_ACTIVITY 					= "ValueActivity";
	public static final String ACTOR 							= "Actor";
	public static final String COLLUDED_ACTOR 					= "ColludedActor";
	public static final String MARKET_SEGMENT 					= "MarketSegment";
	public static final String COLLUDED_MARKET_SEGMENT 			= "ColludedMarketSegment";
	public static final String VALUE_INTERFACE 					= "ValueInterface";
	public static final String VALUE_PORT_WEST 					= "ValuePortWest";
	public static final String VALUE_PORT_EAST 					= "ValuePortEast";
	public static final String VALUE_PORT_NORTH 				= "ValuePortNorth";
	public static final String VALUE_PORT_SOUTH 				= "ValuePortSouth";
	public static final String VALUE_EXCHANGE 					= "ValueExchange";
	public static final String NON_OCCURRING_VALUE_EXCHANGE 	= "NonOccurringValueExchange";
	public static final String HIDDEN_VALUE_EXCHANGE 			= "HiddenValueExchange";
	public static final String CONNECTION_ELEMENT 				= "ConnectionElement";
	public static final String START_SIGNAL 					= "StartSignal";
	public static final String END_SIGNAL 						= "EndSignal";
	public static final String DOT 								= "Dot";
	public static final String BAR 								= "Bar";
	public static final String LOGIC_BASE 						= "LogicBase";
	public static final String SOUTH_TRIANGLE 					= "SouthTriangle";
	public static final String WEST_TRIANGLE 					= "WestTriangle";
	public static final String NORTH_TRIANGLE 					= "NorthTriangle";
	public static final String EAST_TRIANGLE 					= "EastTriangle";
	public static final String NAME_TEXT 						= "NameText";
	public static final String NOTE 							= "Note";
	
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
	
	/**
	 * Gets the name of the style (without postfix)
	 * @return
	 */
	public String getName() {
		String name = doc
				.getDocumentElement()
				.getElementsByTagName("name")
				.item(0)
				.getTextContent();
		
		return name;
	}
	
	/**
	 * Gets the name of the style postfixed with a (session unique) identifier.
	 * @return
	 */
	public String getUniqueName() {
		return getName() + "_" + ID;
	}
	
	public Document getXmlDocForJGraphX() {
		// Find the name
		String name = getUniqueName();
		
		// Apply the naming subsitution in the XML and turn it back into a document
		return mxXmlUtils.parseXml(getXML().replace("{!name}", name));
	}
	
	public String getXML() {
		return mxXmlUtils.getXml(doc);
	}
	
	public E3Style(E3Style other) {
		this(
				other.getXML(),
				other.marketSegment_template,
				other.startSignal,
				other.endSignal,
				other.valuePort,
				other.note,
				other.northTriangle,
				other.eastTriangle,
				other.southTriangle,
				other.westTriangle,
				other.bar,
				other.dot
				);
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
		
		doc = mxXmlUtils.parseXml(xml);
		
//		parseXmlWithNameSubstitution(xml);

		// Get the rest of the info
		
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
					
					if (as.equals(mxConstants.STYLE_FILLCOLOR)) {
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
		if (getUniqueName().contains(" ")) {
			//System.out.println("Error: name of style contains spaces!");
			return;
		}
		
		// Add all the stencils
		String name = getUniqueName();
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
	
	public Color getModelBackgroundColor() {
		Color backgroundColor;
		
		try {
			backgroundColor = Color.decode(
					doc
						.getDocumentElement()
						.getElementsByTagName("background")
						.item(0)
						.getTextContent()
				);
		} catch (NumberFormatException ex) {
			backgroundColor = Color.BLACK;
		}
		
		return backgroundColor;
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
		addStringStencil(getUniqueName() + "_", marketSegmentXML);
	}
	
	/**
	 * Styles a graph component with this style, and removes all actor/marketsegment/
	 * valueactivity specific styling.
	 * @param graphComponent
	 */
	public void styleGraphComponent(mxGraphComponent graphComponent) {
		// System.out.println("Styling graph with " + name);
		
		mxGraph graph = graphComponent.getGraph();
		
		// Apply the style
		mxCodec codec = new mxCodec();
		graph.setStylesheet(new mxStylesheet());

		codec.decode(getXmlDocForJGraphX().getDocumentElement(), graph.getStylesheet());
		
		// Set the editor-specific settings
		graphComponent.getViewport().setOpaque(true);
		graphComponent.getViewport().setBackground(getModelBackgroundColor());
		
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
		return getUniqueName() + "_MarketSegmentStencil" + hexColor.toUpperCase();
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
	
	public Optional<Node> getAddFromNodeList(NodeList nl, String asValue) {
		// Find the XML node with as="element"
		Optional<Node> targetNodeOptional = Optional.empty();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			NamedNodeMap nnm = node.getAttributes();
			
			if (nnm == null) {
				continue;
			}

			if (nnm.getNamedItem("as") == null) {
				continue;
			}
			
			Node asNode = nnm.getNamedItem("as");
			if (!asNode.getTextContent().equals(asValue)) {
				continue;
			}
			
			targetNodeOptional = Optional.of(node);
			break;
		}
		
		return targetNodeOptional;
	}
	
	public Optional<Node> getElementStyle(String element) {
		System.out.println("XML:\n\n" + getXML());
		
		NodeList nl = doc // .getDocumentElement()
			.getElementsByTagName("mxStylesheet")
			.item(0)
			.getChildNodes();
		
		return getAddFromNodeList(nl, element);
	}
	
	public Optional<String> getAttribute(Node node, String attribute) {
		NamedNodeMap nnm = node.getAttributes();
		
		if (nnm == null) {
			return Optional.empty();
		}

		if (nnm.getNamedItem(attribute) == null) {
			return Optional.empty();
		}
		
		return Optional.of(nnm.getNamedItem(attribute).getTextContent());
	}
	
	public Optional<Node> getPropertyOfElement(String element, String property, boolean recurse) {
		Optional<Node> elementStyleOptional = getElementStyle(element);
		if (!elementStyleOptional.isPresent()) {
			return Optional.empty();
		}
		
		Node elementStyle = elementStyleOptional.get();
		
		Optional<Node> addOptional = getAddFromNodeList(elementStyle.getChildNodes(), property);
		
		if (addOptional.isPresent()) {
			return addOptional;
		}
		
		Optional<String> baseStyleOptional = getAttribute(elementStyle, "extend");
		if (baseStyleOptional.isPresent() && recurse) {
			return getPropertyOfElement(baseStyleOptional.get(), property, recurse);
		}
		
		return Optional.empty();
	}
	
	public Optional<Node> getOrCreatePropertyOfElement(String element, String property, String defaultValue) {
		Optional<Node> elementStyleOptional = getElementStyle(element);
		
		if (!elementStyleOptional.isPresent()) {
			return Optional.empty();
		}
		
		Node elementStyle = elementStyleOptional.get();
		
		Optional<Node> addOptional = getAddFromNodeList(elementStyle.getChildNodes(), property);
		
		if (addOptional.isPresent()) {
			return addOptional;
		}
		
		org.w3c.dom.Element node = doc.createElement("add");
		node.setAttribute("as", property);
		node.setAttribute("value", defaultValue);
		elementStyle.appendChild(node);

		return Optional.of(node);
	}
	
	public Optional<String> getValueOfPropertyOfElement(String element, String property, boolean recurse) {
		Optional<Node> propertyNodeOptional = getPropertyOfElement(element, property, recurse);
		
		if (!propertyNodeOptional.isPresent()) {
			return Optional.empty();
		}
		
		Node propertyNode = propertyNodeOptional.get();
		
		return getAttribute(propertyNode, "value");
	}
	
	private Optional<Color> getPropertyAsColor(String element, String property) {
		Optional<String> stringProperty = getValueOfPropertyOfElement(element, property, true);
		
		if (stringProperty.isPresent()) {
			try {
				return Optional.of(Color.decode(stringProperty.get()));
			} catch (NumberFormatException ex) {
				return Optional.empty();
			}
		} 
		
		return Optional.empty();
	}
	
	private void setElementPropertyValue(String element, String property, String value) {
		Optional<Node> propertyNodeOptional = getOrCreatePropertyOfElement(element, property, "#000000");
		
		if (!propertyNodeOptional.isPresent()) {
			return;
		}
		
		Node propertyNode = propertyNodeOptional.get();
		
		NamedNodeMap nnm = propertyNode.getAttributes();
		
		if (nnm == null) {
			return;
		}
		
		if (nnm.getNamedItem("value") == null) {
			Node attNode = propertyNode.getOwnerDocument().createAttribute("value");
			attNode.setNodeValue(value);
			nnm.setNamedItem(attNode);
		}
		
		Node valueAttr = nnm.getNamedItem("value");
		
		valueAttr.setTextContent(value);
	}
	
	private void setPropertyColor(String element, String property, Color color) {
		setElementPropertyValue(element, property, Utils.colorToHex(color));
	}
	
	public Optional<Color> getBackgroundColor(String element) {
		return getPropertyAsColor(element, mxConstants.STYLE_FILLCOLOR);
	}
	
	public Optional<Color> getStrokeColor(String element) {
		return getPropertyAsColor(element, mxConstants.STYLE_STROKECOLOR);
	}
	
	public Optional<Color> getFontColor(String element) {
		return getPropertyAsColor(element, mxConstants.STYLE_FONTCOLOR);
	}
	
	public Font getFont(String element) {
		String fontFamily = getValueOfPropertyOfElement(element, mxConstants.STYLE_FONTFAMILY, true).orElse("Dialog");
		String fontSizeStr = getValueOfPropertyOfElement(element, mxConstants.STYLE_FONTSIZE, true).orElse("11");
		int fontSize = 11;

		try {
			fontSize = Integer.parseInt(fontSizeStr);
		} catch (NumberFormatException ex) {
			// We just keep it at 11
			System.out.println("Could not parse fontsize: \"" + fontSizeStr + "\"");
		}
		
		String fontStyleStr = getValueOfPropertyOfElement(element, mxConstants.STYLE_FONTSTYLE, true).orElse("0");
		int fontStyleInt = 0;

		try {
			fontStyleInt = Integer.parseInt(fontStyleStr);
		} catch (NumberFormatException ex) {
			// We just keep it at 0
			System.out.println("Could not parse fontstyle: \"" + fontStyleStr + "\"");
		}

		boolean isBold = (fontStyleInt & mxConstants.FONT_BOLD) == mxConstants.FONT_BOLD;
		boolean isItalic = (fontStyleInt & mxConstants.FONT_ITALIC) == mxConstants.FONT_ITALIC;
		
		int styleFlags = 0;
		if (isBold) {
			styleFlags = styleFlags | Font.BOLD;
		}
		if (isItalic) {
			styleFlags = styleFlags | Font.ITALIC;
		}
		
		return new Font(fontFamily, styleFlags, fontSize);
	}
	
	/**
	 * Sets the background color on an element. If it does not exist, it creates it.
	 * @param element
	 * @param color
	 */
	public void setBackgroundColor(String element, Color color) {
		setPropertyColor(element, mxConstants.STYLE_FILLCOLOR, color);
	}
	
	/**
	 * Sets the stroke color on an element. If it does not exist, it creates it.
	 * @param element
	 * @param color
	 */
	public void setStrokeColor(String element, Color color) {
		setPropertyColor(element, mxConstants.STYLE_STROKECOLOR, color);
	}
	
	/**
	 * Sets the font color on an element. If it does not exist, it creates it.
	 * @param element
	 * @param color
	 */
	public void setFontColor(String element, Color color) {
		setPropertyColor(element, mxConstants.STYLE_FONTCOLOR, color);
	}
	
	/**
	 * Sets the font size on an element. If it does not exist, it creates it.
	 * @param element
	 * @param size
	 */
	public void setFontSize(String element, int size) {
		setElementPropertyValue(element, mxConstants.STYLE_FONTSIZE, size + "");
	}
	
	/**
	 * Sets the font family for an element. If it does not exist, it creates it.
	 * @param element
	 * @param fontFamily Any font, as long as java.awt.Font understands it.
	 */
	public void setFontFamily(String element, String fontFamily) {
		setElementPropertyValue(element, mxConstants.STYLE_FONTFAMILY, fontFamily);
	}
	
	/**
	 * Sets the font style for an element. If it does not exist, it creates it.
	 * @param element
	 * @param isBold
	 * @param isItalic
	 */
	public void setFontStyle(String element, boolean isBold, boolean isItalic) {
		int styleFlags = 0;
		if (isBold) {
			styleFlags = styleFlags | mxConstants.FONT_BOLD;
		}
		if (isItalic) {
			styleFlags = styleFlags | mxConstants.FONT_ITALIC;
		}
		
		setElementPropertyValue(element, mxConstants.STYLE_FONTSTYLE, styleFlags + "");
	}
	
	private class StringPair {
		public final String left;
		public final String right;
		
		StringPair(String left, String right) {
			this.left = left;
			this.right = right;
		}
	}
	
	/**
	 * Applies every single style detail from every style element from style delta
	 * to the current style if they have changed.
	 * @param styleDelta
	 * @return True if any elements were changed
	 */
	public boolean applyStyleDelta(Map<Element, E3StyleEvent> styleDelta) {
		boolean anythingChanged = false;
		
		for (Entry<Element, E3StyleEvent> entry : styleDelta.entrySet()) {
			String element = entry.getKey().getStyleName();
			E3StyleEvent e3se = entry.getValue();
			List<StringPair> pairs = new ArrayList<>();
			
			pairs.add(new StringPair(mxConstants.STYLE_FILLCOLOR, Utils.colorToHex(e3se.bgColor)));
			pairs.add(new StringPair(mxConstants.STYLE_GRADIENTCOLOR, Utils.colorToHex(e3se.bgColor)));
			pairs.add(new StringPair(mxConstants.STYLE_STROKECOLOR, Utils.colorToHex(e3se.strokeColor)));
			pairs.add(new StringPair(mxConstants.STYLE_FONTCOLOR, Utils.colorToHex(e3se.fontColor)));
			pairs.add(new StringPair(mxConstants.STYLE_FONTSIZE, e3se.font.getSize() + ""));
			pairs.add(new StringPair(mxConstants.STYLE_FONTFAMILY, e3se.font.getFamily()));
			
			int styleFlags = 0;
			if (e3se.font.isBold()) {
				styleFlags |= mxConstants.FONT_BOLD;
			}
			if (e3se.font.isItalic()) {
				styleFlags |= mxConstants.FONT_ITALIC;
			}
			
			pairs.add(new StringPair(mxConstants.STYLE_FONTSTYLE, styleFlags + ""));
			
			for (StringPair pair : pairs) {
				Optional<String> currentValueOptional = getValueOfPropertyOfElement(element, pair.left, true);
				
				if (currentValueOptional.isPresent()) {
					// If the value is present, check if it is different.
					String currentValue = currentValueOptional.get();
					if (!currentValue.equals(pair.right)) {
						// If it differs, set it
						setElementPropertyValue(element, pair.left, pair.right);
						anythingChanged = true;
					}
				} else {
					// If it is not present, set it immediately
					setElementPropertyValue(element, pair.left, pair.right);
					anythingChanged = true;
				}
			}
		}
		
		return anythingChanged;
	}
}
