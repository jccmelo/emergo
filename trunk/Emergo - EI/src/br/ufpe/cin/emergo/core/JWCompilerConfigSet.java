package br.ufpe.cin.emergo.core;

import org.apache.commons.collections.map.LRUMap;

import dk.au.cs.java.compiler.ifdef.IfDefVarSet;

/**
 * Represents a set of features using by wrapping around Johnni Winther's
 * Experimental Compiler IfDefVarSet.
 * 
 * Use the static method of to make use of the embedded caching mechanism
 * instead of creating a new instance of this class.
 * 
 * @author Társis
 * 
 */
public class JWCompilerConfigSet implements ConfigSet {

	/*
	 * The immutability of the IfDefVarSet class allows for an implementation of
	 * a simple caching mechanism. If the client wants to bypass the caching
	 * mechanism, he/she can do so by invoking the constructor directly.
	 * Otherwise, the static method of can be used.
	 */
	private static final LRUMap cache = new LRUMap(16);

	private final IfDefVarSet varSet;

	/**
	 * @return the varSet
	 */
	// XXX Temporarily exposing the varset for a temporary workaround.
	public IfDefVarSet getVarSet() {
		return varSet;
	}

	/**
	 * Public constructor.
	 * 
	 * @param varSet
	 * @throws IllegalArgumentException if varSet is null
	 */
	public JWCompilerConfigSet(IfDefVarSet varSet) {
		if (varSet == null)
			throw new IllegalArgumentException("Argument varSet cannot be null ");
		this.varSet = varSet;
	}

	/**
	 * Returns an instance of JWCompilerConfigSet, but using a simple caching
	 * mechanism.
	 * 
	 * @param varSet
	 * @throws IllegalArgumentException if varSet is null
	 * @return and instance of JWCompilerConfigSet
	 */
	public static JWCompilerConfigSet of(IfDefVarSet varSet) {
		if (varSet == null)
			throw new IllegalArgumentException("Argument varSet cannot be null. ");
		JWCompilerConfigSet cacheShot = (JWCompilerConfigSet) cache.get(varSet);
		if (cacheShot == null) /* MISS? */{
			JWCompilerConfigSet cacheElement = new JWCompilerConfigSet(varSet);
			cache.put(varSet, cacheElement);
			return cacheElement;
		} /* HIT! */else {
			return cacheShot;
		}
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
			throw new IllegalArgumentException("Operation or between types "
					+ JWCompilerConfigSet.class + " and " + other.getClass()
					+ " not supported");
		}
	}

	@Override
	public ConfigSet and(ConfigSet other) {
		if (other instanceof JWCompilerConfigSet) {
			IfDefVarSet otherVarSet = ((JWCompilerConfigSet) other).varSet;
			return new JWCompilerConfigSet(varSet.and(otherVarSet));
		} else {
			throw new IllegalArgumentException("Operation and between types "
					+ JWCompilerConfigSet.class + " and " + other.getClass()
					+ " not supported");
		}
	}

	@Override
	public String toString() {
		return varSet.toString();
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
		result = prime * result + ((varSet == null) ? 0 : varSet.hashCode());
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

	@Override
	public boolean isEmpty() {
		return this.varSet.isEmpty();
	}

	@Override
	public boolean isValid() {
		return this.varSet.isValidInFeatureModel();
	}

}
