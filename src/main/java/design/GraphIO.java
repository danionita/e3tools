package design;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.help.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.swing.JOptionPane;

import com.mxgraph.io.mxCodecRegistry;
import com.mxgraph.io.mxObjectCodec;
import com.mxgraph.util.mxConstants;

import design.export.ObjectXStreamCodec;
import design.info.Actor;
import design.info.Base;
import design.info.ConnectionElement;
import design.info.EndSignal;
import design.info.LogicBase;
import design.info.LogicDot;
import design.info.MarketSegment;
import design.info.SignalDot;
import design.info.StartSignal;
import design.info.ValueActivity;
import design.info.ValueExchange;
import design.info.ValueInterface;
import design.info.ValuePort;

/**
 * Houses all the functionality and global state involved with importing models
 * @author Bobe
 *
 */
public class GraphIO {

	// This keeps track of whether or not the codecs are registered
	static boolean registered = false;
	public static void assureRegistered() {
		if (!registered) {
			registerCodecs();
			registered = true;
		}
	}

	/** 
	 * A custom codec that can also encode enums. 
	 * Source: http://forum.jgraph.com/questions/2302/how-to-encode-enums?page=1#3433
	 * (Except for the replace part; that is my addition.)
	 */
	public static class ObjectCodecWithEnum extends mxObjectCodec {
		public ObjectCodecWithEnum(Object template) {
			super(template);
		}

		protected boolean isPrimitiveValue(Object value) {
			return super.isPrimitiveValue(value) || value.getClass().isEnum();
		}

		protected void setFieldValue(Object obj, String fieldname, Object value) {
			Field field = getField(obj, fieldname);

			if (field.getType().isEnum()) {
				Object[] c = field.getType().getEnumConstants();

				for (int i = 0; i < c.length; i++) {
					if (c[i].toString().equals(value)) {
						value = c[i];
						break;
					}
				}
			}

			super.setFieldValue(obj, fieldname, value);
		}
		
		@Override
		public String getName() {
			String name = super.getName();
			return name.replace("$", "___");
		}
	}

	/**
	 * Registers all the codecs needed for loading and saving.
	 * 
	 */
	private static void registerCodecs() {
		// from UserObject.java
		// Defines the user objects, which are preferrably XML nodes that allow
		// storage of complex values as child nodes and string, number or
		// boolean properties as attributes.
		//
		// When using Java objects as user objects, make sure to add the
		// package name containg the class and register a codec for the user
		// object class as follows:
		//
		// mxCodecRegistry.addPackage("com.example");
		// mxCodecRegistry.register(new mxObjectCodec(
		// new com.example.CustomUserObject()));
		//
		// Note that the object must have an empty constructor and a setter and
		// getter for each property to be persisted. The object must not have
		// a property called ID as this property is reserved for resolving cell
		// references and will cause problems when used inside the user object.

		// enums aren't supported out of the box. with the code below you can
		// support it; posted by gaudenz:
		// http://forum.jgraph.com/questions/2302/how-to-encode-enums?page=1#3433
		
		mxCodecRegistry.addPackage("design.info");
		
		ObjectXStreamCodec xStreamCodec = new ObjectXStreamCodec();
		mxCodecRegistry.register(xStreamCodec);	
		
		mxCodecRegistry.addAlias(ValuePort.class.getSimpleName(), xStreamCodec.getName());
		mxCodecRegistry.addAlias(ValueInterface.class.getSimpleName(), xStreamCodec.getName());
		mxCodecRegistry.addAlias(Actor.class.getSimpleName(), xStreamCodec.getName());
		mxCodecRegistry.addAlias(MarketSegment.class.getSimpleName(), xStreamCodec.getName());
		mxCodecRegistry.addAlias(ValueActivity.class.getSimpleName(), xStreamCodec.getName());
		mxCodecRegistry.addAlias(SignalDot.class.getSimpleName(), xStreamCodec.getName());
		mxCodecRegistry.addAlias(LogicDot.class.getSimpleName(), xStreamCodec.getName());
		mxCodecRegistry.addAlias(LogicBase.class.getSimpleName(), xStreamCodec.getName());
		mxCodecRegistry.addAlias(ValueExchange.class.getSimpleName(), xStreamCodec.getName());
		mxCodecRegistry.addAlias(StartSignal.class.getSimpleName(), xStreamCodec.getName());
		mxCodecRegistry.addAlias(EndSignal.class.getSimpleName(), xStreamCodec.getName());
		mxCodecRegistry.addAlias(ConnectionElement.class.getSimpleName(), xStreamCodec.getName());
	}

