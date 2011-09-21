package br.ufpe.cin.emergo.graph;

import org.jgrapht.graph.DefaultEdge;

/**
 * An edge that may contain a (parametric) value.
 * 
 * @author Társis
 * 
 * @param <S>
 */
public class ValueContainerEdge<S> extends DefaultEdge {

	private static final long serialVersionUID = 1L;

	/**
	 * The actual value referenced by an instance.
	 */
	private S value;

	/**
	 * Sets the value.
	 * 
	 * @param value
	 */
	public void setValue(S value) {
		this.value = value;
	}

	/**
	 * Gets the value.
	 * 
	 * @return
	 */
	public S getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value == null ? "" : value.toString();
	}
}
