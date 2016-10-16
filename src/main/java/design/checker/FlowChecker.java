package design.checker;

import design.E3Graph;
import design.Utils;
import design.info.StartSignal;

public class FlowChecker extends E3Walker {

	public FlowChecker(E3Graph graph) {
		super(graph);
		
		Utils.getAllCells(graph).stream()
			.filter(obj -> graph.getModel().getValue(obj) instanceof StartSignal)
			.forEach(obj -> {
				System.out.println("\n\n\nChecking");
				checkPath(obj);
			});
	}
	
	@Override
	public void visitStartSignal(Object ss) {
		System.out.println("Start signal");
	}
	
	/**
	 * @param dot
	 * @param in True if the connection element is upstream
	 */
	@Override
	public void visitLogicDot(Object dot, boolean in) {
		System.out.println("Logic dot");
	}
	
	/**
	 * @param dot
	 * @param in True if the connection element is upstream
	 */
	@Override
	public void visitSignalDot(Object dot, boolean in) {
		System.out.println("Signal dot");
	}
	
	/**
	 * @param vp
	 * @param in True if upstream is the value exchange
	 */
	@Override
	public void visitValuePort(Object vp, boolean in) {
		System.out.println("Value port");
	}

	@Override
	public void visitValueExchange(Object vpUp, Object ve, Object vpDown) {
		System.out.println("Value exchange");
	}

	@Override
	public void visitConnectionElement(Object dotUp, Object ce, Object dotDown) {
		System.out.println("Connection element");
	}
	
	/**
	 * @param gate
	 * @param narrowing True if upstream is at the non-unit dot's
	 * side. This means that the side where the user can add or remove
	 * input dots is upstream.
	 */
	@Override
	public void visitLogicBase(Object gate, boolean narrowing) {
		System.out.println("Logic base");
	}
	
	/**
	 * @param vi
	 * @param in True if the direction of value exchanges is upstream
	 */
	@Override
	public void visitValueInterface(Object vi, boolean in) {
		System.out.println("Value interface");
	}
	
	@Override
	public void visitEndSignal(Object es) {
		System.out.println("End signal");
	}
}
