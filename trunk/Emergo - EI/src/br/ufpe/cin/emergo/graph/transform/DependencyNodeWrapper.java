package br.ufpe.cin.emergo.graph.transform;

import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.core.SelectionPosition;
import br.ufpe.cin.emergo.graph.DependencyNode;


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
	private final ConfigSet configSet;

	public DependencyNodeWrapper(T data, SelectionPosition position, ConfigSet config) {
		this.data = data;
		this.position = position;
		if (config == null)
			throw new IllegalArgumentException("Argument config cannot be null ");
		this.configSet = config;
	}

	@Override
	public ConfigSet getConfigSet() {
		return configSet;
	}
	
	@Override
	public ConfigSet getFeatureSet() {
		return configSet;
	}

//	public DependencyNodeWrapper<T> setFeatureSet(ConfigSet configSet){
//		if (configSet == null)
//			throw new IllegalArgumentException("Argument featureSet cannot be null ");
//		
//		this.configSet = configSet;
//		return this;
//	}


	/**
	 * Gets the data wrapped inside this instance.
	 * 
	 * @return
	 */
	public T getData() {
		return data;
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
		result = prime * result + ((configSet == null) ? 0 : configSet.hashCode());
		result = prime * result + ((data == null) ? 0 : data.hashCode());
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
		if (configSet == null) {
			if (other.configSet != null)
				return false;
		} else if (!configSet.equals(other.configSet))
			return false;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		
		return true;
	}

	@Override
	public SelectionPosition getPosition() {
		return this.position;
	}

	@Override
	public boolean isInSelection() {
		// TODO Auto-generated method stub
		return false;
	}

}
