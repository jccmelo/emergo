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

import java.util.Collection;
import java.util.Set;

import soot.Unit;
import soot.jimple.AssignStmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import br.ufpe.cin.emergo.analysis.ArraySparseSet;
import br.ufpe.cin.emergo.analysis.EagerMapLiftedFlowSet;
import br.ufpe.cin.emergo.instrument.FeatureTag;
import br.ufpe.cin.emergo.instrument.IConfigRep;
import br.ufpe.cin.emergo.instrument.IFeatureRep;

/**
 * This implementation of the Reaching Definitions analysis uses a LiftedFlowSet
 * as a lattice element. The only major change is how its KILL method is
 * implemented. Everything else is quite similar to a 'regular' FlowSet-based
 * analysis.
 */
public class LiftedReachingDefinitions extends ForwardFlowAnalysis<Unit, EagerMapLiftedFlowSet> {

	private Set<IConfigRep> configurations;

	/**
	 * Instantiates a new TestReachingDefinitions.
	 * 
	 * @param graph
	 *            the graph
	 */
	public LiftedReachingDefinitions(DirectedGraph<Unit> graph, Set<IConfigRep> configurations) {
		super(graph);
		this.configurations = configurations;
		super.doAnalysis();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.AbstractFlowAnalysis#copy(java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	protected void copy(EagerMapLiftedFlowSet source, EagerMapLiftedFlowSet dest) {
		source.copy(dest);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.AbstractFlowAnalysis#merge(java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void merge(EagerMapLiftedFlowSet source1, EagerMapLiftedFlowSet source2, EagerMapLiftedFlowSet dest) {
		source1.union(source2, dest);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.AbstractFlowAnalysis#entryInitialFlow()
	 */
	@Override
	protected EagerMapLiftedFlowSet entryInitialFlow() {
		return new EagerMapLiftedFlowSet(configurations);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.AbstractFlowAnalysis#newInitialFlow()
	 */
	@Override
	protected EagerMapLiftedFlowSet newInitialFlow() {
		return new EagerMapLiftedFlowSet(configurations);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.FlowAnalysis#flowThrough(java.lang.Object,
	 * java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void flowThrough(EagerMapLiftedFlowSet source, Unit unit, EagerMapLiftedFlowSet dest) {
		
		if (unit instanceof AssignStmt) {
			AssignStmt assignment = (AssignStmt) unit;
			
			FeatureTag tag = (FeatureTag) assignment.getTag(FeatureTag.FEAT_TAG_NAME);
			IFeatureRep featureRep = tag.getFeatureRep();
	
			Collection<IConfigRep> configs = source.getConfigurations();	
			for (IConfigRep config : configs) {
				FlowSet sourceFlowSet = source.getLattice(config);
				FlowSet destFlowSet = dest.getLattice(config);
				if (config.belongsToConfiguration(featureRep)) {
					kill(sourceFlowSet, assignment, destFlowSet);
					gen(destFlowSet, assignment);
				} else {
					sourceFlowSet.copy(destFlowSet);
				}
			}
		} else {
			source.copy(dest);
		}
		
	}

	/**
	 * Creates a KILL set for the given unit and remove the elements that are in
	 * KILL from the destination FlowSet.
	 * 
	 * @param source
	 * @param unit
	 * @param dest
	 * @param configuration
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
	 * Creates a GEN set for a given Unit and add it to the FlowSet dest. In
	 * this case, our GEN set are all the definitions present in the unit.
	 * 
	 * @param dest
	 *            the dest
	 * @param unit
	 *            the unit
	 * @param configuration
	 */
	private void gen(FlowSet dest, AssignStmt unit) {
		dest.add(unit);
	}

}
