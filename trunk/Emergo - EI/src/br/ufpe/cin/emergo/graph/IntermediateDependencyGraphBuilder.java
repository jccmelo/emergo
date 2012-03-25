package br.ufpe.cin.emergo.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DirectedMultigraph;

import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.core.DefUseRules;
import br.ufpe.cin.emergo.core.JWCompilerConfigSet;
import br.ufpe.cin.emergo.core.SelectionPosition;
import br.ufpe.cin.emergo.util.DebugUtil;
import dk.au.cs.java.compiler.analysis.AnswerAdapter;
import dk.au.cs.java.compiler.cfg.ControlFlowGraph;
import dk.au.cs.java.compiler.cfg.Worklist;
import dk.au.cs.java.compiler.cfg.analysis.InterproceduralAnalysis;
import dk.au.cs.java.compiler.cfg.analysis.PointVisitor;
import dk.au.cs.java.compiler.cfg.edge.Edge;
import dk.au.cs.java.compiler.cfg.point.Expression;
import dk.au.cs.java.compiler.cfg.point.Point;
import dk.au.cs.java.compiler.cfg.point.Read;
import dk.au.cs.java.compiler.cfg.point.Variable;
import dk.au.cs.java.compiler.cfg.point.Write;
import dk.au.cs.java.compiler.ifdef.IfDefVarSet;
import dk.au.cs.java.compiler.ifdef.SharedSimultaneousAnalysis;
import dk.au.cs.java.compiler.node.AAndIfdefExp;
import dk.au.cs.java.compiler.node.ACompilationUnit;
import dk.au.cs.java.compiler.node.AIdIfdefExp;
import dk.au.cs.java.compiler.node.AIfdefStm;
import dk.au.cs.java.compiler.node.ANotIfdefExp;
import dk.au.cs.java.compiler.node.AOrIfdefExp;
import dk.au.cs.java.compiler.node.AProgram;
import dk.au.cs.java.compiler.node.Node;
import dk.au.cs.java.compiler.node.NodeFilter;
import dk.au.cs.java.compiler.node.Token;
import dk.brics.lattice.LatticeSet;
import dk.brics.lattice.LatticeSetFilter;

public class IntermediateDependencyGraphBuilder {

	/**
	 * Defeats instantiation.
	 * 
	 */
	private IntermediateDependencyGraphBuilder() {

	}

	/**
	 * Finds the dependencies in the {@code cfg} wrt the user selection ({@code pointsInUserSelection}). These
	 * dependencies are represented as an (possibly cyclic) directed graph.
	 * 
	 * @param node
	 * @param cfg
	 * @param pointsInUserSelection
	 * @param methodDecl
	 * @return
	 */
	public static DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> buildIntraproceduralGraph(AProgram node, ControlFlowGraph cfg, final Collection<Point> pointsInUserSelection, Node methodDecl) {
		DebugUtil.writeStringToFile(cfg.toDot(), "cfg-intra.dot");
		
		/*
		 * Instantiates an analysis with the Def-Use rules.
		 */
		SharedSimultaneousAnalysis<LatticeSet<Point>> sharedAnalysis = new SharedSimultaneousAnalysis<LatticeSet<Point>>(new DefUseRules());
		
		Worklist.process(cfg, sharedAnalysis);
		
		DebugUtil.writeStringToFile(cfg.toDot(sharedAnalysis), "cfg-defuse-intra.dot");
		
		/*
		 * Create a new feature-sensitive dependency graph based on the results of the analysis. The vertices are Reads
		 * or Writes instances.
		 */
		DirectedGraph<Point, ValueContainerEdge<ConfigSet>> dependencyGraph = createGraph(cfg, sharedAnalysis);
		DebugUtil.exportToDotFile(dependencyGraph, "dep-intra.dot");

		/*
		 * The selection boundaries are used here to filter the graph. All nodes that does not contain a path from one
		 * of the nodes in the user selection to it is not present in the new graph generated. The old graph is left
		 * untouched as the new one is created.
		 * 
		 * In this graph, the vertices are DependencyNode instances used as wrappers for Reads and Writes.
		 */
		DirectedGraph<DependencyNodeWrapper<Point>, ValueContainerEdge<ConfigSet>> filteredDependencyGraph = filterWithUserSelection(pointsInUserSelection, dependencyGraph);
		DebugUtil.exportToDotFile(filteredDependencyGraph, "fil-intra.dot");

		// Produce a more compact graph by collapsing nodes that belongs to the same line number.
		DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> collapsedDependencyGraph = collapseIntoLineNumbers(filteredDependencyGraph);
		DebugUtil.exportToDotFile(collapsedDependencyGraph, "col-intra.dot");

		return collapsedDependencyGraph;
	}

