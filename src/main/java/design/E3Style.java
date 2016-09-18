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

public class E3Style {
	public static final double DOTRADIUS = 4;
	public static final int LABEL_FONTSIZE = 12;
	public static int idCounter = 0;

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
	
	public static Optional<E3Style> loadInternal(String name) {
		List<String> files = Arrays.asList(
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
				)
			.stream()
			.map(f -> Utils.readInternal("/styles/" + name + "/" + f))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList());
		
		if (files.size() != 12) {
			return Optional.empty();
		}
		
		// TODO: This looks ugly!
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
	
	public static E3Style load(File file) {
		throw new RuntimeException("E3Style loading not yet implemented!");
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
		
		// Parse the xml
		doc = mxXmlUtils.parseXml(xml);
		
		// Get the name of the style
		// TODO: Error handling here as well
		System.out.println(xml);
		name = doc
				.getDocumentElement()
				.getElementsByTagName("name")
				.item(0)
				.getTextContent();
		name = name + "_" + ID;
		
		// Apply the naming subsitution in the XML
		xml = xml.replace("{!name}", name);
		doc = mxXmlUtils.parseXml(xml);

		System.out.println(xml);
		
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
		
		// TODO: Fall back to default style somehow here and show an error box
		// (if there is a space in the name - spaces are a recipe for disaster
		if (name.contains(" ")) {
			System.out.println("Error: name of style contains spaces!");
		}
		
		// Prepend name_ to all the node's as and extend attributes, s.t.
		// the styles remain separate by namespace from eachother
//		NodeList addNodes = doc.getDocumentElement().getElementsByTagName("add");
//
//		for (int i = 0; i < addNodes.getLength(); i++) {
//			Node node = addNodes.item(i);
//			NamedNodeMap nnm = node.getAttributes();
//			
//			Node asNode = nnm.getNamedItem("as");
//			if (asNode != null) {
//				asNode.setTextContent(name + "_" + asNode.getTextContent());
//			}
//			
//			Node extendNode = nnm.getNamedItem("extend");
//			if (extendNode != null) {
//				extendNode.setTextContent(name + "_" + asNode.getTextContent());
//			}
//		}
		
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
		addMarketSegmentColor("", "#C0C0C0"); // Makes sure the default one has standard color
		// TODO: Take standard color from xml, not constant #C0C0C0
	}

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
	
	public void styleGraphComponent(mxGraphComponent graphComponent) {
		System.out.println("Styling graph with " + name);
		
		mxGraph graph = graphComponent.getGraph();
		
		// Register styles
		mxCodec codec = new mxCodec();
		
		graph.setStylesheet(new mxStylesheet());

		if (doc != null) {
			codec.decode(doc.getDocumentElement(), graph.getStylesheet());
		} else {
			System.out.println("Failed loading style");
			return;
		}
		
		graphComponent.getViewport().setOpaque(true);
		graphComponent.getViewport().setBackground(backgroundColor);
		graphComponent.setGridVisible(showGrid);
		
		// To get rid of the folding icon
		graphComponent.setFoldingEnabled(false);
		
		// Make sure all the market segments look neutral
		Utils.update(graph, () -> {
			Utils.getAllCells(graph)
				.stream()
				.forEach(o -> {
					if (!(o instanceof Base)) return;

					Base val = (Base) graph.getModel().getValue(o);
					if (val instanceof MarketSegment) {
						graph.setCellStyle(name + "_MarketSegment");
					} 
				});
		}); 
		
		graph.refresh();
			
		// Add custom stencils
//		addStencil("valueport.shape");
//		addStencil("startsignal.shape");
//		addStencil("endsignal.shape");
//		addStencil("dot.shape");
//		addStencil("bar.shape");
//		addStencil("easttriangle.shape");
//		addStencil("southtriangle.shape");
//		addStencil("westtriangle.shape");
//		addStencil("northtriangle.shape");
//		addStencil("note.shape");
		
		// Set appropriate per-cell style names
//		Utils.update(graph, () -> {
//			Utils.getAllCells(graph)
//				.stream()
//				.forEach(o -> {
//					Base val = (Base) graph.getModel().getValue(o);
//					if (val instanceof Actor) {
//						Actor acInfo = (Actor) val;
//						if (acInfo.colluded) {
//							graph.setCellStyle(name + "_ColludedActor");
//						} else {
//							graph.setCellStyle(name + "_Actor");
//						}
//					} else if (val instanceof ValueActivity) {
//						graph.setCellStyle(name + "_ValueActivity");
//					} else if (val instanceof MarketSegment) {
//						graph.setCellStyle(name + "_MarketSegment");
//					} else if (val instanceof ValueInterface) {
//						graph.setCellStyle(name + "_ValueInterface");
//					} else if (val instanceof )
//				});
//		}); 
	}
	
	// Keeps track of all the names of the stencils that are loaded
	public static Set<String> loadedStencils = new HashSet<>();
	
	/**
	 * Reads the file at filename and adds it as a stencil
	 * @param filename
	 */
	public static void addStencil(String filename) {
		try {
			// Read the file
			String xmlString = mxUtils.readInputStream(
                                E3Style.class.getResourceAsStream("/" + filename));
			
			// Add it
			addStringStencil(xmlString);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Unable to load stencil " + filename);
		}
	}
	
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

		System.out.println("Adding " + name);

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
	
	public static List<String> getAvailableThemes() {
		List includedWithEditor = Arrays.asList(
				"E3Style",
				"E3Mono"
				);
		
		// TODO: Read a bunch of dirs in my documents
		// TODO: Handle duplicates

		return includedWithEditor;
	}
}
