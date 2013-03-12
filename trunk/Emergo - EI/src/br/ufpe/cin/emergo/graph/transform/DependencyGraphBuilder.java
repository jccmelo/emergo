package br.ufpe.cin.emergo.graph.transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedMultigraph;

import soot.Local;
import soot.Unit;
import soot.jimple.DefinitionStmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JGotoStmt;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.core.EmergoException;
import br.ufpe.cin.emergo.core.SelectionPosition;
import br.ufpe.cin.emergo.graph.DependencyNode;
import br.ufpe.cin.emergo.graph.ValueContainerEdge;
import br.ufpe.cin.emergo.preprocessor.ContextManager;
import br.ufpe.cin.emergo.util.ASTNodeUnitBridge;
import br.ufpe.cin.emergo.util.ASTNodeUnitBridgeGroovy;

public class DependencyGraphBuilder {

	/**
	 * This method generates the data dependency graph
	 * 
	 * @param cfg
	 * @param analysisResult
	 * @return
	 */
	public DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> generateDependencyGraph(
			UnitGraph cfg,
			ForwardFlowAnalysis<Unit, ? extends FlowSet> analysis,
			Collection<Unit> unitsInSelection,
			SelectionPosition selectionPosition) {

		// This graph will be return
		DirectedMultigraph<DependencyNode, ValueContainerEdge<ConfigSet>> dependencyGraph = new DirectedMultigraph<DependencyNode, ValueContainerEdge<ConfigSet>>(
				(Class<? extends ValueContainerEdge<ConfigSet>>) ValueContainerEdge.class);

		// List of nodes to be visited
		List<DependencyNode> visitedNodes = new ArrayList<DependencyNode>();
		List<DependencyNode> defs = new ArrayList<DependencyNode>();

		ConfigSet configSet = new ConfigSetImpl(); // without feature

		// Iterate over the results
		Iterator i = cfg.iterator();
		
		System.out.println("Body\n\n"+cfg.getBody()+"\n\n");

		// Computes the analysis results
		while (i.hasNext()) {
			// Gets a node/unit
			Unit u = (Unit) i.next();
			DependencyNode node;
			
			if (u instanceof DefinitionStmt) {
				DefinitionStmt definition = (DefinitionStmt) u;
				Local leftOp = (Local) definition.getLeftOp();
				
				// exclude definitions when it's $temp on the leftOp.
				if (leftOp.getName().contains("$")) {
					continue;
				}

				node = createNode(unitsInSelection, selectionPosition, configSet, u);
				visitedNodes.add(node);
				
			} else if (u instanceof JInvokeStmt) {
				JInvokeStmt inv = (JInvokeStmt) u;
				for (Unit unit : unitsInSelection) {
					if(unit instanceof JAssignStmt){
						JAssignStmt assignStmt = (JAssignStmt) unit;
						Local leftOp = (Local) assignStmt.getLeftOp();
						
						List useBoxes = inv.getUseBoxes();
						for (Object use : useBoxes) {
							if(use.toString().contains(leftOp.toString())) {
								node = createNode(unitsInSelection, selectionPosition, configSet, u);
								visitedNodes.add(node);
								break;
							}
						}
					}
				}
			}

			// Gets value of OUT set for unit
			// FlowSet out = analysis.getFlowAfter(u);

			// ==================TODO=================
			// gets the set of features from line of code
			// Set<String> setFeatures =
			// ContextManager.getContext().getFeaturesByLine(ContextManager.getLineNumberForUnit(u));

			// =======================================
		}

		// Iterates over all nodes of the graph
		for (int j = 0; j < visitedNodes.size(); j++) {

			DependencyNode currentNode = visitedNodes.get(j);

			if (currentNode.isInSelection()) {
				// Now, currentNode is def
				// Gets all uses for this def
				List<DependencyNode> uses = getUse(visitedNodes, currentNode);
				
				//To avoid duplicate edges
				int tam = uses.size();
				for (int k = 0; k < tam; k++) {
					for (int l = k+1; l < tam; l++) {
						if(uses.get(k).getPosition().getStartLine() == uses.get(l).getPosition().getStartLine()){
							uses.remove(l);
						}
					}
				}

				// To avoid longer than one root node
				if (defs.isEmpty() || defs.get(0) == currentNode) {
					// for each use found.. creates one directed edge (def -> use)
					for (DependencyNode use : uses) {
						connectVertices(dependencyGraph, configSet, use, currentNode);
						defs.add(currentNode);
					}
				}
				
			}
		}

		// TODO: removes the nodes which have not edge

		return dependencyGraph;
	}

