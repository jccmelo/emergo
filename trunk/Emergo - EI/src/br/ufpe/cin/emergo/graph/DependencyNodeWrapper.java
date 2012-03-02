package br.ufpe.cin.emergo.graph;

import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.core.JWCompilerConfigSet;
import br.ufpe.cin.emergo.core.SelectionPosition;
import dk.au.cs.java.compiler.ifdef.IfDefVarSet;

/**
 * This is a simple wrapper class meant to be used as nodes in the dependency graph. It is capable of holding an
 * arbitrary and parametric instance, as well as position information regarding the parametric instance. This should be
 * enough for some very basic abstractions that the dependency graph might need for now.
 * 
 * @author T�rsis Toledo
 * 
 * @param <T>
 */
public class DependencyNodeWrapper<T> implements DependencyNode {
	private final T data;
	private final SelectionPosition position;
	private final boolean isInSelection;
	private final IfDefVarSet config;
	private IfDefVarSet featureSet;

	public DependencyNodeWrapper(T data, SelectionPosition position, boolean isInSelection, IfDefVarSet config) {
		this.data = data;
		this.position = position;
		this.isInSelection = isInSelection;
		this.config = config;
	}

	/**
	 * Returns the configuration.
	 * 
	 * @return
	 */
	public ConfigSet getConfigSet() {
		return new JWCompilerConfigSet(config);
	}

	/**
	 * Returns wether this node belongs to the selection or not.
	 * 
	 * @return true if this intance is within the selection, false otherwise.
	 */
	public boolean isInSelection() {
		return isInSelection;
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
		// return position.toString() + " " + data.toString();
		return position.toString();
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
		result = prime * result + ((config == null) ? 0 : config.hashCode());
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + (isInSelection ? 1231 : 1237);
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
		if (getClass() != obj.getClass())
			return false;
		DependencyNodeWrapper<?> other = (DependencyNodeWrapper<?>) obj;
		if (config == null) {
			if (other.config != null)
				return false;
		} else if (!config.equals(other.config))
			return false;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		if (isInSelection != other.isInSelection)
			return false;
		if (position == null) {
			if (other.position != null)
				return false;
		} else if (!position.equals(other.position))
			return false;
		return true;
	}
	
	public DependencyNodeWrapper<T> setFeatureSet(IfDefVarSet varSet){
		this.featureSet = varSet;
		return this;
	}

	@Override
	public ConfigSet getFeatureSet() {
		return new JWCompilerConfigSet(featureSet);
	}

}
