package br.ufpe.cin.emergo.core;

import java.io.FileNotFoundException;
import java.util.Map;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;

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
	 * @throws FileNotFoundException
	 */
	public static DirectedGraph<Object, ValueContainerEdge> findFromSelection(DependencyFinderID finder, SelectionPosition selectionPosition, Map<Object, Object> options) throws FileNotFoundException {
		switch (finder) {
		case JWCOMPILER:
			return new JWCompilerDependencyFinder(selectionPosition, options).getGraph();
		case SOOT:
		default:
			throw new UnsupportedOperationException("Depency finder unavailable");
		}
	}
}