	public static DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> buildInterproceduralGraph(AProgram node, ControlFlowGraph cfg, int depth, int inline, final SelectionPosition selectionPosition) {
		DebugUtil.writeStringToFile(cfg.toDot(), "cfg-pre-inter.dot");
		/*
		 * XXX it should be much faster to look for the nodes that are in the selection BEFORE creating the
		 * interprocedural graph. However, when checking for equality on elements that are in the interprocedural graph
		 * with the ones that are in the intraprocedural one ALWAYS fails. This is most likely due to the fact that the
		 * classes that comprise the graphs do not implement the equals/hashCode contract.
		 */
		cfg = InterproceduralAnalysis.createInterproceduralControlFlowGraph(cfg,depth,inline);
		DebugUtil.writeStringToFile(cfg.toDot(), "cfg-inter.dot");
		final Collection<Point> pointsInUserSelection = new HashSet<Point>();
		cfg.apply(new PointVisitor<Object, Object>() {
			@Override
			protected Object defaultPoint(Point point, Object question) {
				Token token = point.getToken();
				int line = token.getLine();
				if (line >= selectionPosition.getStartLine() + 1 && line <= selectionPosition.getEndLine() + 1) {
					pointsInUserSelection.add(point);
				}
				return null;
			}
		});

		SharedSimultaneousAnalysis<LatticeSet<Point>> sharedAnalysis = new SharedSimultaneousAnalysis<LatticeSet<Point>>(new DefUseRules());
		DebugUtil.writeStringToFile(cfg.toDot(sharedAnalysis), "cfg-defuse-inter.dot");
			
		Worklist.process(cfg, sharedAnalysis);

		DirectedGraph<Point, ValueContainerEdge<ConfigSet>> dependencyGraph = createGraph(cfg, sharedAnalysis);
		DebugUtil.exportToDotFile(dependencyGraph, "dep-inter.dot");

		DirectedGraph<DependencyNodeWrapper<Point>, ValueContainerEdge<ConfigSet>> filteredDependencyGraph = filterWithUserSelection(pointsInUserSelection, dependencyGraph);
		DebugUtil.exportToDotFile(filteredDependencyGraph, "fil-inter.dot");

		DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> collapsedDependencyGraph = collapseIntoLineNumbers(filteredDependencyGraph);
		DebugUtil.exportToDotFile(collapsedDependencyGraph, "col-inter.dot");

		return collapsedDependencyGraph;
	}

	private static DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> collapseIntoLineNumbers(DirectedGraph<DependencyNodeWrapper<Point>, ValueContainerEdge<ConfigSet>> dependencyGraph) {
		assert dependencyGraph != null;

		DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> collapsedGraph = new DefaultDirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>>((Class<? extends ValueContainerEdge<ConfigSet>>) ValueContainerEdge.class);
		
		/*
		 * This graph can not be collapsed if there are no connections between the nodes or enough nodes to enable edges
		 * to exist.
		 */
		Set<DependencyNodeWrapper<Point>> vertexSet = dependencyGraph.vertexSet();
		Set<ValueContainerEdge<ConfigSet>> edgeSet = dependencyGraph.edgeSet();
		if (edgeSet.size() == 0 || vertexSet.size() <= 1) {
			return collapsedGraph;
		}
		
		class LineAndFile {
			public final int line;
			public final String file;
			
			public LineAndFile(Token token) {
				this.line = token.getLine();
				this.file = token.getAncestor(ACompilationUnit.class).getFile().getAbsolutePath();
			}
			
			@Override
			public int hashCode() {
				return new HashCodeBuilder().append(line).append(file).build();
			}
			
			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof LineAndFile)) {  
			        return false;  
			    }  
			    if (this == obj) {  
			    	return true;  
			    }  
			    final LineAndFile otherObject = (LineAndFile) obj;  
			  
