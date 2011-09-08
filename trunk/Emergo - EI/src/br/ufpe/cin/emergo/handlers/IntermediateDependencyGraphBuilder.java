package br.ufpe.cin.emergo.handlers;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.StringNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;

import dk.au.cs.java.compiler.cfg.ControlFlowGraph;
import dk.au.cs.java.compiler.cfg.DirectAnalysis;
import dk.au.cs.java.compiler.cfg.analysis.AnalysisProcessor;
import dk.au.cs.java.compiler.cfg.point.Point;
import dk.au.cs.java.compiler.cfg.point.Read;
import dk.au.cs.java.compiler.cfg.point.Write;
import dk.au.cs.java.compiler.node.AMethodDecl;
import dk.au.cs.java.compiler.node.AProgram;
import dk.brics.lattice.LatticeSet;

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

		DirectAnalysis<LatticeSet<Object>> analysisResult = AnalysisProcessor.process(methodDecl, new DefUseRules(reachesData));

		DefaultDirectedGraph<Object, ValueContainerEdge> filteredReachesData = filterWithUserSelection(pointsInUserSelection, reachesData);

		// XXX Prints out stuff to help debugging. Remove later.
		_debug(cfg, reachesData, filteredReachesData, analysisResult);

		return reachesData;
	}

	private static DefaultDirectedGraph<Object, ValueContainerEdge> filterWithUserSelection(List<Point> pointsInUserSelection, DefaultDirectedGraph<Object, ValueContainerEdge> reachesData) {
		final DefaultDirectedGraph<Object, ValueContainerEdge> filteredGraph = new DefaultDirectedGraph<Object, ValueContainerEdge>(ValueContainerEdge.class);

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
				filteredGraph.addEdge(head, target);
				if (alreadyVisitedPoints.add(target)) {
					workList.add(target);
				}
			}
		}

		return filteredGraph;
	}

	// public static void build(ControlFlowGraph controlFlowGraph, SharedSimultaneousAnalysis<?> analysisResult) {
	// DirectedMultigraph<Write, ValueContainerEdge> reachesData = new DirectedMultigraph<Write,
	// ValueContainerEdge>(ValueContainerEdge.class);
	// // List of points to be visited. Starts with the CFG entry point.
	// LinkedList<Point> pendingPoints = new LinkedList<Point>();
	// LinkedList<Point> visitedPoints = new LinkedList<Point>();
	// pendingPoints.add(controlFlowGraph.getEntryPoint());
	//
	// // Worklist-like iteration idiom
	// while (!pendingPoints.isEmpty()) {
	// // Consumes 1st point
	// Point poppedPoint = pendingPoints.removeFirst();
	// visitedPoints.add(poppedPoint);
	//
	// // For each edge out of the point being iterated...
	// Set<? extends Edge> outgoingEdges = poppedPoint.getOutgoingEdges();
	// for (Edge edge : outgoingEdges) {
	// // ...adds targets to iterate later.
	// pendingPoints.add(edge.getTarget());
	//
	// // The analysis information can be retrieve from the analysis using an edge.
	// Map<IfDefVarSet, LatticeSet<Write>> variable = (Map<IfDefVarSet, LatticeSet<Write>>)
	// analysisResult.getVariable(edge);
	// Set<Entry<IfDefVarSet, LatticeSet<Write>>> entrySet = variable.entrySet();
	//
	// for (Entry<IfDefVarSet, LatticeSet<Write>> entry : entrySet) {
	// LatticeSet<Write> value = entry.getValue();
	//
	// // **Hacks** a LatticeSet to get information in a private field: XXX unsafe.
	// Set<Write> lattice = LatticeSetUtil.getSet(value);
	// for (Write write : lattice) {
	// Token writeToken = write.getToken();
	//
	// }
	// }
	// }
	// }
	// }
	//
	// public static void build(ControlFlowGraph controlFlowGraph, DirectAnalysis<?> analysisResult) {
	// // List of points to be visited. Starts with the CFG entry point.
	// LinkedList<Point> pendingPoints = new LinkedList<Point>();
	// LinkedList<Point> visitedPoints = new LinkedList<Point>();
	// pendingPoints.add(controlFlowGraph.getEntryPoint());
	//
	// // Worklist-like iteration idiom
	// while (!pendingPoints.isEmpty()) {
	// // Consumes 1st point
	// Point poppedPoint = pendingPoints.removeFirst();
	// visitedPoints.add(poppedPoint);
	// Token token = poppedPoint.getToken();
	//
	// // For each edge out of the point being iterated...
	// Set<? extends Edge> outgoingEdges = poppedPoint.getOutgoingEdges();
	// for (Edge edge : outgoingEdges) {
	// // ...adds targets to iterate later.
	// pendingPoints.add(edge.getTarget());
	//
	// // The analysis information can be retrieve from the analysis using an edge.
	// LatticeSet<Write> variable = (LatticeSet<Write>) analysisResult.getVariable(edge);
	//
	// // **Hacks** a LatticeSet to get information in a private field: XXX unsafe.
	// Set<Write> lattice = LatticeSetUtil.getSet(variable);
	// for (Write write : lattice) {
	// Token writeToken = write.getToken();
	//
	// }
	// }
	// }
	// }

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
