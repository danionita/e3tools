package design.main;

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

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.swing.JOptionPane;

import com.mxgraph.io.mxCodecRegistry;
import com.mxgraph.io.mxObjectCodec;

import design.main.export.ObjectXStreamCodec;
import design.main.info.Actor;
import design.main.info.Base;
import design.main.info.ConnectionElement;
import design.main.info.EndSignal;
import design.main.info.LogicBase;
import design.main.info.LogicDot;
import design.main.info.MarketSegment;
import design.main.info.SignalDot;
import design.main.info.StartSignal;
import design.main.info.ValueActivity;
import design.main.info.ValueExchange;
import design.main.info.ValueInterface;
import design.main.info.ValuePort;

public class GraphIO {

	static boolean registered = false;
	public static void assureRegistered() {
		if (!registered) {
			registerCodecs();
			registered = true;
		}
	}

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
		
		mxCodecRegistry.addPackage("design.main.info");
		
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

	public static Optional<E3Graph> loadGraph(String fileName) {
		// TODO: Try-with-resource here
		ZipFile zf;
		try {
			zf = new ZipFile(fileName);
		} catch (IOException e1) {
			e1.printStackTrace();
			return Optional.empty();
		}

		String properties = null, xml = null;
		List<String[]> files = zf.stream().map(entry -> {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(zf.getInputStream(entry)));

				StringBuilder out = new StringBuilder();
				String line;

				while ((line = reader.readLine()) != null) {
					out.append(line + "\n");
				}
				reader.close();
				
				return new String[]{entry.getName(), out.toString()};
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return null;
		}).collect(Collectors.toList());
		
		if (files.contains(null)) {
			return Optional.empty();
		}
		
		for (String[] file : files) {
			if (file[0].equals("graph.xml")) {
				xml = file[1];
			} else if (file[0].equals("properties.json")) {
				properties = file[1];
			}
		}
		
		E3Graph graph = E3Graph.fromXML(xml);

		JsonObject json = Json.createReader(new StringReader(properties)).readObject();
		
		if (json.containsKey("title")) {
			graph.title = json.getString("title");
		}
		
		if (json.containsKey("fraud")) {
			graph.isFraud = json.getBoolean("fraud");
		}
		
		if (json.containsKey("valueObjects")) {
			graph.valueObjects.clear();
			JsonArray valueObjects = json.getJsonArray("valueObjects");
			for (int i = 0;  i < valueObjects.size(); i++) {
				graph.valueObjects.add(valueObjects.getString(i));
			}
		}
		
		try {
			zf.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to close the zip stream");
		}
		
		return Optional.of(graph);
	}

	public static void saveGraph(E3Graph graph, String fileName) throws IOException {
		String xml = graph.toXML();
		
		JsonArrayBuilder valueObjectsJson = Json.createArrayBuilder();
		for (String vo : graph.valueObjects) {
			valueObjectsJson.add(vo);
		}
				
		String properties = Json.createObjectBuilder()
				.add("fraud", graph.isFraud)
				.add("title", graph.title == null ? "" : graph.title)
				.add("valueObjects", valueObjectsJson)
				.build().toString();
		
		// TODO: Try-with-resource here!
		ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(fileName));
		
		{
			zout.putNextEntry(new ZipEntry("graph.xml"));
			byte[] b = xml.getBytes();
			zout.write(b, 0, b.length);
			zout.closeEntry();
		}
		
		{
			zout.putNextEntry(new ZipEntry("properties.json"));
			byte[] b = properties.getBytes();
			zout.write(b, 0, b.length);
			zout.closeEntry();
		}
		
		zout.close();
		
		JOptionPane.showMessageDialog(Main.mainFrame, "File saved to: " + fileName);
	}
}
