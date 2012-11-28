/*
 * This is a prototype implementation of the concept of Feature-Sen
 * sitive Dataflow Analysis. More details in the AOSD'12 paper:
 * Dataflow Analysis for Software Product Lines
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package br.ufpe.cin.emergo.analysis.reachingdefs;

import soot.Unit;
import soot.jimple.AssignStmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import br.ufpe.cin.emergo.analysis.ArraySparseSet;
import br.ufpe.cin.emergo.instrument.FeatureTag;
import br.ufpe.cin.emergo.instrument.IConfigRep;
import br.ufpe.cin.emergo.instrument.IFeatureRep;

// TODO: Find a better name!
/**
 */
public class UnliftedReachingDefinitions extends ForwardFlowAnalysis<Unit, FlowSet> {

	/** The empty set. */
	private FlowSet emptySet = new ArraySparseSet();

	private IConfigRep configuration;

	/**
	 */
	public UnliftedReachingDefinitions(DirectedGraph<Unit> graph, IConfigRep configuration) {
		super(graph);
		this.configuration = configuration;
		super.doAnalysis();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.AbstractFlowAnalysis#copy(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void copy(FlowSet source, FlowSet dest) {
		source.copy(dest);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.AbstractFlowAnalysis#merge(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void merge(FlowSet source1, FlowSet source2, FlowSet dest) {
		source1.union(source2, dest);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.AbstractFlowAnalysis#entryInitialFlow()
	 */
	@Override
	protected FlowSet entryInitialFlow() {
		return this.emptySet.clone();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.AbstractFlowAnalysis#newInitialFlow()
	 */
	@Override
	protected FlowSet newInitialFlow() {
		return this.emptySet.clone();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.FlowAnalysis#flowThrough(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void flowThrough(FlowSet source, Unit unit, FlowSet dest) {
		
		if (unit instanceof AssignStmt) {
			AssignStmt assignment = (AssignStmt) unit;
	
			FeatureTag tag = (FeatureTag) assignment.getTag(FeatureTag.FEAT_TAG_NAME);
			IFeatureRep featureRep = tag.getFeatureRep();
	
			if (featureRep.belongsToConfiguration(configuration)) {
				kill(source, assignment, dest);
				gen(dest, assignment);
			} else {
				source.copy(dest);
			}
		} else {
			source.copy(dest);
		}
	}

	/**
	 * Creates a KILL set for a given Unit and it to the FlowSet dest. In this case, our KILL set are the Assignments
	 * made to the same Value that this Unit assigns to.
	 * 
	 * @param src
	 *            the src
	 * @param unit
	 *            the unit
	 * @param dest
	 *            the dest
	 */
	private void kill(FlowSet source, AssignStmt assignment, FlowSet dest) {
		FlowSet kills = new ArraySparseSet();
		for (Object earlierAssignment : source.toList()) {
			if (earlierAssignment instanceof AssignStmt) {
				AssignStmt stmt = (AssignStmt) earlierAssignment;
				if (stmt.getLeftOp().equivTo(assignment.getLeftOp())) {
					kills.add(earlierAssignment);
				}
			}
		}
		source.difference(kills, dest);
	}

	/**
	 * Creates a GEN set for a given Unit and it to the FlowSet dest. In this case, our GEN set are all the definitions
	 * present in the unit.
	 * 
	 * @param dest
	 *            the dest
	 * @param unit
	 *            the unit
	 */
	private void gen(FlowSet dest, AssignStmt unit) {
		dest.add(unit);
	}

}
