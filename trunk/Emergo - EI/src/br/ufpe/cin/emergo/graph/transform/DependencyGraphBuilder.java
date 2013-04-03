package br.ufpe.cin.emergo.graph.transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedMultigraph;

import soot.Local;
import soot.Unit;
import soot.jimple.DefinitionStmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.core.SelectionPosition;
import br.ufpe.cin.emergo.graph.DependencyNode;
import br.ufpe.cin.emergo.graph.ValueContainerEdge;
import br.ufpe.cin.emergo.instrument.FeatureTag;
import br.ufpe.cin.emergo.instrument.IConfigRep;
import br.ufpe.cin.emergo.instrument.IFeatureRep;
import br.ufpe.cin.emergo.util.ASTNodeUnitBridge;
import br.ufpe.cin.emergo.util.ASTNodeUnitBridgeGroovy;

public class DependencyGraphBuilder {
	
	private List<DependencyNode> defs = new ArrayList<DependencyNode>();
	private static boolean featureDependence = false;

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
			SelectionPosition selectionPosition,
			Set<IConfigRep> configReps,
			Map<Object, Object> options) {
		
		featureDependence = (Boolean) options.get("featureDependence");

		// This graph will be return
		DirectedMultigraph<DependencyNode, ValueContainerEdge<ConfigSet>> dependencyGraph = new DirectedMultigraph<DependencyNode, ValueContainerEdge<ConfigSet>>(
				(Class<? extends ValueContainerEdge<ConfigSet>>) ValueContainerEdge.class);

		// List of nodes to be visited
		List<DependencyNode> visitedNodes = new ArrayList<DependencyNode>();
		
		// Iterate over the results
		Iterator i = cfg.iterator();
		
		System.out.println("Body\n"+cfg.getBody()+"\nEnd body");

		// Computes the analysis results
		while (i.hasNext()) {
			// Gets a node/unit
			Unit u = (Unit) i.next();
			DependencyNode node;
			
			if (u instanceof DefinitionStmt) {
				DefinitionStmt definition = (DefinitionStmt) u;
				Local leftOp = (Local) definition.getLeftOp();
				// exclude definitions when it's $temp on the leftOp.
//				if (leftOp.getName().contains("$")) {
//					continue;
//				}
				
				FeatureTag tag = (FeatureTag) u.getTag("FeatureTag");
				
				for (IConfigRep confRep : configReps) {
					if(tag.getFeatureRep().belongsToConfiguration(confRep)) {
						node = createNode(unitsInSelection, selectionPosition, confRep, tag.getFeatureRep(), u);
						visitedNodes.add(node);
						break;
					}
				}
				
//				node = createNode(unitsInSelection, selectionPosition, configReps.iterator().next(), u);
//				visitedNodes.add(node);
				
			} else if (u instanceof JInvokeStmt) {
				FeatureTag tag = (FeatureTag) u.getTag("FeatureTag");
				
				for (IConfigRep confRep : configReps) {
					if(tag.getFeatureRep().belongsToConfiguration(confRep)) {
						node = createNode(unitsInSelection, selectionPosition, confRep, tag.getFeatureRep(), u);
						visitedNodes.add(node);
						break;
					}
				}
			}
		} // end while
		
		while (!defs.isEmpty()) {
			DependencyNode currentNode = defs.get(0);
			// Now, currentNode is def
			// Gets all uses for this def
			List<DependencyNode> uses = getUse(visitedNodes, currentNode);
			
			//To avoid duplicate edges
			for (int k = 0; k < uses.size(); k++) {
				for (int l = k+1; l < uses.size(); l++) {
					if(uses.get(k).getPosition().getStartLine() == uses.get(l).getPosition().getStartLine()){
						uses.remove(l--);
					}
				}
			}
			//For supporting transitivity property..
			for (int j = 0; j < uses.size(); j++) {
				if (isDef(uses.get(j))) {
					defs.add(uses.get(j));
				}
			}
			
			defs.remove(0);

			// for each use found.. creates one directed edge (def -> use)
			for (DependencyNode use : uses) {
				connectVertices(dependencyGraph, use, currentNode);
			}
			
		}

		return dependencyGraph;
	}

	private DependencyNode createNode(Collection<Unit> unitsInSelection,
			SelectionPosition selectionPosition, IConfigRep configRep, IFeatureRep featureRep, Unit u) {
		
		DependencyNode node;
		
		// Gets the exact position of a given unit
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
		
		// verifies if the current unit is in selection..
		if (unitsInSelection.contains(u)) {
			
			node = new DependencyNodeWrapper<Unit>(u, true,	pos, configRep, featureRep);

			if (isDef(node)) {
				defs.add(node);
//				System.out.println("Invalid selection..");
			}
		} else { // otherwise..
			node = new DependencyNodeWrapper<Unit>(u, false, pos, configRep, featureRep);
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
			final DependencyNode use,
			final DependencyNode def) {

		// To avoid same nodes (e.g. def == use)
		if(def.getPosition().getStartLine() == use.getPosition().getStartLine()) {
			return;
		}
		
		// takes into account the statements related to current feature
		if (DependencyGraphBuilder.featureDependence && def.getFeatureSet() == use.getFeatureSet()) {
			addVerticesAndEdge(graph, def, use);
		} else if (def.getFeatureSet() != use.getFeatureSet()) { //between other features
			addVerticesAndEdge(graph, def, use);
		}
	}

	private static void addVerticesAndEdge(
			Graph<DependencyNode, ValueContainerEdge<ConfigSet>> graph,
			DependencyNode def, DependencyNode use) {
		/*
		 * Counting on the graph's implementation to check for the existance of
		 * the nodes before adding to avoid duplicate vertices.
		 */
		graph.addVertex(def);
		graph.addVertex(use);
		
		if (!graph.containsEdge(def, use)) {
			ValueContainerEdge<ConfigSet> addedEdge = graph.addEdge(def, use);
			
			ConfigSet configS = def.getConfigSet().and(use.getConfigSet());
			addedEdge.setValue(configS);
		}
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
	
//	public DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> generateGraph(
//			UnitGraph cfg,
//			ForwardFlowAnalysis<Unit, ? extends FlowSet> analysis,
//			Collection<Unit> unitsInSelection,
//			SelectionPosition selectionPosition,
//			Set<IConfigRep> configReps) {
//		
//		// This graph will be return
//		DirectedMultigraph<DependencyNode, ValueContainerEdge<ConfigSet>> dependencyGraph = new DirectedMultigraph<DependencyNode, ValueContainerEdge<ConfigSet>>(
//				(Class<? extends ValueContainerEdge<ConfigSet>>) ValueContainerEdge.class);
//		
//		Map<Pair<Unit, IConfigRep>, Set<Unit>> unitConfigurationMap = new HashMap<Pair<Unit, IConfigRep>, Set<Unit>>();
//		FeatureTag bodyFeatureTag = (FeatureTag) cfg.getBody().getTag("FeatureTag");
//		
//		for (Unit unit : unitsInSelection) {
//			System.out.println("FeatureTag: "+unit.getTag("FeatureTag"));
//		}
//		
//		for (Unit unit : unitsInSelection) {
//			// exclude definitions when it's $temp on the leftOp.
//			DefinitionStmt definition = (DefinitionStmt) unit;
//            Local leftOp = (Local) definition.getLeftOp();
//            if (leftOp.getName().contains("$")) {
//                continue;
//            }
//            
//            Set<FeatureTag> featuresThatUseDefinition = new HashSet<FeatureTag>();
//            
//            // for every unit in the body
//            Iterator<Unit> iterator = cfg.getBody().getUnits().snapshotIterator();
//            while (iterator.hasNext()) {
//				Unit u = (Unit) iterator.next();
//				FeatureTag unitTag = (FeatureTag) u.getTag("FeatureTag");
//				
//				List useAndDefBoxes = u.getUseAndDefBoxes();
//				for (Object object : useAndDefBoxes) {
//					ValueBox vbox = (ValueBox) object;
//					
//					if(vbox.getValue().equivTo(leftOp)) {
//						featuresThatUseDefinition.add(unitTag);
//					}
//				}
//				
//				EagerMapLiftedFlowSet liftedFlowAfter = (EagerMapLiftedFlowSet) analysis.getFlowAfter(u);
//				List<FlowSet> lattices = new ArrayList<FlowSet>();
//				
//				Iterator<IConfigRep> it = configReps.iterator();
//				while (it.hasNext()) {
//					IConfigRep configRep = (IConfigRep) it.next();
//					FlowSet lattice = liftedFlowAfter.getLattice(configRep);
//					
//					lattices.add(lattice);
//				}
//				
//				// and for every configuration..
//				for (int i = 0; i < lattices.size(); i++) {
//					FlowSet flowSet = lattices.get(i);
//					IFeatureRep featureRep = bodyFeatureTag.getFeatureRep();
//					
//					// if the unit belongs to the configuration..
//					Iterator<IConfigRep> it2 = configReps.iterator();
//					while (it2.hasNext()) {
//						IConfigRep configRep = (IConfigRep) it2.next();
//						if (unitTag.getFeatureRep().belongsToConfiguration(configRep)) { //or featureRep
//							
//							// if the definition reaches this unit..
//							if (flowSet.contains(definition)) {
//								List<ValueBox> useBoxes = u.getUseBoxes();
//								for (ValueBox valueBox : useBoxes) {
//									/**
//									 * and the definition is used, then
//									 * add to the map (graph)..
//									 */
//									if (valueBox.getValue().equivTo(leftOp)) {
//										Pair<Unit, IConfigRep> currentPair = new Pair<Unit, IConfigRep>(definition, configRep);
//										Set<Unit> unitConfigReachesSet = unitConfigurationMap.get(currentPair);
//										
//										DependencyNode defNode = createNode(unitsInSelection, selectionPosition, configRep, definition);
//										DependencyNode useNode = createNode(unitsInSelection, selectionPosition, configRep, u);
//										
//										if (!dependencyGraph.containsVertex(defNode)) {
//											dependencyGraph.addVertex(defNode);
//										}
//										if (!dependencyGraph.containsVertex(useNode)) {
//											dependencyGraph.addVertex(useNode);
//										}
//										
//										Set<ValueContainerEdge<ConfigSet>> allEdges = dependencyGraph.getAllEdges(defNode, useNode);
//										if (allEdges.size() >= 1) {
//											int diffCounter = 0;
//											Iterator<ValueContainerEdge<ConfigSet>> edgeIte = allEdges.iterator();
//											Set<ValueContainerEdge<ConfigSet>> edgeRemovalSchedule = new HashSet<ValueContainerEdge<ConfigSet>>();
//											
//											while (edgeIte.hasNext()) {
//												ValueContainerEdge<ConfigSet> valueContainerEdge = (ValueContainerEdge<ConfigSet>) edgeIte.next();
//												ConfigSet valueConfig = valueContainerEdge.getValue();
//												Integer idForConfig = 0;
//												FlowSet flowSetFromOtherReached = lattices.get(idForConfig);
//												if (flowSetFromOtherReached.equals(flowSet)) {
////													if (valueConfig.length > featureRep.size() && featuresThatUseDefinition.contains(featureRep)) {
////														edgeRemovalSchedule.add(valueContainerEdge);
////														ValueContainerEdge<ConfigSet> addEdge = dependencyGraph.addEdge(defNode, useNode);
////														addEdge.setValue(featureRep);
////													    continue;
////													}
//												} else {
//													diffCounter++;
//												}
//											}
//											
//											if (diffCounter == allEdges.size() && featuresThatUseDefinition.contains(featureRep)) {
//												ValueContainerEdge<ConfigSet> addEdge = dependencyGraph.addEdge(defNode, useNode);
//												ConfigSetImpl configSetImpl = new ConfigSetImpl();
//												addEdge.setValue(configSetImpl);
//											}
//											dependencyGraph.removeAllEdges(edgeRemovalSchedule);
//										} else {
//											ValueContainerEdge<ConfigSet> addEdge = dependencyGraph.addEdge(defNode, useNode);
//											ConfigSetImpl configSetImpl = new ConfigSetImpl();
//											addEdge.setValue(configSetImpl);
//										}
//										
//										if (unitConfigReachesSet == null) {
//											unitConfigReachesSet = new HashSet<Unit>();
//											unitConfigReachesSet.add(u);
//											unitConfigurationMap.put(currentPair, unitConfigReachesSet);
//										} else {
//											unitConfigReachesSet.add(u);
//										}
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//            
//		}
//		
//		return dependencyGraph;
//	}

}
