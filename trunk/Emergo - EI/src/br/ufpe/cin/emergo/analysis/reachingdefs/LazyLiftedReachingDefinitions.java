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
import java.util.Map;
import java.util.Map.Entry;

import soot.Unit;
import soot.jimple.AssignStmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import br.ufpe.cin.emergo.analysis.ArraySparseSet;
import br.ufpe.cin.emergo.analysis.LazyMapLiftedFlowSet;
import br.ufpe.cin.emergo.instrument.FeatureTag;
import br.ufpe.cin.emergo.instrument.IConfigRep;
import br.ufpe.cin.emergo.instrument.IFeatureRep;
import br.ufpe.cin.emergo.instrument.ILazyConfigRep;
import br.ufpe.cin.emergo.util.Pair;

/**
 * This implementation of the Reaching Definitions analysis uses a LiftedFlowSet as a lattice element. The only major
 * change is how its KILL method is implemented. Everything else is quite similar to a 'regular' FlowSet-based analysis.
 */
public class LazyLiftedReachingDefinitions extends ForwardFlowAnalysis<Unit, LazyMapLiftedFlowSet> {

	private ILazyConfigRep configurations;

	/**
	 * Instantiates a new TestReachingDefinitions.
	 * 
	 * @param graph
	 *            the graph
	 */
	public LazyLiftedReachingDefinitions(DirectedGraph<Unit> graph, ILazyConfigRep configs) {
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
	protected void copy(LazyMapLiftedFlowSet source, LazyMapLiftedFlowSet dest) {
		source.copy(dest);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.AbstractFlowAnalysis#merge(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void merge(LazyMapLiftedFlowSet source1, LazyMapLiftedFlowSet source2, LazyMapLiftedFlowSet dest) {
		source1.union(source2, dest);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.AbstractFlowAnalysis#entryInitialFlow()
	 */
	@Override
	protected LazyMapLiftedFlowSet entryInitialFlow() {
		return new LazyMapLiftedFlowSet(configurations);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.AbstractFlowAnalysis#newInitialFlow()
	 */
	@Override
	protected LazyMapLiftedFlowSet newInitialFlow() {
		return new LazyMapLiftedFlowSet(configurations);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see soot.toolkits.scalar.FlowAnalysis#flowThrough(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void flowThrough(LazyMapLiftedFlowSet source, Unit unit, LazyMapLiftedFlowSet dest) {

		// pre-copy the information from source to dest
		source.copy(dest);
		
		if (unit instanceof AssignStmt) {
			AssignStmt assignment = (AssignStmt) unit;
	
			// get feature instrumentation for this unit
			FeatureTag tag = (FeatureTag) assignment.getTag(FeatureTag.FEAT_TAG_NAME);
			IFeatureRep featureRep = tag.getFeatureRep();
	
			Map<IConfigRep, FlowSet> destMapping = dest.getMapping();
	
			// iterate over all entries of the lazy flowset (source)
			Map<IConfigRep, FlowSet> sourceMapping = source.getMapping();
			Iterator<Entry<IConfigRep, FlowSet>> iterator = sourceMapping.entrySet().iterator();
	
			while (iterator.hasNext()) {
				Entry<IConfigRep, FlowSet> entry = iterator.next();
				ILazyConfigRep lazyConfig = (ILazyConfigRep) entry.getKey();
	
				FlowSet sourceFlowSet = entry.getValue();
				FlowSet destFlowSet = destMapping.get(lazyConfig);
	
				/*
				 * gets the set of configurations whose lattices should be passed to the transfer function.
				 * 
				 * applyToConfigurations = 0 => copy the lattice to dest
				 * 
				 * applyToConfigurations != 0 && applyToConfigurations == lazyConfig => apply the transfer function
				 * 
				 * applyToConfigurations != 0 && applyToConfigurations != lazyConfig => split and apply the transfer
				 * function (on who?)
				 * 
				 * the 1st case has already been addressed with the pre-copy
				 */
				Pair<ILazyConfigRep, ILazyConfigRep> split = lazyConfig.split(featureRep);
				ILazyConfigRep first = split.getFirst();
	
				if (first.size() != 0) {
					if (first.size() == lazyConfig.size()) {
						kill(sourceFlowSet, assignment, destFlowSet);
						gen(assignment, destFlowSet);
					} else {
						ILazyConfigRep second = split.getSecond();
	
						/*
						 * in this case, this lattice doesnt have a copy from the sourceFlowSet
						 */
						FlowSet destToBeAppliedLattice = new ArraySparseSet();
	
						// apply point-wise transfer function
						kill(sourceFlowSet, assignment, destToBeAppliedLattice);
						gen(assignment, destToBeAppliedLattice);
	
						/*
						 * make sure an empty config rep doesnt get into the lattice, or it will propagate garbage
						 */
						if (second.size() != 0) {
							destMapping.put(second, destFlowSet);
						}
	
						// add the new lattice
						destMapping.put(first, destToBeAppliedLattice);
	
						// remove config rep that has been split
						destMapping.remove(lazyConfig);
					}
				}
			}
		}
	}

	protected void kill(FlowSet source, AssignStmt assignStmt, FlowSet dest) {
		FlowSet kills = new ArraySparseSet();
		for (Object earlierAssignment : source.toList()) {
			if (earlierAssignment instanceof AssignStmt) {
				AssignStmt stmt = (AssignStmt) earlierAssignment;
				if (stmt.getLeftOp().equivTo(assignStmt.getLeftOp())) {
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
