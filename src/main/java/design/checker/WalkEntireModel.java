package design.checker;

import java.util.HashSet;
import java.util.Set;

import design.E3Graph;
import design.Utils;
import design.info.Base;
import design.info.StartSignal;

public class WalkEntireModel extends E3Walker {

	public WalkEntireModel(E3Graph graph) {
		super(graph);
		
		Utils.getAllCells(graph).stream()
			.filter(obj -> graph.getModel().getValue(obj) instanceof StartSignal)
			.forEach(obj -> {
				System.out.println("\n\n\nChecking");
				checkPath(obj);
				System.out.println("Done checking");
			});
	}
	
	public long getSUID(Object o) {
		return ((Base) graph.getModel().getValue(o)).SUID;
	}
	
	@Override
	public void visitStartSignal(Object ss) {
		System.out.println("Start signal #" + getSUID(ss));
	}
	
	/**
	 * @param dot
	 * @param in True if the connection element is upstream
	 */
	@Override
	public void visitLogicDot(Object dot, boolean in) {
		System.out.println("Logic dot #" + getSUID(dot));
	}
	
	/**
	 * @param dot
	 * @param in True if the connection element is upstream
	 */
	@Override
	public void visitSignalDot(Object dot, boolean in) {
		System.out.println("Signal dot #" + getSUID(dot));
	}
	
	/**
	 * @param vp
	 * @param prev Previous element (ve or vi)
	 */
	@Override
	public void visitValuePort(Object vp, Object prev) {
		System.out.println("Value port #" + getSUID(vp));
	}

	@Override
	public void visitValueExchange(Object vpUp, Object ve, Object vpDown) {
		System.out.println("Value exchange #" + getSUID(ve));
	}

	@Override
	public void visitConnectionElement(Object dotUp, Object ce, Object dotDown) {
		System.out.println("Connection element #" + getSUID(ce));
	}
	
	/**
	 * @param gate
	 * @param narrowing True if upstream is at the non-unit dot's
	 * side. This means that the side where the user can add or remove
	 * input dots is upstream.
	 */
	@Override
	public void visitLogicBase(Object gate, boolean narrowing) {
		System.out.println("Logic base" + getSUID(gate));
	}
	
	/**
	 * @param vi
	 * @param in True if the direction of value exchanges is upstream
	 */
	@Override
	public void visitValueInterface(Object vi, boolean in) {
		System.out.println("Value interface #" + getSUID(vi));
	}
	
	@Override
	public void visitEndSignal(Object es) {
		System.out.println("End signal #" + getSUID(es));
	}
	
	@Override
	public void visitBadStartSignal(Object es) {
		System.out.println("Bad start signal #" + getSUID(es));
	}
}
