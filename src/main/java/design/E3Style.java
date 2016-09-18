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

import org.w3c.dom.Document;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.io.mxCodec;
import com.mxgraph.shape.mxStencilShape;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;

public class E3Style {
	public static final double DOTRADIUS = 4;
	public static final int LABEL_FONTSIZE = 12;
	
	public static void styleGraphComponent(mxGraphComponent graphComponent) {
		mxGraph graph = graphComponent.getGraph();
		
		// Register styles
		mxCodec codec = new mxCodec();
		String xmlStyleSheet = "/e3_style.xml";
		Document doc = mxUtils.loadDocument(EditorActions.class
				.getResource(xmlStyleSheet).toString());

		if (doc != null)
		{
			codec.decode(doc.getDocumentElement(), graph.getStylesheet());
		}
		
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
//		String marketSegmentXML = String.format(templateMarketSegment, postfix, hexColor, hexColor, hexColor);
		
		String marketSegmentXML = templateMarketSegment
				.replace("{!postfix}", postfix)
				.replace("{!bg_color}", hexColor);
		
		// Add it
		addStringStencil(marketSegmentXML);
	}
	
	/**
	 * This returns a shape name to be used with mxConstants.STYLE_SHAPE. Example:
	 * 
	 * <code>
	 * model.beginUpdate();
	 * try {
	 * 		String backgroundColor = "#FF0000";
	 * 		E3Style.addMarketSegmentColor(backgroundColor);
	 * 		graph.setCellStyles(mxConstants.STYLE_SHAPE, E3Style.getMarketSegmentShapeName(backgroundColor), new Object[]{aCell});
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
	public static String getMarketSegmentShapeName(String hexColor) {
		return "MarketSegmentStencil" + hexColor.toUpperCase();
	}
}
