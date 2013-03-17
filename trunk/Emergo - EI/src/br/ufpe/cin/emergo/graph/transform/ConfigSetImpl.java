package br.ufpe.cin.emergo.graph.transform;

import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.instrument.IConfigRep;
import br.ufpe.cin.emergo.instrument.IFeatureRep;


public class ConfigSetImpl implements ConfigSet {

	private IConfigRep configRep;
	private IFeatureRep featureRep;
	
	public ConfigSetImpl() {
		
	}
	
	public ConfigSetImpl(IConfigRep configRep) {
		this.configRep = configRep;
	}
	
	public ConfigSetImpl(IConfigRep configRep, IFeatureRep featureRep) {
		this.configRep = configRep;
		this.featureRep = featureRep;
	}


	@Override
	public ConfigSet not() {
//		new ConfigSetImpl(configRep, featureRep);
		assert (configRep.hashCode() != -1);
		return null;
	}
	
	@Override
	public ConfigSet or(ConfigSet other) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public ConfigSet and(ConfigSet other) {
		if (other instanceof ConfigSetImpl) {
			IConfigRep otherVarSet = ((ConfigSetImpl) other).configRep;
			return new ConfigSetImpl(configRep.union(otherVarSet), this.featureRep);
		} else {
			throw new IllegalArgumentException("Operation and between types "
					+ ConfigSetImpl.class + " and " + other.getClass()
					+ " not supported");
		}
	}
	
	@Override
	public boolean isTrueSet() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String toString() {
		return configRep.toString();
	}
}
