package br.ufpe.cin.emergo.handlers;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.StringNameProvider;
import org.jgrapht.graph.DirectedGraphUnion;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.DirectedPseudograph;

import dk.au.cs.java.compiler.cfg.ControlFlowGraph;
import dk.au.cs.java.compiler.cfg.DirectAnalysis;
import dk.au.cs.java.compiler.cfg.analysis.AnalysisProcessor;
import dk.au.cs.java.compiler.cfg.analysis.Process;
import dk.au.cs.java.compiler.cfg.analysis.ReachingDefinitionsRules;
import dk.au.cs.java.compiler.cfg.analysis.VariableInfo;
import dk.au.cs.java.compiler.cfg.point.LValue;
import dk.au.cs.java.compiler.cfg.point.Point;
import dk.au.cs.java.compiler.cfg.point.Read;
import dk.au.cs.java.compiler.cfg.point.Write;
import dk.au.cs.java.compiler.node.AMethodDecl;
import dk.au.cs.java.compiler.node.AProgram;
import dk.au.cs.java.compiler.type.members.Method;
import dk.brics.lattice.LatticeSet;
import dk.brics.lattice.LatticeSetFilter;

public class IntermediateDependencyGraphBuilder {

	private static final class ReachingDefinitionsRulesExtension extends ReachingDefinitionsRules {
		private final DirectedGraph<Object, ValueContainerEdge> reachesData;

		private ReachingDefinitionsRulesExtension(DirectedGraph<Object, ValueContainerEdge> reachesData) {
			this.reachesData = reachesData;
		}

		@Override
		public void computeRead(final Read point, VariableInfo<LatticeSet<Write>> info) {
			info.process(point, new Process<LatticeSet<Write>>() {
				public LatticeSet<Write> process(LatticeSet<Write> set) {
					return set.filter(new LatticeSetFilter<Write>() {

						public boolean accept(Write write) {
							LValue lValue = write.getLValue();
							Read read = point;
							if (lValue.equals(read.getExpression())) {
								reachesData.addVertex(read);
								reachesData.addEdge(write, read);
							}
							return true;
						}
					});
				}
			});
			reachesData.addVertex(point);
		}
		
		@Override
		public void computeWrite(final Write point, VariableInfo<LatticeSet<Write>> info) {
			super.computeWrite(point, info);
			reachesData.addVertex(point);
		}
	}

	public static void build(AProgram node, ControlFlowGraph cfg, final List<Point> pointsInUserSelection, AMethodDecl methodDecl) {
		final DirectedPseudograph<Object, ValueContainerEdge> reachesData = new DirectedPseudograph<Object, ValueContainerEdge>(ValueContainerEdge.class);

		ReachingDefinitionsRules defUseRules = new ReachingDefinitionsRulesExtension(reachesData);
		DirectAnalysis<LatticeSet<Write>> analysisResult = AnalysisProcessor.process(methodDecl, defUseRules);
//		DirectAnalysis<LatticeSet<Object>> analysisResult = AnalysisProcessor.process(methodDecl, new DefUseRules());
		analysisResult.startAnalysis(cfg);
		analysisResult.endAnalysis(cfg);

		// XXX DEBUG code. Move it somewhere else.
		{
			DOTExporter<Object, ValueContainerEdge> exporter = new DOTExporter<Object, ValueContainerEdge>(new StringNameProvider<Object>() {
				@Override
				public String getVertexName(Object vertex) {
					if (vertex instanceof Write) {
						return "\"w " + vertex.toString().replace("\"", "'") + "\"";
					} else if (vertex instanceof Read) {
						return "\"r " + vertex.toString().replace("\"", "'") + "\"";
					} else {
						return "\"" + vertex.toString().replace("\"", "'") + "\"";
					}
				}
			}, null, null);
			try {
				File file1Dot = new File(System.getProperty("user.home") + File.separator + "jwdebuginfo.dot");
				FileWriter fileWriter1 = new FileWriter(file1Dot);
				exporter.export(fileWriter1, reachesData);
				fileWriter1.close();

				File file2Dot = new File(System.getProperty("user.home") + File.separator + "selectioncfg.dot");
				FileWriter fileWriter2 = new FileWriter(file2Dot);
				fileWriter2.write(cfg.toDot(analysisResult));
				fileWriter2.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
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

}
