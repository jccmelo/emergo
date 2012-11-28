package br.ufpe.cin.emergo.graph.transform;

public interface DependencyNode {

	// public SelectionPosition getPosition();

	// public boolean isInSelection();

	public ConfigSet getConfigSet();

	public ConfigSet getFeatureSet();

	public Object getData();
}
