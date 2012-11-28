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

package br.ufpe.cin.emergo.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import soot.toolkits.scalar.AbstractFlowSet;
import soot.toolkits.scalar.FlowSet;
import br.ufpe.cin.emergo.instrument.IConfigRep;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class ReversedMapLiftedFlowSet extends AbstractFlowSet {
	protected BiMap<FlowSet, IConfigRep> map;

	public BiMap<FlowSet, IConfigRep> getMapping() {
		return map;
	}

	protected ReversedMapLiftedFlowSet(Map<FlowSet, IConfigRep> map) {
		this.map = HashBiMap.create(map);
	}

	public ReversedMapLiftedFlowSet(Collection<IConfigRep> configs) {
		this.map = HashBiMap.create();
		for (IConfigRep config : configs) {
			map.put(new ArraySparseSet(), config);
		}
	}

	public ReversedMapLiftedFlowSet(IConfigRep seed) {
		this.map = HashBiMap.create();
		map.put(new ArraySparseSet(), seed);
	}

	public ReversedMapLiftedFlowSet() {
		this.map = HashBiMap.create();
	}

	@Override
	public ReversedMapLiftedFlowSet clone() {
		Set<Entry<FlowSet, IConfigRep>> entrySet = map.entrySet();
		Map<FlowSet, IConfigRep> newMap = new HashMap<FlowSet, IConfigRep>();
		for (Entry<FlowSet, IConfigRep> entry : entrySet) {
			newMap.put(entry.getKey().clone(), entry.getValue());
		}
		return new ReversedMapLiftedFlowSet(newMap);
	}

	@Override
	public void copy(FlowSet dest) {
		ReversedMapLiftedFlowSet destLifted = (ReversedMapLiftedFlowSet) dest;
		dest.clear();
		Set<Entry<FlowSet, IConfigRep>> entrySet = map.entrySet();
		for (Entry<FlowSet, IConfigRep> entry : entrySet) {
			FlowSet key = entry.getKey();
			IConfigRep value = entry.getValue();
			destLifted.map.put(key.clone(), value);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (this == o)
			return true;
		if (!(o instanceof ReversedMapLiftedFlowSet))
			return false;
		ReversedMapLiftedFlowSet that = (ReversedMapLiftedFlowSet) o;
		return this.map.equals(that.map);
	}
	
	@Override
	public int hashCode() {
		return 7 + this.map.hashCode();
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public void union(FlowSet aOther, FlowSet aDest) {
		ReversedMapLiftedFlowSet other = (ReversedMapLiftedFlowSet) aOther;
		ReversedMapLiftedFlowSet dest = (ReversedMapLiftedFlowSet) aDest;

		Set<Entry<FlowSet, IConfigRep>> otherEntrySet = other.map.entrySet();

		dest.map = HashBiMap.create(this.map);
		for (Entry<FlowSet, IConfigRep> otherEntry : otherEntrySet) {
			FlowSet key = otherEntry.getKey();
			dest.putAndMerge(key, otherEntry.getValue());
		}
	}

	@Override
	public void intersection(FlowSet aOther, FlowSet aDest) {
		// TODO
		throw new UnsupportedOperationException();
	}

	public FlowSet add(FlowSet flow, IConfigRep config) {
		return (FlowSet) map.put(flow, config);
	}

	@Override
	public void add(Object arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void remove(Object arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return this.map.size();
	}

	@Override
	public List toList() {
		List list = new ArrayList(this.map.values());
		
		return list;
	}

	@Override
	public String toString() {
		return map.toString();
	}
	
	public void putAndMerge(FlowSet flowSet, IConfigRep config) {
		boolean containsKey = map.containsKey(flowSet);
		boolean containsVal = map.containsValue(config);
		if (!containsKey && !containsVal) {
			map.put(flowSet, config);
			return;
		} 
		if (containsKey && containsVal) {
			IConfigRep inConfig = map.get(flowSet);
			FlowSet inFlowSet = map.inverse().get(config);
			map.remove(flowSet);
			ArraySparseSet unionFlowSet = new ArraySparseSet();
			inFlowSet.union(flowSet, unionFlowSet);
			putAndMerge(unionFlowSet, inConfig.union(config));
			return;
		} 
		if (containsKey) {
			IConfigRep inConfig = map.get(flowSet);
			IConfigRep union = inConfig.union(config);
			map.remove(flowSet);
			putAndMerge(flowSet, union);
			return;
		} 
		if (containsVal) {
			BiMap<IConfigRep, FlowSet> inverse = map.inverse();
			FlowSet inFlowSet = inverse.get(config);
			ArraySparseSet unionFlowSet = new ArraySparseSet();
			inFlowSet.union(flowSet, unionFlowSet);
			inverse.remove(config);
			putAndMerge(unionFlowSet, config);
			return;
		}
	}
}