	/**
	 * Loads the graph at location filename. If it fails returns an empty optional.
	 * @param fileName
	 * @return
	 */
	public static Optional<E3Graph> loadGraph(String fileName) {
		List<String[]> files = null;
		
		// Get all files from the zip file
		try (ZipFile zf = new ZipFile(fileName)) {
			// For each file
			files = zf.stream().map(entry -> {
				try {
					// Construct the file from the stream
					BufferedReader reader = new BufferedReader(new InputStreamReader(zf.getInputStream(entry)));

					StringBuilder out = new StringBuilder();
					String line;

					while ((line = reader.readLine()) != null) {
						out.append(line + "\n");
					}
					reader.close();
					
					return new String[]{entry.getName(), out.toString()};
				} catch (IOException e) {
					// If it fails, print a stacktrace and return null
					e.printStackTrace();
				}
				
				return null;
			}).collect(Collectors.toList());
			
			// If one file failed, just abort.
			if (files.contains(null)) {
				return Optional.empty();
			}
		} catch (IOException e1) {
			// If something happens, abort
			e1.printStackTrace();
			return Optional.empty();
		}
			
		// Find the xml file and property file in the zip files
		String xml = null;
		String properties = null;
		for (String[] file : files) {
			if (file[0].equals("graph.xml")) {
				xml = file[1];
			} else if (file[0].equals("properties.json")) {
				properties = file[1];
			}
		}
		
		// If either wasn't found, abort.
		if (xml == null || properties == null) return Optional.empty();
		
		// Create a graph from XML
		E3Graph graph = E3Graph.fromXML(xml);

		// Create a JSON object
		JsonObject json = Json.createReader(new StringReader(properties)).readObject();
		
		// Find basic information
		if (json.containsKey("title")) {
			graph.title = json.getString("title");
		}
		
		if (json.containsKey("fraud")) {
			graph.isFraud = json.getBoolean("fraud");
		}
		
		if (json.containsKey("valueObjects")) {
			// Clear all the value objects
			graph.valueObjects.clear();
			JsonArray valueObjects = json.getJsonArray("valueObjects");
			// For each value object, add it
			for (int i = 0;  i < valueObjects.size(); i++) {
				graph.valueObjects.add(valueObjects.getString(i));
			}
		}
		
		// For each market segment, if it has a MarketSegmentStencil#FFFFFF
		// shape, add that color shape to the registry
		Utils.getAllCells(graph).stream()
		.filter(obj -> graph.getModel().getValue(obj) instanceof MarketSegment)
		.forEach(obj -> {
			java.util.Map<String, Object> style = graph.getCellStyle(obj);

			if (!style.containsKey(mxConstants.STYLE_SHAPE)) return;
			String shape = (String) style.get(mxConstants.STYLE_SHAPE);

			if (!shape.contains("#")) return;
			String[] parts = shape.split("#");

			if (parts.length != 2) return;
			String hexCode = "#" + parts[1];
			// TODO: Enable style loading
//			E3Style.addMarketSegmentColor(hexCode);
		});
		
		return Optional.of(graph);
	}

	/**
	 * Saves the graph to path filename.
	 * @param graph
	 * @param fileName
	 * @throws IOException if something fails
	 */
	public static void saveGraph(E3Graph graph, String fileName) throws IOException {
		String xml = graph.toXML();
		
		// Create the JSON object with value objects and other properties
		JsonArrayBuilder valueObjectsJson = Json.createArrayBuilder();
		for (String vo : graph.valueObjects) {
			valueObjectsJson.add(vo);
		}
				
		String properties = Json.createObjectBuilder()
				.add("fraud", graph.isFraud)
				.add("title", graph.title == null ? "" : graph.title)
				.add("valueObjects", valueObjectsJson)
				.build().toString();
		
		// Create a zip
		try (ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(fileName))) {
			// Print the graph xml to a zip file entry
			zout.putNextEntry(new ZipEntry("graph.xml"));
			byte[] b = xml.getBytes();
			zout.write(b, 0, b.length);
			zout.closeEntry();

			// Print the json to a zip file entry
			zout.putNextEntry(new ZipEntry("properties.json"));
			b = properties.getBytes();
			zout.write(b, 0, b.length);
			zout.closeEntry();
		}
		
		JOptionPane.showMessageDialog(Main.mainFrame, "File saved to: " + fileName);
	}
}
