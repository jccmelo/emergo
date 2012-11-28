package br.ufpe.cin.emergo.graph.transform;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedMultigraph;

import soot.Unit;
import soot.jimple.internal.JAssignStmt;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import br.ufpe.cin.emergo.preprocessor.ContextManager;

public class DependencyGraphBuilder {

	/**
	 * This method generates the data dependency graph
	 * 
	 * @param cfg
	 * @param analysisResult
	 * @return
	 */
	public DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> generateDependencyGraph(
			UnitGraph cfg, ForwardFlowAnalysis<Unit, ? extends FlowSet> analysis) {

		// This graph will be return
		DirectedMultigraph<DependencyNode, ValueContainerEdge<ConfigSet>> dependencyGraph = new DirectedMultigraph<DependencyNode, ValueContainerEdge<ConfigSet>>((Class<? extends ValueContainerEdge<ConfigSet>>) ValueContainerEdge.class);
		
		// List of nodes to be visited
		List<DependencyNode> visitedNodes = new ArrayList<DependencyNode>();
		
		// Iterate over the results
		Iterator i = cfg.iterator();
		
		// Computes the analysis results
		while (i.hasNext()) {
			// Gets a node/unit
			Unit u = (Unit) i.next();
			// Gets value of OUT set for unit
			FlowSet out = analysis.getFlowAfter(u);
			
			//==================TODO=================
			// gets the set of features from line of code
			Set<String> setFeatures = ContextManager.getContext().getFeaturesByLine(ContextManager.getLineNumberForUnit(u));
			
			ConfigSet configSet = new ConfigSetImpl(); //without feature
			//=======================================
			
			// Creates the node
			DependencyNode node = new DependencyNodeWrapper<Unit>(u, configSet);
			dependencyGraph.addVertex(node);
			
			visitedNodes.add(node);
		}
		
		// Iterates over all nodes of the graph
		for (int j = 0; j < visitedNodes.size(); j++) {
			
			DependencyNode currentNode = visitedNodes.get(j);
			
			if (isDef(currentNode)) {
				// Now, currentNode is def
				// Gets the all uses for this def
				List<DependencyNode> uses = getUse(visitedNodes, currentNode);
				
				// for each use found.. creates one directed edge (def -> use)
				for (DependencyNode use : uses) {
					connectVertices(dependencyGraph, use, currentNode);
				}
			}
		}

		// TODO: removes the nodes which have not edge

		return dependencyGraph;
	}

	private List<DependencyNode> getUse(List<DependencyNode> nodes, DependencyNode currentNode) {
		List<DependencyNode> uses = new ArrayList<DependencyNode>();
		
		Unit unit = (Unit) currentNode.getData();
		JAssignStmt def = (JAssignStmt) unit;
		
		for (Iterator it = nodes.iterator(); it.hasNext();) {
			DependencyNode useCandidateNode = (DependencyNode) it.next();
			
			Unit u = (Unit) useCandidateNode.getData();
			JAssignStmt stmt = (JAssignStmt) u;
			
			if (stmt.getRightOp().toString().contains(def.getLeftOp().toString())) {
				uses.add(useCandidateNode);
			}
			
		}
		
		return uses;
	}

	private boolean isDef(DependencyNode dependencyNode) {
		
		Unit u = (Unit)dependencyNode.getData();
		
		if(u instanceof JAssignStmt) {
			JAssignStmt stmt = (JAssignStmt) u;
			
			if (stmt.getLeftOp() != null && stmt.getRightOp() != null){ // it is def
				return true;
			} else { // it is use
				return false;
			}
		}
		return false;
	}

	/**
	 * Creates an edge between {@code source} and {@code target} objects in
	 * {@code graph} for the configuration set {@code key} to represent a
	 * dependency between these vertices. If an edge already exists between
	 * these vertices, then instead of creating a new edge, the old one is
	 * replaced by a new one containing both the old configuration sets and the
	 * new configuration set.
	 * 
	 * @param graph
	 * @param use
	 * @param def
	 */
	private static void connectVertices(
			final Graph<DependencyNode, ValueContainerEdge<ConfigSet>> graph,
			final DependencyNode use, DependencyNode def) {

		/*
		 * Counting on the graph's implementation to check for the existance of
		 * the nodes before adding to avoid duplicate vertices.
		 */
		graph.addVertex(use);
		graph.addVertex(def);

		
		ValueContainerEdge<ConfigSet> addedEdge = graph.addEdge(def, use);
		
		
		
		/*
		 * To avoid having more than one edge between two given nodes, the
		 * information contained in these edges, internally an IfDefVarSet
		 * instance, is merged by using the OR operator.
		 */
//		if (graph.containsEdge(def, use)) {
//			ValueContainerEdge<ConfigSet> existingEdge = graph.getEdge(def, use);
//			ConfigSet existingConfigSet = (ConfigSet) existingEdge.getValue();
//			// ConfigSet or = existingConfigSet.or(new
//			// JWCompilerConfigSet(configurationMean));
//
//			// TODO: is checking against the feature model necessary? It won't
//			// hurt to leave this here though.
//			// if (((JWCompilerConfigSet)
//			// or).getVarSet().isValidInFeatureModel())
//			// existingEdge.setValue(or);
//
//		} else {
//			// JWCompilerConfigSet sourceConfigAndMean = new
//			// JWCompilerConfigSet(configurationMean.and(use.getVarSet()).and(def.getVarSet()));
//			// if (sourceConfigAndMean.isEmpty() ||
//			// !sourceConfigAndMean.getVarSet().isValidInFeatureModel()) {
//			// return;
//			// }
//			// if (configurationMean.isValidInFeatureModel()) {
//			ValueContainerEdge<ConfigSet> addedEdge = graph.addEdge(def, use);
//			// addedEdge.setValue(sourceConfigAndMean);
//			// }
//		}
	}

}
