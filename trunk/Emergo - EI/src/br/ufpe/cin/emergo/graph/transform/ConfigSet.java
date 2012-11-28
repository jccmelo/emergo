package br.ufpe.cin.emergo.graph.transform;

public interface ConfigSet {

	public ConfigSet not();

	public ConfigSet or(ConfigSet other);

	public ConfigSet and(ConfigSet other);
	
	public boolean isTrueSet();

	boolean isEmpty();

	public boolean isValid();
}