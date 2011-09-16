package br.ufpe.cin.emergo.graph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.StringNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DirectedMultigraph;

import br.ufpe.cin.emergo.core.DefUseRules;
import dk.au.cs.java.compiler.cfg.ControlFlowGraph;
import dk.au.cs.java.compiler.cfg.DirectAnalysis;
import dk.au.cs.java.compiler.cfg.analysis.AnalysisProcessor;
import dk.au.cs.java.compiler.cfg.edge.Edge;
import dk.au.cs.java.compiler.cfg.point.AbstractPoint;
import dk.au.cs.java.compiler.cfg.point.Expression;
import dk.au.cs.java.compiler.cfg.point.Point;
import dk.au.cs.java.compiler.cfg.point.Read;
import dk.au.cs.java.compiler.cfg.point.Variable;
import dk.au.cs.java.compiler.cfg.point.Write;
import dk.au.cs.java.compiler.ifdef.IfDefVarSet;
import dk.au.cs.java.compiler.ifdef.SharedSimultaneousAnalysis;
import dk.au.cs.java.compiler.node.AMethodDecl;
import dk.au.cs.java.compiler.node.AProgram;
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
	public static Graph<Object, ValueContainerEdge> build(AProgram node, ControlFlowGraph cfg, final List<Point> pointsInUserSelection, AMethodDecl methodDecl) {
		// This graph will contain a representation for the data dependencies.
		final DefaultDirectedGraph<Object, ValueContainerEdge> reachesData = new DefaultDirectedGraph<Object, ValueContainerEdge>(ValueContainerEdge.class);
		// DirectAnalysis<LatticeSet<Object>> analysisResult = AnalysisProcessor.process(methodDecl, new
		// DefUseRules(reachesData));
		// DefaultDirectedGraph<Object, ValueContainerEdge> filteredReachesData =

		// XXX Prints out stuff to help debugging. Remove later.
		// _debug(cfg, reachesData, filteredReachesData, analysisResult);

		SharedSimultaneousAnalysis<LatticeSet<Object>> sharedAnalysis = AnalysisProcessor.processShared(methodDecl, new DefUseRules(reachesData));
		DirectedMultigraph<Object, ValueContainerEdge> build3 = build3(cfg, sharedAnalysis);
		DirectedMultigraph<Object, ValueContainerEdge> filterWithUserSelection = filterWithUserSelection(pointsInUserSelection, build3);

		String dot = cfg.toDot(sharedAnalysis);
		System.out.println(dot);

		_debug2(filterWithUserSelection, cfg, sharedAnalysis);

		return reachesData;
	}

	private static DirectedMultigraph<Object, ValueContainerEdge> filterWithUserSelection(List<Point> pointsInUserSelection, DirectedMultigraph<Object, ValueContainerEdge> reachesData) {
		final DirectedMultigraph<Object, ValueContainerEdge> filteredGraph = new DirectedMultigraph<Object, ValueContainerEdge>(ValueContainerEdge.class);

		LinkedList<Point> workList = new LinkedList<Point>(pointsInUserSelection);
		HashSet<Point> alreadyVisitedPoints = new HashSet<Point>();

		while (!workList.isEmpty()) {
			Point head = workList.removeFirst();
			if (!reachesData.containsVertex(head)) {
				alreadyVisitedPoints.add(head);
				continue;
			}
			Set<ValueContainerEdge> outgoingEdges = reachesData.outgoingEdgesOf(head);
			if (outgoingEdges.isEmpty()) {
				alreadyVisitedPoints.add(head);
				continue;
			}
			alreadyVisitedPoints.add(head);
			for (ValueContainerEdge edge : outgoingEdges) {
				Point target = (Point) reachesData.getEdgeTarget(edge);
				filteredGraph.addVertex(head);
				filteredGraph.addVertex(target);
				ValueContainerEdge addedEdge = filteredGraph.addEdge(head, target);
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

	public static DirectedMultigraph<Object, ValueContainerEdge> build3(ControlFlowGraph controlFlowGraph, SharedSimultaneousAnalysis<LatticeSet<Object>> analysisResult) {
		final DirectedMultigraph<Object, ValueContainerEdge> reachesData = new DirectedMultigraph<Object, ValueContainerEdge>(ValueContainerEdge.class);
		// List of points to be visited. Starts with the CFG entry point.
		LinkedList<Point> pendingPoints = new LinkedList<Point>();
		HashSet<Point> visitedPoints = new HashSet<Point>();
		pendingPoints.add(controlFlowGraph.getEntryPoint());

		// Worklist-like iteration idiom
		while (!pendingPoints.isEmpty()) {
			// Consumes 1st point
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
					Map<IfDefVarSet, LatticeSet<Object>> variable = analysisResult.getVariable(edge);
					Set<Entry<IfDefVarSet, LatticeSet<Object>>> entrySet = variable.entrySet();
					for (Entry<IfDefVarSet, LatticeSet<Object>> entry : entrySet) {
						LatticeSet<Object> value = entry.getValue();
						final IfDefVarSet key = entry.getKey();

						/*
						 * Iterate over the lattice. In this case, the statement at issue is a Read. This means that the
						 * elements from the lattice we are interested are Writes and other Reads.
						 */
						value.filter(new LatticeSetFilter<Object>() {

							public boolean accept(Object obj) {
								if (obj instanceof Read) {
									Read element = (Read) obj;
									if (expression.toString().contains(element.getVariable().toString())) {
										reachesData.addVertex(read);
										reachesData.addVertex(element);

										/*
										 * To avoid having more than one edge between two given nodes, the information
										 * contained in these edges, that is, an IfDefVarSet instance, is merged by
										 * using the OR operator.
										 */
										if (reachesData.containsEdge(poppedPoint, element)) {
											ValueContainerEdge existingEdge = reachesData.getEdge(poppedPoint, element);
											IfDefVarSet existingIfDefVarSet = (IfDefVarSet) existingEdge.getValue();
											IfDefVarSet or = existingIfDefVarSet.or(key);
											existingEdge.setValue(or);
										} else {
											ValueContainerEdge addEdge = reachesData.addEdge(element, poppedPoint);
											addEdge.setValue(key);
										}
									}
									return true;

								} else if (obj instanceof Write) {
									Write element = (Write) obj;
									if (element.getLValue().equals(expression)) {
										reachesData.addVertex(element);
										reachesData.addVertex(read);
										if (reachesData.containsEdge(poppedPoint, element)) {
											ValueContainerEdge existingEdge = reachesData.getEdge(poppedPoint, element);
											IfDefVarSet existingIfDefVarSet = (IfDefVarSet) existingEdge.getValue();
											IfDefVarSet or = existingIfDefVarSet.or(key);
											existingEdge.setValue(or);
										} else {
											ValueContainerEdge addEdge = reachesData.addEdge(element, poppedPoint);
											addEdge.setValue(key);
										}
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
					Map<IfDefVarSet, LatticeSet<Object>> variable = analysisResult.getVariable(edge);
					Set<Entry<IfDefVarSet, LatticeSet<Object>>> entrySet = variable.entrySet();
					for (Entry<IfDefVarSet, LatticeSet<Object>> entry : entrySet) {
						LatticeSet<Object> value = entry.getValue();
						final IfDefVarSet key = entry.getKey();
						value.filter(new LatticeSetFilter<Object>() {

							public boolean accept(Object obj) {
								if (obj instanceof Read) {
									Read element = (Read) obj;
									if (rValue.equals(element.getVariable())) {
										reachesData.addVertex(write);
										reachesData.addVertex(element);
										if (reachesData.containsEdge(poppedPoint, element)) {
											ValueContainerEdge existingEdge = reachesData.getEdge(poppedPoint, element);
											IfDefVarSet existingIfDefVarSet = (IfDefVarSet) existingEdge.getValue();
											IfDefVarSet or = existingIfDefVarSet.or(key);
											existingEdge.setValue(or);
										} else {
											ValueContainerEdge addEdge = reachesData.addEdge(element, poppedPoint);
											addEdge.setValue(key);
										}
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
					Map<IfDefVarSet, LatticeSet<Object>> variable = analysisResult.getVariable(edge);
					Set<Entry<IfDefVarSet, LatticeSet<Object>>> entrySet = variable.entrySet();
					for (Entry<IfDefVarSet, LatticeSet<Object>> entry : entrySet) {
						LatticeSet<Object> value = entry.getValue();
						final IfDefVarSet key = entry.getKey();
						value.filter(new LatticeSetFilter<Object>() {

							public boolean accept(Object obj) {
								if (obj instanceof Read) {
									Read element = (Read) obj;
									if (strPoint.contains(element.getVariable().toString())) {
										reachesData.addVertex(poppedPoint);
										reachesData.addVertex(element);
										if (reachesData.containsEdge(poppedPoint, element)) {
											ValueContainerEdge existingEdge = reachesData.getEdge(poppedPoint, element);
											IfDefVarSet existingIfDefVarSet = (IfDefVarSet) existingEdge.getValue();
											IfDefVarSet or = existingIfDefVarSet.or(key);
											existingEdge.setValue(or);
										} else {
											ValueContainerEdge addEdge = reachesData.addEdge(element, poppedPoint);
											addEdge.setValue(key);
										}
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

	private static void _debug2(Graph<Object, ValueContainerEdge> graph, ControlFlowGraph cfg, SharedSimultaneousAnalysis<LatticeSet<Object>> sharedAnalysis) {
		// XXX DEBUG code. Move it somewhere else.
		DOTExporter<Object, ValueContainerEdge> exporter = new DOTExporter<Object, ValueContainerEdge>(new StringNameProvider<Object>() {
			@Override
			public String getVertexName(Object vertex) {
				return "\"[" + ((AbstractPoint) vertex).getToken().getLine() + "]" + vertex.toString().replace("\"", "'") + "\"";
			}
		}, null, new EdgeNameProvider<ValueContainerEdge>() {

			public String getEdgeName(ValueContainerEdge edge) {
				Object value = edge.getValue();
				if (value == null)
					return "";
				return value.toString();
			}
		});

		try {
			File file = new File(System.getProperty("user.home") + File.separator + "jwdifdef.dot");
			FileWriter writer = new FileWriter(file);
			exporter.export(writer, graph);
			writer.close();

			File file2Dot = new File(System.getProperty("user.home") + File.separator + "cfg.dot");
			FileWriter fileWriter2 = new FileWriter(file2Dot);
			fileWriter2.write(cfg.toDot(sharedAnalysis));
			fileWriter2.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void _debug(ControlFlowGraph cfg, final DefaultDirectedGraph<Object, ValueContainerEdge> reachesData, DefaultDirectedGraph<Object, ValueContainerEdge> filteredReachesData, DirectAnalysis<LatticeSet<Object>> analysisResult) {
		// XXX DEBUG code. Move it somewhere else.
		DOTExporter<Object, ValueContainerEdge> exporter = new DOTExporter<Object, ValueContainerEdge>(new StringNameProvider<Object>() {
			@Override
			public String getVertexName(Object vertex) {
				if (vertex instanceof Write) {
					return "\"[" + ((Write) vertex).getToken().getLine() + "]w " + vertex.toString().replace("\"", "'") + "\"";
				} else if (vertex instanceof Read) {
					return "\"[" + ((Read) vertex).getToken().getLine() + "]r " + vertex.toString().replace("\"", "'") + "\"";
				} else {
					return "\"" + vertex.toString().replace("\"", "'") + "\"";
				}
			}
		}, null, null);
		try {
			File file1Dot = new File(System.getProperty("user.home") + File.separator + "jwd.dot");
			FileWriter fileWriter1 = new FileWriter(file1Dot);
			exporter.export(fileWriter1, reachesData);
			fileWriter1.close();

			File file3Dot = new File(System.getProperty("user.home") + File.separator + "jwdf.dot");
			FileWriter fileWriter3 = new FileWriter(file3Dot);
			exporter.export(fileWriter3, filteredReachesData);
			fileWriter3.close();

			File file2Dot = new File(System.getProperty("user.home") + File.separator + "cfg.dot");
			FileWriter fileWriter2 = new FileWriter(file2Dot);
			fileWriter2.write(cfg.toDot(analysisResult));
			fileWriter2.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