			    return new EqualsBuilder().append(this.line, otherObject.line).append(this.file, otherObject.file).isEquals();
			}
		}

		Map<LineAndFile, Set<DependencyNodeWrapper<Point>>> lineNodesSetMapping = new HashMap<LineAndFile, Set<DependencyNodeWrapper<Point>>>();
		Map<LineAndFile, DependencyNodeWrapper<Set<DependencyNodeWrapper<Point>>>> lineNodeMapping = new HashMap<LineAndFile, DependencyNodeWrapper<Set<DependencyNodeWrapper<Point>>>>();
		Set<LineAndFile> keysInSelection = new HashSet<LineAndFile>();

		/*
		 * In this first step the vertices are grouped by their line number in a map. Each entry maps an integer into a
		 * set of vertices.
		 */
		for (DependencyNodeWrapper<Point> dependencyNode : vertexSet) {
			Token pointToken = ((Point) dependencyNode.getData()).getToken();
			LineAndFile lineAndFile = new LineAndFile(pointToken);
			Set<DependencyNodeWrapper<Point>> set = lineNodesSetMapping.get(lineAndFile);
			if (set == null) {
				set = new HashSet<DependencyNodeWrapper<Point>>();
				lineNodesSetMapping.put(lineAndFile, set);
			}
			set.add(dependencyNode);

			// Store nodes that belong to the selection for later use.
			if (dependencyNode.isInSelection()) {
				keysInSelection.add(lineAndFile);
			}
		}

		/*
		 * All nodes that are mapped by the same integer are packed into a set and embedded into a
		 * DependencyNodeWrapper. Each of these DependencyNodeWrappers are then added to the graph that will be
		 * returned.
		 */
		Set<Entry<LineAndFile, Set<DependencyNodeWrapper<Point>>>> entrySet = lineNodesSetMapping.entrySet();
		for (Entry<LineAndFile, Set<DependencyNodeWrapper<Point>>> entry : entrySet) {
			LineAndFile lineAndFile = entry.getKey();
			Set<DependencyNodeWrapper<Point>> nodes = entry.getValue();

			IfDefVarSet accumulator = null;
			for (DependencyNodeWrapper<Point> dependencyNode : nodes) {
				if (accumulator == null) {
					accumulator = dependencyNode.getData().getVarSet();
				} else {
					accumulator = accumulator.and(dependencyNode.getData().getVarSet());
				}
			}
			
			DependencyNodeWrapper<Set<DependencyNodeWrapper<Point>>> dependencyNode = new DependencyNodeWrapper<Set<DependencyNodeWrapper<Point>>>(nodes, SelectionPosition.builder().startLine(lineAndFile.line).filePath(lineAndFile.file).build(), keysInSelection.contains(lineAndFile), accumulator);
			dependencyNode.setFeatureSet(makeFeatureSet(nodes));
			collapsedGraph.addVertex(dependencyNode);
			lineNodeMapping.put(lineAndFile, dependencyNode);
		}

		/*
		 * At this point, all nodes in the graph actually represents a set of nodes (see steps 1 and 2 above). Thus, it
		 * is only necessary to iterate over these embedded nodes and add the edges that connected them before.
		 */
		for (Entry<LineAndFile, Set<DependencyNodeWrapper<Point>>> entry : entrySet) {
			LineAndFile lineAndFile = entry.getKey();
			Set<DependencyNodeWrapper<Point>> nodes = entry.getValue();
			for (DependencyNodeWrapper<Point> dependencyNode : nodes) {
				Set<ValueContainerEdge<ConfigSet>> outgoingEdgesOf = dependencyGraph.outgoingEdgesOf(dependencyNode);
				for (ValueContainerEdge<ConfigSet> valueContainerEdge : outgoingEdgesOf) {
					DependencyNodeWrapper<Point> edgeTarget = dependencyGraph.getEdgeTarget(valueContainerEdge);
					Token targeToken = (Token) edgeTarget.getData().getToken();
					DependencyNodeWrapper<Set<DependencyNodeWrapper<Point>>> srcNodeWrapper = lineNodeMapping.get(lineAndFile);
					DependencyNodeWrapper<Set<DependencyNodeWrapper<Point>>> tgtNodeWrapper = lineNodeMapping.get(new LineAndFile(targeToken));

					ValueContainerEdge<ConfigSet> addedEdge = collapsedGraph.addEdge(srcNodeWrapper, tgtNodeWrapper);

					/*
					 * If addedEdge is null, then there is already and edge connecting these nodes. The information
					 * contained in these nodes must be merged.
					 */
					if (addedEdge == null) {
						ValueContainerEdge<ConfigSet> existingEdge = collapsedGraph.getEdge(srcNodeWrapper, tgtNodeWrapper);
						existingEdge.setValue(existingEdge.getValue().and(valueContainerEdge.getValue()));
					} else {
						addedEdge.setValue(valueContainerEdge.getValue());
					}
				}
			}
		}

		return collapsedGraph;
	}

	/**
	 * Creates a new Graph based on {@code reachesData} by creating a new graph in which all points that are not reached
	 * by a path from one of the points in the user selection are not present. The nodes in this Graph are
	 * DependencyNodes instances.
	 * 
	 * @param pointsInUserSelection
	 * @param dependencyGraph
	 * @return a new filtered graph
	 */
	private static DirectedGraph<DependencyNodeWrapper<Point>, ValueContainerEdge<ConfigSet>> filterWithUserSelection(Collection<Point> pointsInUserSelection, DirectedGraph<Point, ValueContainerEdge<ConfigSet>> dependencyGraph) {
		// The new graph that will be returned from this method.
		final DirectedMultigraph<DependencyNodeWrapper<Point>, ValueContainerEdge<ConfigSet>> filteredGraph = new DirectedMultigraph<DependencyNodeWrapper<Point>, ValueContainerEdge<ConfigSet>>((Class<? extends ValueContainerEdge<ConfigSet>>) ValueContainerEdge.class);

		/*
		 * A worklist-like iteration idiom. Visit a point; add the point to visited; add others points (if present) in
		 * the to-be-visited list; repeat untill list is empty.
		 * 
		 * The filtering result is achieved simply by setting the points in user selection in the starting list.
		 */
		LinkedList<Point> workList = new LinkedList<Point>(pointsInUserSelection);
		HashSet<Point> alreadyVisitedPoints = new HashSet<Point>();

		while (!workList.isEmpty()) {
			Point head = workList.removeFirst();
			if (!dependencyGraph.containsVertex(head)) {
				alreadyVisitedPoints.add(head);
				continue;
			}
			Set<ValueContainerEdge<ConfigSet>> outgoingEdges = dependencyGraph.outgoingEdgesOf(head);
			if (outgoingEdges.isEmpty()) {
				alreadyVisitedPoints.add(head);
				continue;
			}
			alreadyVisitedPoints.add(head);
			for (ValueContainerEdge<ConfigSet> edge : outgoingEdges) {
				Point target = (Point) dependencyGraph.getEdgeTarget(edge);
				DependencyNodeWrapper<Point> dependencyNodeTarget = new DependencyNodeWrapper<Point>(target, makePosition(target), pointsInUserSelection.contains(target), target.getVarSet());
				filteredGraph.addVertex(dependencyNodeTarget);

				DependencyNodeWrapper<Point> dependencyNodeHead = new DependencyNodeWrapper<Point>(head, makePosition(head), pointsInUserSelection.contains(head), head.getVarSet());
				filteredGraph.addVertex(dependencyNodeHead);

				ValueContainerEdge<ConfigSet> addedEdge = filteredGraph.addEdge(dependencyNodeHead, dependencyNodeTarget);
				if (addedEdge != null) {
					addedEdge.setValue(edge.getValue());
				}

				if (alreadyVisitedPoints.add(target)) {
					workList.add(target);
				}
			}
		}
		return filteredGraph;
	}

	private static DirectedMultigraph<Point, ValueContainerEdge<ConfigSet>> createGraph(ControlFlowGraph controlFlowGraph, SharedSimultaneousAnalysis<LatticeSet<Point>> analysisResult) {
		// The dependency graph that this method will return.
		final DirectedMultigraph<Point, ValueContainerEdge<ConfigSet>> reachesData = new DirectedMultigraph<Point, ValueContainerEdge<ConfigSet>>((Class<? extends ValueContainerEdge<ConfigSet>>) ValueContainerEdge.class);

		// List of points to be visited. Starts with the CFG entry point.
		LinkedList<Point> pendingPoints = new LinkedList<Point>();
		HashSet<Point> visitedPoints = new HashSet<Point>();
		pendingPoints.add(controlFlowGraph.getEntryPoint());

		// Worklist-like iteration idiom
		while (!pendingPoints.isEmpty()) {
			// Consumes a point
			final Point poppedPoint = pendingPoints.removeFirst();
			visitedPoints.add(poppedPoint);

			Set<? extends Edge> outgoingEdges = poppedPoint.getOutgoingEdges();
			for (Edge edge : outgoingEdges) {
				// ...adds targets to iterate later if they haven't been yet.
				if (visitedPoints.add(edge.getTarget())) {
					pendingPoints.add(edge.getTarget());
				}
			}

			// TODO: there is plenty of duplicated code here. Refactor for reuse.
			// treat Reads and writes accordingly
			if (poppedPoint instanceof Read) {
				final Read read = (Read) poppedPoint;
				final Expression expression = read.getExpression();
				Set<? extends Edge> ingoingEdges = read.getIngoingEdges();
				for (Edge edge : ingoingEdges) {
					Map<IfDefVarSet, LatticeSet<Point>> variable = analysisResult.getVariable(edge);

					if (variable == null)
						continue;

					Set<Entry<IfDefVarSet, LatticeSet<Point>>> entrySet = variable.entrySet();
					for (Entry<IfDefVarSet, LatticeSet<Point>> entry : entrySet) {
						LatticeSet<Point> value = entry.getValue();
						final IfDefVarSet key = entry.getKey();

						if (key.and(poppedPoint.getVarSet()).isEmpty())
							continue;

						/*
						 * Iterate over the lattice. In this case, the statement at issue is a Read. This means that the
						 * elements from the lattice we are interested are Writes and other Reads.
						 */
						value.filter(new LatticeSetFilter<Object>() {

							public boolean accept(Object obj) {
								if (obj instanceof Read) {
									Read element = (Read) obj;
									if (expression.toString().contains(element.getVariable().toString())) {
										connectVertices(reachesData, poppedPoint, key, element);
									}
									return true;

								} else if (obj instanceof Write) {
									Write element = (Write) obj;
									if (element.getLValue().equals(expression)) {
										connectVertices(reachesData, poppedPoint, key, element);
									}
								}
								return true;
							}
						});
					}
				}
			} else if (poppedPoint instanceof Write) {
				final Write write = (Write) poppedPoint;
				final Variable rValue = write.getVariable();
				Set<? extends Edge> ingoingEdges = write.getIngoingEdges();
				for (Edge edge : ingoingEdges) {
					Map<IfDefVarSet, LatticeSet<Point>> variable = analysisResult.getVariable(edge);

					if (variable == null)
						continue;

					Set<Entry<IfDefVarSet, LatticeSet<Point>>> entrySet = variable.entrySet();
					for (Entry<IfDefVarSet, LatticeSet<Point>> entry : entrySet) {
						LatticeSet<Point> value = entry.getValue();
						final IfDefVarSet key = entry.getKey();
						value.filter(new LatticeSetFilter<Point>() {

							public boolean accept(Point obj) {
								if (obj instanceof Read) {
									Read element = (Read) obj;
									if (rValue.equals(element.getVariable())) {
										connectVertices(reachesData, poppedPoint, key, element);
									}
								}
								return true;
							}
						});
					}
				}
			} else {
				final String strPoint = poppedPoint.toString();
				Set<? extends Edge> ingoingEdges = poppedPoint.getIngoingEdges();
				for (Edge edge : ingoingEdges) {
					Map<IfDefVarSet, LatticeSet<Point>> variable = analysisResult.getVariable(edge);

					if (variable == null)
						continue;

					Set<Entry<IfDefVarSet, LatticeSet<Point>>> entrySet = variable.entrySet();
					for (Entry<IfDefVarSet, LatticeSet<Point>> entry : entrySet) {
						LatticeSet<Point> value = entry.getValue();
						final IfDefVarSet key = entry.getKey();
						value.filter(new LatticeSetFilter<Point>() {

							public boolean accept(Point obj) {
								if (obj instanceof Read) {
									Read element = (Read) obj;
									if (strPoint.contains(element.getVariable().toString())) {
										connectVertices(reachesData, poppedPoint, key, element);
									}
								}
								return true;
							}
						});
					}
				}
			}
		}
		return reachesData;
	}

	/**
	 * Creates an edge between {@code source} and {@code target} objects in {@code graph} for the configuration set
	 * {@code key} to represent a dependency between these vertices. If an edge already exists between these vertices,
	 * then instead of creating a new edge, the old one is replaced by a new one containing both the old configuration
	 * sets and the new configuration set.
	 * 
	 * @param graph
	 * @param use
	 * @param configurationMean
	 * @param def
	 */
	private static void connectVertices(final Graph<Point, ValueContainerEdge<ConfigSet>> graph, final Point use, final IfDefVarSet configurationMean, Point def) {

		/*
		 * Counting on the graph's implementation to check for the existance of the nodes before adding to avoid
		 * duplicate vertices.
		 */
		graph.addVertex(use);
		graph.addVertex(def);

		/*
		 * To avoid having more than one edge between two given nodes, the information contained in these edges,
		 * internally an IfDefVarSet instance, is merged by using the OR operator.
		 */
		if (graph.containsEdge(def, use)) {
			ValueContainerEdge<ConfigSet> existingEdge = graph.getEdge(def, use);
			ConfigSet existingIfDefVarSet = (ConfigSet) existingEdge.getValue();
			ConfigSet or = existingIfDefVarSet.or(new JWCompilerConfigSet(configurationMean));

			// TODO: is checking against the feature model necessary? It won't hurt to leave this here though.
			if (((JWCompilerConfigSet) or).getVarSet().isValidInFeatureModel())
				existingEdge.setValue(or);

		} else {
			JWCompilerConfigSet sourceConfigAndMean = new JWCompilerConfigSet(configurationMean.and(use.getVarSet()).and(def.getVarSet()));
			if (sourceConfigAndMean.isEmpty() || !sourceConfigAndMean.getVarSet().isValidInFeatureModel()) {
				return;
			}
			if (configurationMean.isValidInFeatureModel()) {
				ValueContainerEdge<ConfigSet> addedEdge = graph.addEdge(def, use);
				addedEdge.setValue(sourceConfigAndMean);
			}
		}
	}

	private static SelectionPosition makePosition(Point p) {
		ACompilationUnit ancestorCompilationUnit = p.getToken().getAncestor(ACompilationUnit.class);
		return SelectionPosition.builder().startColumn(p.getToken().getPos()).startLine(p.getToken().getLine()).filePath(ancestorCompilationUnit.getFile().getAbsolutePath()).build();
	}
	
	private static IfDefVarSet makeFeatureSet(Set<DependencyNodeWrapper<Point>> points){
		for (DependencyNodeWrapper<Point> node : points) {
			final Point point = node.getData();
			AIfdefStm ifdefStm = (AIfdefStm) point.getToken().getAncestor(new NodeFilter<Node>() {
				
				@Override
				public boolean accept(Node node) {
					if (node instanceof AIfdefStm){
						return true; 
					}
					return false;
				}

				@Override
				public boolean guard(Node node) {
					return false;
				}

			});
			if (ifdefStm != null) {
				return ifdefStm.getExp().apply(new AnswerAdapter<IfDefVarSet>() {
					@Override
					public IfDefVarSet caseAIdIfdefExp(AIdIfdefExp node) {
						return IfDefVarSet.getIfDefVarSet(node.getIdentifier().getText());
					}
			
					@Override
					public IfDefVarSet caseANotIfdefExp(ANotIfdefExp node) {
						return node.getExp().apply(this).not();
					}
			
					@Override
					public IfDefVarSet caseAAndIfdefExp(AAndIfdefExp node) {
						IfDefVarSet left = node.getLeft().apply(this);
						IfDefVarSet right = node.getRight().apply(this);
						return left.and(right);
					}
			
					@Override
					public IfDefVarSet caseAOrIfdefExp(AOrIfdefExp node) {
						IfDefVarSet left = node.getLeft().apply(this);
						IfDefVarSet right = node.getRight().apply(this);
						return left.or(right);
					}
				});
			}
		}
		return IfDefVarSet.getAll();
	}
}
