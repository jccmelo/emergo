package br.ufpe.cin.emergo.graph.transform;

import org.jgrapht.graph.DefaultEdge;

/**
 * An edge that may contain a (parametric) value.
 * 
 * @author T�rsis
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ValueContainerEdge other = (ValueContainerEdge) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	
}
