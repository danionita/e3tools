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
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.shape.mxStencilShape;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;

public class E3Style {
	public static final double DOTRADIUS = 4;
	public static final int LABEL_FONTSIZE = 12;
	
	public static void styleGraphComponent(mxGraphComponent graphComponent) {
		mxGraph graph = graphComponent.getGraph();
		
		// Register styles
		mxStylesheet stylesheet = graph.getStylesheet();
		Hashtable<String, Object> style;
		Hashtable<String, Object> baseStyle = new Hashtable<>();
		
		// Gonna need this at some point:
//				mxGraphComponent graphComponent = (mxGraphComponent) e
//						.getSource();
//				mxGraph graph = graphComponent.getGraph();
//				mxCodec codec = new mxCodec();
//				Document doc = mxUtils.loadDocument(EditorActions.class
//						.getResource(stylesheet).toString());
//
//				if (doc != null)
//				{
//					codec.decode(doc.getDocumentElement(),
//							graph.getStylesheet());
//					graph.refresh();
//				}

		baseStyle.put(mxConstants.STYLE_STROKECOLOR,  "#000000");
		baseStyle.put(mxConstants.STYLE_STROKEWIDTH, 1.0);
		baseStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
		baseStyle.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_LEFT);
		baseStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_TOP);
		baseStyle.put(mxConstants.STYLE_FONTSIZE, LABEL_FONTSIZE);
		
		style = new Hashtable<>(baseStyle);
		style.put(mxConstants.STYLE_ROUNDED, true);
		style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
		style.put(mxConstants.STYLE_PERIMETER, "rectanglePerimeter");
		style.put(mxConstants.STYLE_FILLCOLOR, "#C0C0C0");
		style.put(mxConstants.STYLE_GRADIENTCOLOR, "#C0C0C0");
		stylesheet.putCellStyle("ValueActivity", style);
		
		style = new Hashtable<>(baseStyle);
		style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
		style.put(mxConstants.STYLE_FILLCOLOR, "#C0C0C0");
		style.put(mxConstants.STYLE_GRADIENTCOLOR, "#C0C0C0");
		stylesheet.putCellStyle("Actor", style);
		
		style = new Hashtable<>(baseStyle);
		style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
		style.put(mxConstants.STYLE_FILLCOLOR, "#C0C0C0");
		style.put(mxConstants.STYLE_GRADIENTCOLOR, "#C0C0C0");
		style.put(mxConstants.STYLE_STROKECOLOR, "#CC0000");
		style.put(mxConstants.STYLE_STROKEWIDTH, 3);
		stylesheet.putCellStyle("ColludedActor", style);
		
		style = new Hashtable<>(baseStyle);
		style.put(mxConstants.STYLE_SHAPE, "MarketSegmentStencil"); // We added this above in the try block
		style.put(mxConstants.STYLE_FILLCOLOR, "#C0C0C0");
		style.put(mxConstants.STYLE_GRADIENTCOLOR, "#C0C0C0");
		stylesheet.putCellStyle("MarketSegment", style);

		// ArcSize is not taken into account by JGraphX
		// (see class mxRectangleShape. It's right there at the arcSize() function call)
		// TODO: Fix this and create a pull request
		style = new Hashtable<>(baseStyle);
		style.put(mxConstants.STYLE_ROUNDED, true);
		style.put(mxConstants.STYLE_ARCSIZE, 50);
		style.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF");
		style.put(mxConstants.STYLE_GRADIENTCOLOR, "#FFFFFF");
		style.put(mxConstants.STYLE_RESIZABLE, 0);
		stylesheet.putCellStyle("ValueInterface", style);
		
		style = new Hashtable<>(baseStyle);
		style.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF");
		style.put(mxConstants.STYLE_GRADIENTCOLOR, "#FFFFFF");
		style.put(mxConstants.STYLE_SHAPE, "ValuePortStencil");
		style.put(mxConstants.STYLE_RESIZABLE, 0);
		stylesheet.putCellStyle("ValuePortWest", style);

		style = new Hashtable<>(baseStyle);
		style.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF");
		style.put(mxConstants.STYLE_GRADIENTCOLOR, "#FFFFFF");
		style.put(mxConstants.STYLE_SHAPE, "ValuePortStencil");
		style.put(mxConstants.STYLE_RESIZABLE, 0);
		style.put(mxConstants.STYLE_ROTATION, 180);
		stylesheet.putCellStyle("ValuePortEast", style);

		style = new Hashtable<>(baseStyle);
		style.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF");
		style.put(mxConstants.STYLE_GRADIENTCOLOR, "#FFFFFF");
		style.put(mxConstants.STYLE_SHAPE, "ValuePortStencil");
		style.put(mxConstants.STYLE_RESIZABLE, 0);
		style.put(mxConstants.STYLE_ROTATION, 90);
		stylesheet.putCellStyle("ValuePortNorth", style);

		style = new Hashtable<>(baseStyle);
		style.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF");
		style.put(mxConstants.STYLE_GRADIENTCOLOR, "#FFFFFF");
		style.put(mxConstants.STYLE_SHAPE, "ValuePortStencil");
		style.put(mxConstants.STYLE_RESIZABLE, 0);
		style.put(mxConstants.STYLE_ROTATION, 270);
		stylesheet.putCellStyle("ValuePortSouth", style);
		
		style = new Hashtable<>();
		style.put(mxConstants.STYLE_STROKEWIDTH, 3);
		style.put(mxConstants.STYLE_ENDARROW, mxConstants.NONE);
		style.put(mxConstants.STYLE_STROKECOLOR, "#0000FF");
		style.put(mxConstants.STYLE_FONTCOLOR, "#444444");
		style.put(mxConstants.STYLE_FONTSIZE, LABEL_FONTSIZE);
		stylesheet.putCellStyle("ValueExchange", style);
		
		style = new Hashtable<>(style);
		style.put(mxConstants.STYLE_DASHED, true);
		stylesheet.putCellStyle("NonOccurringValueExchange", style);
		
		style = new Hashtable<>(style);
		style.put(mxConstants.STYLE_DASH_PATTERN, "1 1"); // Wtf mxGraph won't allow me to just pass in a plain old float[] wtf wtf
		stylesheet.putCellStyle("HiddenValueExchange", style);
		
		style = new Hashtable<>();
		style.put(mxConstants.STYLE_STROKEWIDTH, 3);
		style.put(mxConstants.STYLE_ENDARROW, mxConstants.NONE);
		style.put(mxConstants.STYLE_STROKECOLOR, "#000000");
		style.put(mxConstants.STYLE_FILLCOLOR, "#FF0000");
		style.put(mxConstants.STYLE_DASHED, true);
		style.put(mxConstants.STYLE_FONTSIZE, LABEL_FONTSIZE);
		stylesheet.putCellStyle("ConnectionElement", style);
		
		style = new Hashtable<>();
		style.put(mxConstants.STYLE_SHAPE, "StartSignalStencil");
		style.put(mxConstants.STYLE_STROKECOLOR, "#000000");
		style.put(mxConstants.STYLE_RESIZABLE, 0);
		// There is also STYLE_VERTICAL_POSTION_ALIGN or something
		// Which is supposed to put a label on top of the cell's border
		// But I think it's broken therefore it's hacked like this
		// For the hack see ToolComponent cell creation of StartSignal
		style.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_TOP);
		style.put(mxConstants.STYLE_FONTCOLOR, "#000000");
		style.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "#FAFAD2");
		stylesheet.putCellStyle("StartSignal", style);
		
		style = new Hashtable<>();
		style.put(mxConstants.STYLE_SHAPE, "EndSignalStencil");
		style.put(mxConstants.STYLE_STROKECOLOR, "#000000");
		style.put(mxConstants.STYLE_RESIZABLE, 0);
		style.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_TOP);
		style.put(mxConstants.STYLE_FONTCOLOR, "#000000");
		style.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "#FAFAD2");
		stylesheet.putCellStyle("EndSignal", style);
		
		style = new Hashtable<>();
		style.put(mxConstants.STYLE_SHAPE, "DotStencil");
		style.put(mxConstants.STYLE_STROKECOLOR, "#000000");
		stylesheet.putCellStyle("Dot", style);
		
		style = new Hashtable<>();
		style.put(mxConstants.STYLE_SHAPE, "BarStencil");
		style.put(mxConstants.STYLE_STROKECOLOR, "#000000");
		stylesheet.putCellStyle("Bar", style);
		
		style = new Hashtable<>();
		style.put(mxConstants.STYLE_OPACITY, 0);
		stylesheet.putCellStyle("LogicBase", style);
		
		style = new Hashtable<>();
		style.put(mxConstants.STYLE_SHAPE, "SouthTriangleStencil");
		style.put(mxConstants.STYLE_STROKECOLOR, "#000000");
		style.put(mxConstants.STYLE_STROKEWIDTH, 2);
		style.put(mxConstants.STYLE_FILLCOLOR, "#000000");
		style.put(mxConstants.STYLE_PERIMETER, mxConstants.PERIMETER_TRIANGLE);
		stylesheet.putCellStyle("SouthTriangle", style);
		
		style = new Hashtable<>();
		style.put(mxConstants.STYLE_SHAPE, "WestTriangleStencil");
		style.put(mxConstants.STYLE_STROKECOLOR, "#000000");
		style.put(mxConstants.STYLE_STROKEWIDTH, 2);
		style.put(mxConstants.STYLE_FILLCOLOR, "#000000");
		style.put(mxConstants.STYLE_PERIMETER, mxConstants.PERIMETER_TRIANGLE);
		stylesheet.putCellStyle("WestTriangle", style);
		
		style = new Hashtable<>();
		style.put(mxConstants.STYLE_SHAPE, "NorthTriangleStencil");
		style.put(mxConstants.STYLE_STROKECOLOR, "#000000");
		style.put(mxConstants.STYLE_STROKEWIDTH, 2);
		style.put(mxConstants.STYLE_FILLCOLOR, "#000000");
		style.put(mxConstants.STYLE_PERIMETER, mxConstants.PERIMETER_TRIANGLE);
		stylesheet.putCellStyle("NorthTriangle", style);
		
		style = new Hashtable<>();
		style.put(mxConstants.STYLE_SHAPE, "EastTriangleStencil");
		style.put(mxConstants.STYLE_STROKECOLOR, "#000000");
		style.put(mxConstants.STYLE_STROKEWIDTH, 2);
		style.put(mxConstants.STYLE_FILLCOLOR, "#000000");
		style.put(mxConstants.STYLE_PERIMETER, mxConstants.PERIMETER_TRIANGLE);
		stylesheet.putCellStyle("EastTriangle", style);
		
		style = new Hashtable<>();
		style.put(mxConstants.STYLE_OPACITY, 0);
		style.put(mxConstants.STYLE_FONTCOLOR, "#000000");
		style.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_LEFT);
		// style.put(mxConstants.STYLE_RESIZABLE, 0);
		stylesheet.putCellStyle("NameText", style);
		
		style = new Hashtable<>();
		style.put(mxConstants.STYLE_SHAPE, "NoteStencil");
		style.put(mxConstants.STYLE_STROKECOLOR, "#808080");
		style.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_LEFT);
		style.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_TOP);
		stylesheet.putCellStyle("Note", style);
		
		// Aw yeah '90
		graphComponent.getViewport().setOpaque(true);
		graphComponent.getViewport().setBackground(Color.WHITE);
		graphComponent.setGridVisible(true);
		
		// To get rid of the folding icon
		graphComponent.setFoldingEnabled(false);
			
		// Add custom stencils
		addStencil("valueport.shape");
		addStencil("startsignal.shape");
		addStencil("endsignal.shape");
		addStencil("dot.shape");
		addStencil("bar.shape");
		addStencil("easttriangle.shape");
		addStencil("southtriangle.shape");
		addStencil("westtriangle.shape");
		addStencil("northtriangle.shape");
		addStencil("note.shape");
		addMarketSegmentColor("", "#C0C0C0");
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
	 * Adds the stencil in xmlString
	 * @param xmlString
	 */
	public static void addStringStencil(String xmlString) {
		// Find first occurrence of < to avoid unicode BOM
		int lessThanIndex = xmlString.indexOf("<");
		if (lessThanIndex != -1) {
			xmlString = xmlString.substring(lessThanIndex);
		}
		mxStencilShape newShape = new mxStencilShape(xmlString);

		// Don't add it if we already know that name
		if (loadedStencils.contains(newShape.getName())) return;

		// Add it to the known names list
		loadedStencils.add(newShape.getName());
		
		// Add it to mxGraph
		mxGraphics2DCanvas.putShape(newShape.getName(), newShape);
	}
	
	/**
	 * Adds a specific market segment color. The name of the stencil
	 * will be "MarketSegment#FFFFFF", where "#FFFFFF" is equal
	 * to hexColor.
	 * @param hexColor
	 */
	public static void addMarketSegmentColor(String hexColor) {
		addMarketSegmentColor(hexColor.toUpperCase(), hexColor.toUpperCase());
	}
	
	/**
	 * Adds a market segment color, but does not add the hexcolor as postfix.
	 * Instead you can choose your own postfix. This can be used to implement aliases
	 * for colors.
	 * @param postfix
	 * @param hexColor
	 */
	private static void addMarketSegmentColor(String postfix, String hexColor) {
		// Cast every hex color to upper case, that way the names are consistent
		hexColor = hexColor.toUpperCase();
		
		String templateMarketSegment = "";
		
		// Get the template market segment stencil
		try {
			templateMarketSegment = mxUtils.readInputStream(
					E3Style.class.getResourceAsStream("/marketsegment_template.shape"));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		// Put the postfix and hexcolor in there
		String marketSegmentXML = String.format(templateMarketSegment, postfix, hexColor, hexColor, hexColor);
		
		// Add it
		addStringStencil(marketSegmentXML);
	}
	
	/**
	 * This returns a shape name to be used with mxConstants.STYLE_SHAPE. Example:
	 * 
	 * model.beginUpdate();
	 * try {
	 * 		String backgroundColor = "#FF0000";
	 * 		E3Style.addMarketSegmentColor(backgroundColor);
	 * 		graph.setCellStyles(mxConstants.STYLE_SHAPE, E3Style.getMarketSegmentShapeName(backgroundColor), new Object[]{aCell});
	 * } finally {
	 * 		model.endUpdate();
	 * }
	 * 
	 * Should "Just Work (TM)".
	 * 
	 * @param hexColor
	 * @return
	 */
	public static String getMarketSegmentShapeName(String hexColor) {
		return "MarketSegmentStencil" + hexColor.toUpperCase();
	}
}
