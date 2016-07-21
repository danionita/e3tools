package design.main.export;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import design.main.E3Graph;

public class JSONExport {
	private E3Graph graph;

	public JSONExport(E3Graph graph) {
		this.graph = graph;
	}
	
	public String generateJSON() {
		Json.createObjectBuilder();
		
		return "";
	}
}
