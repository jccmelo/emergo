package br.ufpe.cin.emergo.core;

public interface ConfigSet {
	public ConfigSet not();

	public ConfigSet or(ConfigSet other);

	public ConfigSet and(ConfigSet other);
}
