package br.ufpe.cin.emergo.core;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import dk.au.cs.java.compiler.cfg.ControlFlowGraph;
import dk.au.cs.java.compiler.cfg.ForwardStrategy;
import dk.au.cs.java.compiler.cfg.analysis.Analysis;
import dk.au.cs.java.compiler.cfg.analysis.Process;
import dk.au.cs.java.compiler.cfg.analysis.ProcessInfo;
import dk.au.cs.java.compiler.cfg.point.LValue;
import dk.au.cs.java.compiler.cfg.point.Point;
import dk.au.cs.java.compiler.cfg.point.Read;
import dk.au.cs.java.compiler.cfg.point.Write;
import dk.brics.lattice.Lattice;
import dk.brics.lattice.LatticeSet;
import dk.brics.lattice.LatticeSetFilter;
import dk.brics.lattice.UnionSetLattice;
import dk.brics.util.collection.Stringifier;

/**
 * Reaching definitions rules for a flow analysis. A def-use graph is built during the analysis, not after. Note that
 * his def-use graph is *not* feature sensistive; the flow analysis frameworks separates the rules (transfer functions)
 * of the analysis from the other logic.
 * 
 * 
 * @author Társis Tolêdo
 * 
 */
public class DefUseRules extends Analysis<LatticeSet<Object>> {

	/**
	 * DUW: Def-Use Web.
	 */
	private static final String ID = "DUW";

	private Graph<Object, DefaultEdge> reachesData;

	private boolean buildGraph;

	/**
	 * Instantiates the rules for the DefUse web.
	 */
	public DefUseRules() {
		super(ID, ForwardStrategy.INSTANCE);
		this.reachesData = null;
		this.buildGraph = false;
	}

	/**
	 * Instantiates the rules for the DefUse web. Will try to build a graph along the way.
	 * 
	 * @param reachesData
	 */
	public DefUseRules(Graph<Object, DefaultEdge> reachesData) {
		super(ID, ForwardStrategy.INSTANCE);
		this.reachesData = reachesData;
		this.buildGraph = true;
	}

	/**
	 * Adds both src and tgt as vertices to {@code reachesData} and also an edge from src to tgt.
	 * 
	 * @param src
	 * @param tgt
	 */
	private void addVerticesAndEdge(Point src, Point tgt) {
		if (buildGraph) {
			reachesData.addVertex(src);
			reachesData.addVertex(tgt);
			reachesData.addEdge(src, tgt);
		} else {
			throw new IllegalStateException("this method should not be called when on graphless mode.");
		}
	}

	@Override
	public Lattice<LatticeSet<Object>> createLattice(ControlFlowGraph controlFlowGraph) {
		return new UnionSetLattice<Object>();
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
	public void computeRead(final Read point, ProcessInfo<LatticeSet<Object>> info) {
		info.process(point, new Process<LatticeSet<Object>>() {

			public LatticeSet<Object> process(LatticeSet<Object> set) {
				set = set.filter(new LatticeSetFilter<Object>() {

					public boolean accept(Object element) {
						if (buildGraph) {
							if (element instanceof Write) {
								Write write = (Write) element;
								if (write.getLValue().equals(point.getExpression())) {
									addVerticesAndEdge(write, point);
								}
								return true;
							} else if (element instanceof Read) {
								Read read = (Read) element;
								if (point.toString().contains(read.getVariable().toString())) {
									addVerticesAndEdge(read, point);
								}
								return true;
							} else {
								// this should not happen at all.
								assert false;
								return false;
							}
						} else {
							return true;
						}
					}
				});
				set = set.include(point);
				return set;
			}
		});
	}

	@Override
	public void computeWrite(final Write point, ProcessInfo<LatticeSet<Object>> info) {

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
							if (buildGraph) {
								Read read = (Read) element;
								if (point.getVariable().equals(read.getVariable())) {
									addVerticesAndEdge(read, point);
								}
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
