package design.main;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.swing.JOptionPane;

import org.w3c.dom.Document;

import com.mxgraph.io.mxCodec;
import com.mxgraph.io.mxCodecRegistry;
import com.mxgraph.io.mxObjectCodec;
import com.mxgraph.util.mxXmlUtils;

import design.main.Info.Actor;
import design.main.Info.ConnectionElement;
import design.main.Info.EndSignal;
import design.main.Info.LogicBase;
import design.main.Info.LogicDot;
import design.main.Info.MarketSegment;
import design.main.Info.SignalDot;
import design.main.Info.StartSignal;
import design.main.Info.ValueActivity;
import design.main.Info.ValueExchange;
import design.main.Info.ValueInterface;
import design.main.Info.ValuePort;

public class GraphIO {

	static boolean registered = false;

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
		
		mxCodecRegistry.addPackage(Info.class.getPackage().toString());
		
		mxCodecRegistry.register(new ObjectCodecWithEnum(new ValuePort()));
		mxCodecRegistry.register(new ObjectCodecWithEnum(new ValueInterface()));
		mxCodecRegistry.register(new ObjectCodecWithEnum(new Actor()));
		mxCodecRegistry.register(new ObjectCodecWithEnum(new MarketSegment()));
		mxCodecRegistry.register(new ObjectCodecWithEnum(new ValueActivity()));
		mxCodecRegistry.register(new ObjectCodecWithEnum(new SignalDot()));
		mxCodecRegistry.register(new ObjectCodecWithEnum(new LogicDot()));
		mxCodecRegistry.register(new ObjectCodecWithEnum(new LogicBase()));
		mxCodecRegistry.register(new ObjectCodecWithEnum(new ValueExchange()));
		mxCodecRegistry.register(new ObjectCodecWithEnum(new StartSignal()));
		mxCodecRegistry.register(new ObjectCodecWithEnum(new EndSignal()));
		mxCodecRegistry.register(new ObjectCodecWithEnum(new ConnectionElement()));
	}

	public static E3Graph loadGraph(String fileName) throws IOException {
		// necessary for this to work:
		if (!registered)
			registerCodecs();

		ZipFile zf = new ZipFile(fileName);

		String properties = null, xml = null;
		List<String[]> files = zf.stream().map(entry -> {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(zf.getInputStream(entry)));

				StringBuilder out = new StringBuilder();
				String line;

				while ((line = reader.readLine()) != null) {
					out.append(line);
				}
				reader.close();
				
				return new String[]{entry.getName(), out.toString()};
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return null;
		}).collect(Collectors.toList());
		
		for (String[] file : files) {
			if (file[0].equals("graph.xml")) {
				xml = file[1];
			} else if (file[0].equals("properties.json")) {
				properties = file[1];
			}
		}

		Document document = mxXmlUtils.parseXml(xml);
		mxCodec codec = new mxCodec(document);
		
		E3Graph graph = new E3Graph(false);
		
		codec.decode(document.getDocumentElement(), graph.getModel());

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
		
		return graph;
	}

	public static void saveGraph(E3Graph graph, String fileName) {
		// info: necessary for this to work:
		if (!registered)
			registerCodecs();

		try {
			// taken from EditorActions class
			mxCodec codec = new mxCodec();
			String xml = mxXmlUtils.getXml(codec.encode(graph.getModel()));
			
			JsonArrayBuilder valueObjectsJson = Json.createArrayBuilder();
			for (String vo : graph.valueObjects) {
				valueObjectsJson.add(vo);
			}
					
			String properties = Json.createObjectBuilder()
					.add("fraud", graph.isFraud)
					.add("title", graph.title == null ? "" : graph.title)
					.add("valueObjects", valueObjectsJson)
					.build().toString();
			
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
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
