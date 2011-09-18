package br.ufpe.cin.emergo.graph;

import br.ufpe.cin.emergo.core.SelectionPosition;

/**
 * This is a simple wrapper class meant to be used as nodes in the dependency graph. It is capable of holding an
 * arbitrary and parametric instance, aswell as position information regarding the parametric instance. This should be
 * enough for some very basic abstractions that the dependency graph might need for now.
 * 
 * @author Társis Toledo
 * 
 * @param <T>
 */
public class DependencyNode<T> {
	private final T data;
	private final SelectionPosition position;

	public DependencyNode(T data, SelectionPosition position) {
		this.data = data;
		this.position = position;
	}

	/**
	 * Gets the data wrapped inside this instance.
	 * 
	 * @return
	 */
	public T getData() {
		return data;
	}

	/**
	 * Gets the position information.
	 * 
	 * @return
	 */
	public SelectionPosition getPosition() {
		return this.position;
	}

	@Override
	public String toString() {
		return position.toString() + " " + data.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + ((position == null) ? 0 : position.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof DependencyNode))
			return false;
		DependencyNode<Object> other = (DependencyNode<Object>) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		if (position == null) {
			if (other.position != null)
				return false;
		} else if (!position.equals(other.position))
			return false;
		return true;
	}

}
