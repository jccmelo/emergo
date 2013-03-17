package br.ufpe.cin.emergo.graph;

import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.core.SelectionPosition;
import br.ufpe.cin.emergo.instrument.IConfigRep;

public interface DependencyNode {
	public SelectionPosition getPosition();

	public boolean isInSelection();
	
	public ConfigSet getConfigSet();
	
	public IConfigRep getFeatureSet();
}
