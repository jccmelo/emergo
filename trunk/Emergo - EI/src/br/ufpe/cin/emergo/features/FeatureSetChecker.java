package br.ufpe.cin.emergo.features;

import java.util.Set;

public interface FeatureSetChecker {

	public boolean check(Set<String> trueSet, Set<String> falseSet);

}
