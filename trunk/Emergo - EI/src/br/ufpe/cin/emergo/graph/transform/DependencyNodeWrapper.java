package br.ufpe.cin.emergo.graph.transform;

import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.core.SelectionPosition;
import br.ufpe.cin.emergo.graph.DependencyNode;
import br.ufpe.cin.emergo.instrument.IConfigRep;
import br.ufpe.cin.emergo.instrument.IFeatureRep;


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
	private final SelectionPosition selectionPosition;
	private final boolean isInSelection;
	private final IConfigRep configRep;
	private IFeatureRep featureRep;

	public DependencyNodeWrapper(T data, boolean isInSelection, SelectionPosition selectionPosition, IConfigRep configRep, IFeatureRep featureRep) {
		this.data = data;
		this.isInSelection = isInSelection;
		this.selectionPosition = selectionPosition;
		if (configRep == null)
			throw new IllegalArgumentException("Argument configRep cannot be null.");
		this.configRep = configRep;
		this.featureRep = featureRep;
	}

	@Override
	public ConfigSet getConfigSet() {
		return new ConfigSetImpl(configRep, featureRep);
	}
	
	@Override
	public IConfigRep getFeatureSet() {
		return configRep;
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
		result = prime * result + ((configRep == null) ? 0 : configRep.hashCode());
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
		if (configRep == null) {
			if (other.configRep != null)
				return false;
		} else if (!configRep.equals(other.configRep))
			return false;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		
		return true;
	}

	@Override
	public boolean isInSelection() {
		return this.isInSelection;
	}

	@Override
	public SelectionPosition getPosition() {
		return this.selectionPosition;
	}

}
