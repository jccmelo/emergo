package br.ufpe.cin.emergo.core;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.jgrapht.DirectedGraph;

import br.ufpe.cin.emergo.graph.DependencyNode;
import br.ufpe.cin.emergo.graph.ValueContainerEdge;

/**
 * A façade class for invoking the process of dependency discovery.
 * 
 * @author Társis
 * 
 */
public class DependencyFinder {

	/**
	 * Defeats instantiation.
	 */
	private DependencyFinder() {
	}

	// XXX apply switching strategy for variability.
	public static Map<ConfigSet, Collection<Integer>> getIfDefLineMapping(File file) {
		return JWCompilerDependencyFinder.ifDefBlocks(file);
	}

	/**
	 * Unleash the depedency-finding process based on the {@code DependencyFinder finder} for a given user selection (
	 * {@code SelectionPosition selectionPosition}). Further information can be passed as parameters to the
	 * {@code options Map}.
	 * 
	 * The return is a directed graph representing the dependencies found.
	 * 
	 * @param finder
	 * @param selectionPosition
	 * @param options
	 * @return
	 * @throws EmergoException
	 */
	public static DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> findFromSelection(DependencyFinderID finder, SelectionPosition selectionPosition, Map<Object, Object> options) throws EmergoException {
		switch (finder) {
		case JWCOMPILER:
			return JWCompilerDependencyFinder.generateDependencyGraph(selectionPosition, options);
		case SOOT:
		default:
			throw new UnsupportedOperationException("Depency finder unavailable");
		}
	}
}
