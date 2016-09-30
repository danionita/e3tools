package design;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

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
		Map<String, String> files = new HashMap<>();
		
		// Get all files from the zip file
		try (ZipFile zf = new ZipFile(fileName)) {
			// For each file
			files.putAll(zf.stream().collect(Collectors.toMap(ZipEntry::getName, entry -> {
				try {
					// Construct the file from the stream
					BufferedReader reader = new BufferedReader(new InputStreamReader(zf.getInputStream(entry)));

					StringBuilder out = new StringBuilder();
					String line;

					while ((line = reader.readLine()) != null) {
						out.append(line + "\n");
					}
					reader.close();
					
					return out.toString();
				} catch (IOException e) {
					// If it fails, print a stacktrace and return null
					e.printStackTrace();
				}
				
				return null;
			})));

			// If one file failed, just abort.
			if (files.values().contains(null)) {
				System.out.println("Null");
				return Optional.empty();
			}
		} catch (IOException e1) {
			// If something happens, abort
			e1.printStackTrace();
			return Optional.empty();
		}
		
		// If either wasn't found, abort.
		if ((!files.containsKey("graph.xml") || files.get("graph.xml") == null)
			|| (!files.containsKey("properties.json") || files.get("properties.json") == null)) {
			System.out.println("Error: Could not find graph.xml or properties.json");
			
			return Optional.empty();
		}

		// Load the default style as backup
		E3Style style = null;
		{
			Optional<E3Style> opt = E3Style.loadInternal("E3Style");
			if (opt.isPresent()) {
				style = opt.get();
			} else {
				throw new RuntimeException("Error loading internal e3style");
			}
		}
		
		
		
		if (files.containsKey("style/style.xml") || files.get("style/style.xml") != null) {
			List<String> styleFiles = E3Style.requiredFiles
				.stream()
				// Would use optional here, but that got me compile errors
				// somehow.
				.map(file -> {
					String contents = files.getOrDefault("style/" + file, null);
					if (contents == null) System.out.println("Null: " + file);
					return contents;
				})
				.filter(c -> c != null)
				.collect(Collectors.toList());
			
			// The order of static variable requiredFiles matches the constructor
			// Doesn't make it any less ugly though
			
			if (styleFiles.size() == 12) {
				style = new E3Style(
						styleFiles.get(0),
						styleFiles.get(1),
						styleFiles.get(2),
						styleFiles.get(3),
						styleFiles.get(4),
						styleFiles.get(5),
						styleFiles.get(6),
						styleFiles.get(7),
						styleFiles.get(8),
						styleFiles.get(9),
						styleFiles.get(10),
						styleFiles.get(11)
						);
			}
		}
		
		// Create a graph from XML
		E3Graph graph = E3Graph.fromXML(files.get("graph.xml"), style);

		// Create a JSON object
		JsonObject json = Json.createReader(new StringReader(files.get("properties.json"))).readObject();
		
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
				java.util.Map<String, Object> styleMap = graph.getCellStyle(obj);

				if (!styleMap.containsKey(mxConstants.STYLE_SHAPE)) return;
				String shape = (String) styleMap.get(mxConstants.STYLE_SHAPE);

				if (!shape.contains("#")) return;
				String[] parts = shape.split("#");

				if (parts.length != 2) return;
				String hexCode = "#" + parts[1];
				graph.style.addMarketSegmentColor(hexCode);
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
			writeFileToZip(zout, "graph.xml", xml);

			// Print the json to a zip file entry
			writeFileToZip(zout, "properties.json", properties);
			
			// Print the style to the zip
			E3Style style = graph.style;
			writeFileToZip(zout, "style/style.xml", style.xml);
			writeFileToZip(zout, "style/marketsegment_template.shape", style.marketSegment_template);
			writeFileToZip(zout, "style/startsignal.shape", style.startSignal);
			writeFileToZip(zout, "style/endsignal.shape", style.endSignal);
			writeFileToZip(zout, "style/valueport.shape", style.valuePort);
			writeFileToZip(zout, "style/note.shape", style.note);
			writeFileToZip(zout, "style/northtriangle.shape", style.northTriangle);
			writeFileToZip(zout, "style/easttriangle.shape", style.eastTriangle);
			writeFileToZip(zout, "style/southtriangle.shape", style.southTriangle);
			writeFileToZip(zout, "style/westtriangle.shape", style.westTriangle);
			writeFileToZip(zout, "style/bar.shape", style.bar);
			writeFileToZip(zout, "style/dot.shape", style.dot);
		}
		
		JOptionPane.showMessageDialog(Main.mainFrame, "File saved to: " + fileName);
	}
	
	private static void writeFileToZip(ZipOutputStream zout, String path, String contents) throws IOException {
		zout.putNextEntry(new ZipEntry(path));
		byte[] b = contents.getBytes();
		zout.write(b, 0, b.length);
		zout.closeEntry();
	}
}
