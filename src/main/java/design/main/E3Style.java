package design.main;

import java.awt.Color;
import java.io.IOException;
import java.util.Hashtable;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.shape.mxStencilShape;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxEdgeStyle;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;

public class E3Style {
	public static void styleGraphComponent(mxGraphComponent graphComponent) {
		mxGraph graph = graphComponent.getGraph();
		
		// Register styles
		mxStylesheet stylesheet = graph.getStylesheet();
		Hashtable<String, Object> style;
		Hashtable<String, Object> baseStyle = new Hashtable<>();
		
		baseStyle.put(mxConstants.STYLE_STROKECOLOR,  "#000000");
		baseStyle.put(mxConstants.STYLE_STROKEWIDTH, 1.0);
		baseStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
		
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
		style.put(mxConstants.STYLE_SHAPE, "MarketSegmentStencil"); // We added this above in the try block
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
		style.put(mxConstants.STYLE_SHAPE, "ValuePortWestStencil");
		style.put(mxConstants.STYLE_RESIZABLE, 0);
		stylesheet.putCellStyle("ValuePortWest", style);

		style = new Hashtable<>(baseStyle);
		style.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF");
		style.put(mxConstants.STYLE_GRADIENTCOLOR, "#FFFFFF");
		style.put(mxConstants.STYLE_SHAPE, "ValuePortEastStencil");
		style.put(mxConstants.STYLE_RESIZABLE, 0);
		stylesheet.putCellStyle("ValuePortEast", style);
		style = new Hashtable<>(baseStyle);

		style.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF");
		style.put(mxConstants.STYLE_GRADIENTCOLOR, "#FFFFFF");
		style.put(mxConstants.STYLE_SHAPE, "ValuePortNorthStencil");
		style.put(mxConstants.STYLE_RESIZABLE, 0);
		stylesheet.putCellStyle("ValuePortNorth", style);

		style = new Hashtable<>(baseStyle);
		style.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF");
		style.put(mxConstants.STYLE_GRADIENTCOLOR, "#FFFFFF");
		style.put(mxConstants.STYLE_SHAPE, "ValuePortSouthStencil");
		style.put(mxConstants.STYLE_RESIZABLE, 0);
		stylesheet.putCellStyle("ValuePortSouth", style);
		
		style = new Hashtable<>();
		style.put(mxConstants.STYLE_STROKEWIDTH, 3);
		style.put(mxConstants.STYLE_ENDARROW, mxConstants.NONE);
		style.put(mxConstants.STYLE_STROKECOLOR, "#0000FF");
		stylesheet.putCellStyle("ValueExchange", style);
		
		style = new Hashtable<>();
		style.put(mxConstants.STYLE_STROKEWIDTH, 3);
		style.put(mxConstants.STYLE_ENDARROW, mxConstants.NONE);
		style.put(mxConstants.STYLE_STROKECOLOR, "#000000");
		style.put(mxConstants.STYLE_FILLCOLOR, "#FF0000");
		style.put(mxConstants.STYLE_DASHED, true);
		stylesheet.putCellStyle("ConnectionElement", style);
		
		style = new Hashtable<>();
		style.put(mxConstants.STYLE_SHAPE, "StartSignalStencil");
		style.put(mxConstants.STYLE_STROKECOLOR, "#000000");
		stylesheet.putCellStyle("StartSignal", style);
		
		style = new Hashtable<>();
		style.put(mxConstants.STYLE_SHAPE, "EndSignalStencil");
		style.put(mxConstants.STYLE_STROKECOLOR, "#000000");
		stylesheet.putCellStyle("EndSignal", style);
		
		style = new Hashtable<>();
		style.put(mxConstants.STYLE_SHAPE, "DotStencil");
		style.put(mxConstants.STYLE_STROKECOLOR, "#000000");
		stylesheet.putCellStyle("Dot", style);
		
		// Aw yeah '90
		graphComponent.getViewport().setOpaque(true);
		style.put(mxConstants.STYLE_FILLCOLOR, "#00FF00");
		graphComponent.getViewport().setBackground(Color.WHITE);
		
		// To get rid of the folding icon
		graphComponent.setFoldingEnabled(false);
			
		// Add custom stencils
		addStencil("valueportwest.shape");
		addStencil("valueporteast.shape");
		addStencil("valueportnorth.shape");
		addStencil("valueportsouth.shape");
		addStencil("marketsegment.shape");
		addStencil("startsignal.shape");
		addStencil("endsignal.shape");
		addStencil("dot.shape");
	}
	
	public static void addStencil(String filename) {
		try {
			String nodeXml = mxUtils.readInputStream(E3Style.class.getResourceAsStream("/design/resources/" + filename));
			
			// Find first occurrence of < to avoid unicode BOM
			int lessThanIndex = nodeXml.indexOf("<");
			if (lessThanIndex != -1) {
				nodeXml = nodeXml.substring(lessThanIndex);
			}
			mxStencilShape newShape = new mxStencilShape(nodeXml);
			String name = newShape.getName();
			
			mxGraphics2DCanvas.putShape(name, newShape);
			System.out.println("Added " + name + " shape");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
