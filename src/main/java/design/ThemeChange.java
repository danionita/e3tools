package design;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxIGraphModel.mxAtomicGraphModelChange;

import design.Utils.IsEntityFilter;
import design.info.Actor;
import design.info.Base;
import design.info.ConnectionElement;
import design.info.MarketSegment;
import design.info.ValueActivity;
import design.info.ValueExchange;

public class ThemeChange extends mxAtomicGraphModelChange {
	private E3GraphComponent graphComponent;
	private E3Graph graph;
	private ToolComponent tc;
	private Map<Object, String> oldStyles = new HashMap<>();
	private Map<Object, String> newStyles = new HashMap<>();
	private E3Style oldStyle;
	private E3Style newStyle;

	ThemeChange(E3GraphComponent graphComponent, ToolComponent tc, E3Style newStyle, boolean preserveSpecificColoring) {
		super(graphComponent.getGraph().getModel());
		
		this.graphComponent = graphComponent;
		this.graph = (E3Graph) graphComponent.getGraph();
		this.tc = tc;
		this.oldStyle = graph.style;
		this.newStyle = newStyle;
		
		oldStyles.putAll(Utils.getAllCells(graph).stream()
				.filter(obj -> {
					Object value = graph.getModel().getValue(obj);
					return value instanceof Actor
							|| value instanceof MarketSegment
							|| value instanceof ValueActivity
							|| value instanceof ConnectionElement
							|| value instanceof ValueExchange;
				})
				.collect(Collectors.toMap(obj -> obj, obj -> {
					return ((mxCell) obj).getStyle();
				})));
		
		newStyles.putAll(Utils.getAllCells(graph).stream()
				.filter(obj -> {
					Object value = graph.getModel().getValue(obj);
					return value instanceof Actor
							|| value instanceof MarketSegment
							|| value instanceof ValueActivity
							|| value instanceof ConnectionElement
							|| value instanceof ValueExchange;
				})
				.collect(Collectors.toMap(obj -> obj, obj -> {
					if (preserveSpecificColoring) {
						return ((mxCell) obj).getStyle();
					} else {
						Base info = Utils.base(graph, obj);
						if (info instanceof Actor && ((Actor) info).colluded) {
							return "ColludedActor";
						} else {
							System.out.println(info.getClass().getSimpleName());
							return info.getClass().getSimpleName();
						}				
					}
				})));
	}
	
	@Override
	public void execute() {
		// Do the toolcomponent
		newStyle.styleGraphComponent(tc);
		
		// Do the graphComponent
		newStyle.styleGraphComponent(graphComponent);
		
		// Apply correct styles
		// We do this "in-place" (so no funky beginUpdate()
		// or endUpdate() calls) because we're in execute().
		// If you do any calls that do begin/endUpdate() or
		// something similar it'll look like the editor is
		// forgetting undo/redo states. Which sucks!
		// Mind you: if you do something in-place, you are also
		// responsible for undoing something in-place.
		// (Which is the case here)
		newStyles.keySet().stream()
			.forEach(obj -> {
				((mxCell) obj).setStyle(newStyles.get(obj));
			});
		
		// Set the correct style within the graph
		graph.style = newStyle;
		
		// Switch new and old around
		E3Style tmp = oldStyle;
		oldStyle = newStyle;
		newStyle = tmp;
		
		Map<Object, String> tmp2 = oldStyles;
		oldStyles = newStyles;
		newStyles = tmp2;
		
		graphComponent.refresh();
	}
}
