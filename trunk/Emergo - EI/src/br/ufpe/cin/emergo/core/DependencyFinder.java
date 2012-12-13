package br.ufpe.cin.emergo.core;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.Range;
import org.jgrapht.DirectedGraph;

import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

import dk.au.cs.java.compiler.node.AProgram;
import dk.au.cs.java.compiler.node.PIfdefExp;

import br.ufpe.cin.emergo.analysis.SootManager;
import br.ufpe.cin.emergo.analysis.reachingdefs.LiftedReachingDefinitions;
import br.ufpe.cin.emergo.compiler.Compiler;
import br.ufpe.cin.emergo.graph.DependencyNode;
import br.ufpe.cin.emergo.graph.ValueContainerEdge;
import br.ufpe.cin.emergo.graph.transform.DependencyGraphBuilder;
import br.ufpe.cin.emergo.instrument.EagerConfigTag;
import br.ufpe.cin.emergo.instrument.FeatureInstrumentor;
import br.ufpe.cin.emergo.instrument.IConfigRep;
import br.ufpe.cin.emergo.util.MethodDeclarationSootMethodBridge;

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
	 * Unleash the dependency-finding process based on the {@code DependencyFinder finder} for a given user selection (
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
	public static DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> findFromSelection(SelectionPosition selectionPosition, Map<Object, Object> options) throws EmergoException {
		try {
		//#ifdef JWCompiler
//			return JWCompilerDependencyFinder.generateDependencyGraph(selectionPosition, options);
		//#else
			
			/*
             * Initialize and configure Soot's options and find out which method
             * contains the selection
             */
//            SootManager.configure("/Users/paolaaccioly/Documents/runtime-EclipseApplication/JCalc/bin/");
			Object cp = options.get("correspondentClasspath");
			SootManager.configure(cp.toString());
			
			String clazz = retrieveClassName(selectionPosition.getFilePath());
    		SootClass c = SootManager.loadAndSupport(clazz);
    		
    		// Retrieve the method and its body
    		SootMethod m = c.getMethodByName("initResolution");
    		Body b = m.retrieveActiveBody();
    		
    		/*
    		 * Instruments bytecode of the output class
    		 */
    		new FeatureInstrumentor().transform(b);
    		
    		/*
    		 * Builds the CFG and runs the analysis
    		 */
    		EagerConfigTag configTag = (EagerConfigTag) b
    				.getTag(EagerConfigTag.TAG_NAME);

    		if (configTag == null) {
    			throw new IllegalStateException(
    					"No EagerConfigTag found for body of method "
    							+ b.getMethod());
    		}

    		Set<IConfigRep> configReps = configTag.getConfigReps();
    		if (configReps.size() < 1) {
    			throw new Exception("configReps less than 1.");
    		}

    		UnitGraph bodyGraph = new BriefUnitGraph(b);
    		
    		LiftedReachingDefinitions liftedReachingDefinitions = new LiftedReachingDefinitions(bodyGraph, configReps);
            
    		/*
    		 * Gets one DependencyGraphBuilder instance, then generates it.
    		 */
    		DependencyGraphBuilder builder = new DependencyGraphBuilder();
    		
    		selectionPosition = selectionPosition.builder().startLine(49).endLine(49).startColumn(9).endColumn(76).length(selectionPosition.getLength()).offSet(selectionPosition.getOffSet()).filePath(selectionPosition.getFilePath()).build();
    		
			return builder.generateDependencyGraph(bodyGraph, liftedReachingDefinitions, selectionPosition);
		//#endif
		} catch (Exception e) {
			throw new UnsupportedOperationException("Dependency finder unavailable");
		}
	}

	//XXX It should not be coupled to JW-Compiler
	public static Map<PIfdefExp, Collection<Range<Integer>>> getIfDefLineMapping(File file, AProgram rootNode) {
		return JWCompilerDependencyFinder.ifDefBlocks(file, rootNode);
	}
	
	/**
     * This method retrieves the classname through
     * attribute classCompleteName.
     * @return the classname
     */
    public static String retrieveClassName(String completeName) {
    	//Getting all parts of the path+classname+extension
    	String[] parts = completeName.split("/");
    	//Getting the classname without extension as well as path
    	String className = parts[parts.length-1].split("\\.")[0];
    	
    	return className;
	}
}
