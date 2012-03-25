package br.ufpe.cin.emergo.core;

import net.silentbeing.measurement.ClassMeasurementFilter;
import dk.au.cs.java.compiler.cfg.ControlFlowGraph;
import dk.au.cs.java.compiler.cfg.ForwardStrategy;
import dk.au.cs.java.compiler.cfg.analysis.Analysis;
import dk.au.cs.java.compiler.cfg.analysis.Process;
import dk.au.cs.java.compiler.cfg.analysis.ProcessInfo;
import dk.au.cs.java.compiler.cfg.analysis.rules.ReachingDefinitionsRules;
import dk.au.cs.java.compiler.cfg.point.LValue;
import dk.au.cs.java.compiler.cfg.point.Point;
import dk.au.cs.java.compiler.cfg.point.Read;
import dk.au.cs.java.compiler.cfg.point.Write;
import dk.au.cs.java.compiler.type.NameContext;
import dk.brics.lattice.BitSetLatticeStrategy;
import dk.brics.lattice.Lattice;
import dk.brics.lattice.LatticeSet;
import dk.brics.lattice.LatticeSetFilter;
import dk.brics.lattice.UnionSetLattice;
import dk.brics.util.collection.BitSetUniverse;
import dk.brics.util.collection.Stringifier;

/**
 * A similar implementation to the Reaching definitions
 * {@link ReachingDefinitionsRules} rules for a flow analysis, except that
 * lattice elements may be Reads or Writes.
 * 
 * @author Társis Tolêdo
 */
public class DefUseRules extends Analysis<LatticeSet<Point>> {

	/**
	 * DUW: Def-Use Web.
	 */
	private static final String ID = "DUW";
	private WriteProcessor writeProcessor = new WriteProcessor();
	private ReadProcessor readProcessor = new ReadProcessor();

	/**
	 * Instantiates the rules for the DefUse web.
	 */
	public DefUseRules() {
		super(ID, ForwardStrategy.INSTANCE);
		measurementFilter = new ClassMeasurementFilter(Point.class);
	}

	@Override
	public Lattice<LatticeSet<Point>> createLattice(ControlFlowGraph controlFlowGraph) {
		return new UnionSetLattice<Point>(
				new BitSetLatticeStrategy<Point>(new BitSetUniverse<Point>()));
	}

	@Override
	public String getVariableText(LatticeSet<Point> variable) {
		if (variable == null) {
			return "";
		}
		return variable.toString(new Stringifier<Point>() {

			@Override
			public String toString(Point point) {
				return point.getLabelText(new NameContext());
			}
		});
	}

	/* 
	 * ReadProcessor encapsulates the logic involved in the application
	 * of the transfer function for a Read.
	 */
	class ReadProcessor implements Process<LatticeSet<Point>> {
		private Read read = null;

		public ReadProcessor setRead(Read read) {
			this.read = read;
			return this;
		}

		@Override
		public LatticeSet<Point> process(LatticeSet<Point> set) {
			return set.include(read);
		}
	}

	@Override
	public void computeRead(final Read point, ProcessInfo<LatticeSet<Point>> info) {
		/*
		 * Blindly accept Reads.
		 */
		info.process(point, readProcessor.setRead(point));
	}

	/*
	 *	Encapsulates the logic behind the transfer function
	 * application in a Write. 
	 */
	class WriteProcessor implements Process<LatticeSet<Point>> {
		Write write = null;

		public WriteProcessor setWrite(Write write) {
			this.write = write;
			return this;
		}

		@Override
		public LatticeSet<Point> process(LatticeSet<Point> set) {
			final LValue lvalue = write.getLValue();
			set = set.filter(new LatticeSetFilter<Point>() {
				@Override
				public boolean accept(Point element) {
					if (element instanceof Write) {
						Write write = (Write)element;
						LValue lv = write.getLValue();
						return !lv.equals(lvalue);
					}
					return true;
				}

			});
			return set.include(write);
		}
	}

	@Override
	public void computeWrite(final Write point, ProcessInfo<LatticeSet<Point>> info) {
		/*
		 * First, kill any previous Write to the same LValue, then add this new one.
		 */
		info.process(point, writeProcessor.setWrite(point));
	}
}