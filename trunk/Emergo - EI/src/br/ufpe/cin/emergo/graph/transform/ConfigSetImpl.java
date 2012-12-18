package br.ufpe.cin.emergo.graph.transform;

import soot.toolkits.scalar.FlowSet;
import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.instrument.IConfigRep;
import br.ufpe.cin.emergo.instrument.IFeatureRep;


public class ConfigSetImpl implements ConfigSet {

	private FlowSet configSet; //Set of variables or ifdefs

	private IConfigRep configRep;
	private IFeatureRep featureRep;
	
	public ConfigSetImpl() {
		
	}
	
	public ConfigSetImpl(FlowSet configSet) {
		this.configSet = configSet;
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
		// TODO Auto-generated method stub
		return null;
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
		return "[featureRep = true]";
	}
}
