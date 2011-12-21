package br.ufpe.cin.emergo.core;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.Range;
import org.jgrapht.DirectedGraph;

import dk.au.cs.java.compiler.node.AProgram;
import dk.au.cs.java.compiler.node.PIfdefExp;

import br.ufpe.cin.emergo.graph.DependencyNode;
import br.ufpe.cin.emergo.graph.ValueContainerEdge;

/**
 * A fa�ade class for invoking the process of dependency discovery.
 * 
 * @author T�rsis
 * 
 */
public class DependencyFinder {

	/**
	 * Defeats instantiation.
	 */
	private DependencyFinder() {
	}

	// XXX apply switching strategy for variability.
	public static Map<ConfigSet, Collection<Range<Integer>>> getIfDefLineMapping(File file) {
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
	 * @param interprocedural 
	 * @return
	 * @throws EmergoException
	 */
	public static DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> findFromSelection(DependencyFinderID finder, SelectionPosition selectionPosition, Map<Object, Object> options, boolean interprocedural) throws EmergoException {
		switch (finder) {
		case JWCOMPILER:
			return JWCompilerDependencyFinder.generateDependencyGraph(selectionPosition, options, interprocedural);
		case SOOT:
		default:
			throw new UnsupportedOperationException("Depency finder unavailable");
		}
	}

	public static Map<PIfdefExp, Collection<Range<Integer>>> getIfDefLineMapping(File file, AProgram rootNode) {
		return JWCompilerDependencyFinder.ifDefBlocks(file, rootNode);
	}
}
