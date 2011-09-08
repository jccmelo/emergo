package br.ufpe.cin.emergo.handlers;

import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedPseudograph;

import dk.au.cs.java.compiler.cfg.ControlFlowGraph;
import dk.au.cs.java.compiler.cfg.ForwardStrategy;
import dk.au.cs.java.compiler.cfg.analysis.Analysis;
import dk.au.cs.java.compiler.cfg.analysis.Process;
import dk.au.cs.java.compiler.cfg.analysis.VariableInfo;
import dk.au.cs.java.compiler.cfg.analysis.VariableRead;
import dk.au.cs.java.compiler.cfg.point.Expression;
import dk.au.cs.java.compiler.cfg.point.LValue;
import dk.au.cs.java.compiler.cfg.point.Point;
import dk.au.cs.java.compiler.cfg.point.Read;
import dk.au.cs.java.compiler.cfg.point.Variable;
import dk.au.cs.java.compiler.cfg.point.Write;
import dk.au.cs.java.compiler.cfg.visualizer.VisualizerPropertyCollector;
import dk.au.cs.java.compiler.node.Token;
import dk.brics.lattice.Lattice;
import dk.brics.lattice.LatticeSet;
import dk.brics.lattice.LatticeSetFilter;
import dk.brics.lattice.UnionSetLattice;
import dk.brics.util.collection.CollectionUtil;
import dk.brics.util.collection.Stringifier;

/**
 * @author Johnni Winther &lt;<a href="mailto:jw@cs.au.dk">jw@cs.au.dk</a>&gt;
 */
public class DefUseRules extends Analysis<LatticeSet<Object>> {
	private static final String ID = "DU";

	private Graph<Object, ValueContainerEdge> reachesData;

	public DefUseRules(Graph<Object, ValueContainerEdge> reachesData) {
		super(ID, ForwardStrategy.INSTANCE);
		this.reachesData = reachesData;
	}

	@Override
	public Lattice<LatticeSet<Object>> createLattice(@SuppressWarnings("unused") ControlFlowGraph controlFlowGraph) {
		return new UnionSetLattice<Object>();
	}

	@Override
	public void generateProperty(VisualizerPropertyCollector collector) {
		collector.addNodeSetProperty(ID, "DefUse");
	}

	@Override
	public void generateProperties(VisualizerPropertyCollector collector, VariableRead<LatticeSet<Object>> info) {
		for (Point source : collector.getPoints()) {
			if (source instanceof Read) {
				Read read = (Read) source;
				LatticeSet<Object> variable = info.getVariable(source);
				if (variable != null) {
					Set<Point> points = CollectionUtil.newInsertOrderSet();
					for (Point target : collector.getPoints()) {
						points.add(target);
						// if (target instanceof Write) {
						// Write write = (Write) target;
						// if (read.getExpression().equals(write.getLValue())) {
						// points.add(write);
						// }
						// }
					}
					collector.addNodeSetPropertyValue(source, ID, points);
				}
			}
		}
	}

	@Override
	public String getVariableText(LatticeSet<Object> variable) {
		if (variable == null) {
			return "";
		}
		return variable.toString(new Stringifier<Object>() {

			public String toString(Object point) {
				return ((Point) point).getLabelText();
			}
		});
	}

	@Override
	public void computeRead(final Read point, VariableInfo<LatticeSet<Object>> info) {
		info.process(point, new Process<LatticeSet<Object>>() {

			public LatticeSet<Object> process(LatticeSet<Object> set) {
				set = set.filter(new LatticeSetFilter<Object>() {

					public boolean accept(Object element) {
						if (element instanceof Write) {
							Write write = (Write) element;
							if (write.getLValue().equals(point.getExpression())) {
								reachesData.addVertex(write);
								reachesData.addVertex(point);
								reachesData.addEdge(write, point);
							}
							return true;
						} else if (element instanceof Read) {
							Read read = (Read) element;
							if (point.toString().contains(read.getVariable().toString())) {
								reachesData.addVertex(read);
								reachesData.addVertex(point);
								reachesData.addEdge(read, point);
							}
							return true;
						} else {
							// this should not happen at all.
							assert false;
							return false;
						}
					}
				});
				set = set.include(point);
				return set;
			}
		});
	}

	@Override
	public void computeWrite(final Write point, VariableInfo<LatticeSet<Object>> info) {

		info.process(point, new Process<LatticeSet<Object>>() {

			public LatticeSet<Object> process(LatticeSet<Object> set) {
				final LValue lvalue = point.getLValue();
				set = set.filter(new LatticeSetFilter<Object>() {

					public boolean accept(Object element) {
						if (element instanceof Write) {
							Write write = (Write) element;
							LValue lv = write.getLValue();
							return !lv.equals(lvalue);
						} else if (element instanceof Read) {
							Read read = (Read) element;
							if (point.getVariable().equals(read.getVariable())) {
								reachesData.addVertex(point);
								reachesData.addVertex(read);
								reachesData.addEdge(read, point);
							}
							return true;
						} else {
							// this should not happen at all.
							assert false;
							return false;
						}
					}

					public String toString() {
						return "!(" + lvalue + " = ...)";
					}
				});
				set = set.include(point);
				return set;
			}
		});
	}
}
