package design.main.export;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import design.main.E3Graph;

public class JSONExport {
	private E3Graph graph;
	private String name;

	public JSONExport(E3Graph graph, String name) {
		this.graph = graph;
		this.name = name;
	}
	
	public String generateJSON() {
		JsonObjectBuilder main = Json.createObjectBuilder();
		
		main.add("name", name);
				
		if (graph.isFraud) {
			main.add("type", "fraud");
		} else {
			main.add("type", "value");
		}
		
		
		
		return "kek";
	}
}
