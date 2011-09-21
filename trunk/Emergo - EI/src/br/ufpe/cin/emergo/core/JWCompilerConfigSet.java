package br.ufpe.cin.emergo.core;

import dk.au.cs.java.compiler.ifdef.IfDefVarSet;

/**
 * Represents a set of features using the representation used by Johnni Winther's Experimental Compiler.
 * 
 * @author Társis
 *
 */
public class JWCompilerConfigSet implements ConfigSet {

	private final IfDefVarSet varSet;
	

	/**
	 * @return the varSet
	 */
	//XXX Temporarily exposing the varset for a temporary workaround.
	public IfDefVarSet getVarSet() {
		return varSet;
	}

	public JWCompilerConfigSet(IfDefVarSet varSet) {
		this.varSet = varSet;
	}

	@Override
	public ConfigSet not() {
		return new JWCompilerConfigSet(varSet.not());
	}

	@Override
	public ConfigSet or(ConfigSet other) {
		if (other instanceof JWCompilerConfigSet) {
			IfDefVarSet otherVarSet = ((JWCompilerConfigSet) other).varSet;
			return new JWCompilerConfigSet(varSet.or(otherVarSet));
		} else {
			throw new IllegalArgumentException("Operation or between types " + JWCompilerConfigSet.class + " and " + other.getClass() + " not supported");
		}
	}

	@Override
	public ConfigSet and(ConfigSet other) {
		if (other instanceof JWCompilerConfigSet) {
			IfDefVarSet otherVarSet = ((JWCompilerConfigSet) other).varSet;
			return new JWCompilerConfigSet(varSet.and(otherVarSet));
		} else {
			throw new IllegalArgumentException("Operation and between types " + JWCompilerConfigSet.class + " and " + other.getClass() + " not supported");
		}
	}
	
	@Override
	public String toString() {
		return varSet.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((varSet == null) ? 0 : varSet.hashCode());
		return result;
	}

	/* (non-Javadoc)
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
		JWCompilerConfigSet other = (JWCompilerConfigSet) obj;
		if (varSet == null) {
			if (other.varSet != null)
				return false;
		} else if (!varSet.equals(other.varSet))
			return false;
		return true;
	}

	@Override
	public boolean isTrueSet() {
		return this.varSet.equals(IfDefVarSet.getAll());
	}

	
	
}