	private DependencyNode createNode(Collection<Unit> unitsInSelection,
			SelectionPosition selectionPosition, ConfigSet configSet, Unit u) {
		
		DependencyNode node;
		
		if (unitsInSelection.contains(u)) {
			node = new DependencyNodeWrapper<Unit>(u, true,	selectionPosition, configSet);

			if (!isDef(node)) {
				System.out.println("Invalid selection..");
				return null;
			}
		} else {
			int line = -1;
			if(selectionPosition.getFilePath().contains("java"))
				line = ASTNodeUnitBridge.getLineFromUnit(u);
			else
				line = ASTNodeUnitBridgeGroovy.getLineFromUnit(u);
			
			SelectionPosition pos = null;
			if (line != -1) {
				pos = new SelectionPosition(selectionPosition.getLength(), selectionPosition.getOffSet(), line, 
						selectionPosition.getStartColumn(), line, selectionPosition.getEndColumn(), selectionPosition.getFilePath());
			}
			node = new DependencyNodeWrapper<Unit>(u, false, pos, configSet);
		}
	
		return node;
	}

	private List<DependencyNode> getUse(List<DependencyNode> nodes,
			DependencyNode currentNode) {
		List<DependencyNode> uses = new ArrayList<DependencyNode>();

		Unit unit = ((DependencyNodeWrapper<Unit>) currentNode).getData();
		
		JAssignStmt def = (JAssignStmt) unit;

		for (Iterator it = nodes.iterator(); it.hasNext();) {
			DependencyNode useCandidateNode = (DependencyNode) it.next();

			Unit u = ((DependencyNodeWrapper<Unit>) useCandidateNode).getData();
			
			if (u instanceof JAssignStmt) {
				JAssignStmt stmt = (JAssignStmt) u;

				if (stmt.getRightOp().toString().contains(def.getLeftOp().toString())) {
					uses.add(useCandidateNode);
				}
			} else if (u instanceof JInvokeStmt) {
				JInvokeStmt inv = (JInvokeStmt) u;
			
				List useBoxes = inv.getUseBoxes();
				for (Object use : useBoxes) {
					if(use.toString().contains(def.getLeftOp().toString())) {
						uses.add(useCandidateNode);
						break;
					}
				}
			}
		}

		return uses;
	}

	private boolean isDef(DependencyNode dependencyNode) {

		Unit u = ((DependencyNodeWrapper<Unit>) dependencyNode).getData();

		if (u instanceof JAssignStmt) {
			JAssignStmt stmt = (JAssignStmt) u;

			if (stmt.getLeftOp() != null && stmt.getRightOp() != null) { 
				return true;
			} else {
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
			final ConfigSet configSet, final DependencyNode use,
			DependencyNode def) {

		/*
		 * Counting on the graph's implementation to check for the existance of
		 * the nodes before adding to avoid duplicate vertices.
		 */
		graph.addVertex(def);
		graph.addVertex(use);
		
		if (!graph.containsEdge(def, use)) {
			ValueContainerEdge<ConfigSet> addedEdge = graph.addEdge(def, use);
			addedEdge.setValue(configSet);
		}

		/*
		 * To avoid having more than one edge between two given nodes, the
		 * information contained in these edges, internally an IfDefVarSet
		 * instance, is merged by using the OR operator.
		 */
		// if (graph.containsEdge(def, use)) {
		// ValueContainerEdge<ConfigSet> existingEdge = graph.getEdge(def, use);
		// ConfigSet existingConfigSet = (ConfigSet) existingEdge.getValue();
		// // ConfigSet or = existingConfigSet.or(new
		// // JWCompilerConfigSet(configurationMean));
		//
		// // TODO: is checking against the feature model necessary? It won't
		// // hurt to leave this here though.
		// // if (((JWCompilerConfigSet)
		// // or).getVarSet().isValidInFeatureModel())
		// // existingEdge.setValue(or);
		//
		// } else {
		// // JWCompilerConfigSet sourceConfigAndMean = new
		// //
		// JWCompilerConfigSet(configurationMean.and(use.getVarSet()).and(def.getVarSet()));
		// // if (sourceConfigAndMean.isEmpty() ||
		// // !sourceConfigAndMean.getVarSet().isValidInFeatureModel()) {
		// // return;
		// // }
		// // if (configurationMean.isValidInFeatureModel()) {
		// ValueContainerEdge<ConfigSet> addedEdge = graph.addEdge(def, use);
		// // addedEdge.setValue(sourceConfigAndMean);
		// // }
		// }
	}

}
