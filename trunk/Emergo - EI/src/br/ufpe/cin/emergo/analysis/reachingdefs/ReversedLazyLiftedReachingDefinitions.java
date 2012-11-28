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

import java.util.Iterator;
import java.util.Map.Entry;

import soot.Unit;
import soot.jimple.AssignStmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import br.ufpe.cin.emergo.analysis.ArraySparseSet;
import br.ufpe.cin.emergo.analysis.ReversedMapLiftedFlowSet;
import br.ufpe.cin.emergo.instrument.FeatureTag;
import br.ufpe.cin.emergo.instrument.IConfigRep;
import br.ufpe.cin.emergo.instrument.IFeatureRep;
import br.ufpe.cin.emergo.instrument.ILazyConfigRep;
import br.ufpe.cin.emergo.util.Pair;

import com.google.common.collect.BiMap;

/**
 * This implementation of the Reaching Definitions analysis uses a LiftedFlowSet as a lattice element. The only major
 * change is how its KILL method is implemented. Everything else is quite similar to a 'regular' FlowSet-based analysis.
 */
public class ReversedLazyLiftedReachingDefinitions extends ForwardFlowAnalysis<Unit, ReversedMapLiftedFlowSet> {

	private ILazyConfigRep configurations;

	/**
	 * Instantiates a new TestReachingDefinitions.
	 * 
	 * @param graph
	 *            the graph
	 */
	public ReversedLazyLiftedReachingDefinitions(DirectedGraph<Unit> graph, ILazyConfigRep configs) {
		super(graph);
		this.configurations = configs;
		super.doAnalysis();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.AbstractFlowAnalysis#copy(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void copy(ReversedMapLiftedFlowSet source, ReversedMapLiftedFlowSet dest) {
		source.copy(dest);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.AbstractFlowAnalysis#merge(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void merge(ReversedMapLiftedFlowSet source1, ReversedMapLiftedFlowSet source2, ReversedMapLiftedFlowSet dest) {
		source1.union(source2, dest);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.AbstractFlowAnalysis#entryInitialFlow()
	 */
	@Override
	protected ReversedMapLiftedFlowSet entryInitialFlow() {
		return new ReversedMapLiftedFlowSet(configurations);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.AbstractFlowAnalysis#newInitialFlow()
	 */
	@Override
	protected ReversedMapLiftedFlowSet newInitialFlow() {
		return new ReversedMapLiftedFlowSet(configurations);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.FlowAnalysis#flowThrough(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void flowThrough(ReversedMapLiftedFlowSet source, Unit unit, ReversedMapLiftedFlowSet dest) {
		
		if (unit instanceof AssignStmt) {
			AssignStmt assignment = (AssignStmt) unit;
	
			// clear the destination lattice to insert new ones
			dest.clear();
	
			// get feature information for the unit which this transfer function is being applied to
			FeatureTag tag = (FeatureTag) assignment.getTag(FeatureTag.FEAT_TAG_NAME);
			IFeatureRep featureRep = tag.getFeatureRep();
	
			// iterate over all entries of the lazy flowset (source)
			BiMap<FlowSet, IConfigRep> sourceMapping = source.getMapping();
			Iterator<Entry<FlowSet, IConfigRep>> iterator = sourceMapping.entrySet().iterator();
	
			while (iterator.hasNext()) {
				Entry<FlowSet, IConfigRep> entry = iterator.next();
				ILazyConfigRep lazyConfig = (ILazyConfigRep) entry.getValue();
	
				FlowSet sourceFlowSet = entry.getKey();
	
				/*
				 *  The split of a lazy configuration L by another lazy configuration O
				 *  gives rise to two other lazy configurations, the FIRST one contains
				 *  the set of configuration that both L and O "have in common", and
				 *  the SECOND contains the set of the "rest".
				 *  
				 *  Thus, if the size of FIRST is 0, there are no configurations in common.
				 *  If the size of FIRST is the same as the size of O, than their corresponding
				 *  sets are the same as well. 
				 */
				Pair<ILazyConfigRep, ILazyConfigRep> split = lazyConfig.split(featureRep);
				ILazyConfigRep first = split.getFirst();
	
				if (first.size() != 0) {
					if (first.size() == lazyConfig.size()) {
						FlowSet destFlowSet = new ArraySparseSet();
						
						/*
						 *  This mutates the destFlowSet to contain the result of the GEN/KILL
						 *  operations.
						 */
						kill(sourceFlowSet, assignment, destFlowSet);
						gen(assignment, destFlowSet);
						
						dest.putAndMerge(destFlowSet, first);
					} else {
						FlowSet destFlowSet = sourceFlowSet.clone();
						/*
						 * This lazy configuration must map the same value that L
						 * mapped to.
						 */
						ILazyConfigRep second = split.getSecond();
						if (second.size() != 0) {
							dest.putAndMerge(destFlowSet, second);
						}
						/*
						 * This flowset will contain the result of the GEN/KILL operations,
						 * and is to be mapped from FIRST.
						 */
						FlowSet destToBeAppliedLattice = new ArraySparseSet();
						kill(sourceFlowSet, assignment, destToBeAppliedLattice);
						gen(assignment, destToBeAppliedLattice);
						
						dest.putAndMerge(destToBeAppliedLattice, first);
					}
				} else {
					/*
					 *  There is nothing to be done in this case, thus we only copy the mapping
					 *  from the source.
					 */
					dest.putAndMerge(sourceFlowSet, lazyConfig);
				}
			}
		} else {
			source.copy(dest);
		}
	}

	protected void kill(FlowSet source, AssignStmt assignment, FlowSet dest) {
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
	 * @param configuration
	 */
	protected void gen(AssignStmt unit, FlowSet dest) {
		dest.add(unit);
	}

}
