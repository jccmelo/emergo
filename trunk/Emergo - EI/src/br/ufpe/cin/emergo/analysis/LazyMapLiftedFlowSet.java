package br.ufpe.cin.emergo.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import soot.toolkits.scalar.FlowSet;
import br.ufpe.cin.emergo.instrument.IConfigRep;
import br.ufpe.cin.emergo.instrument.ILazyConfigRep;

public class LazyMapLiftedFlowSet extends AbstractMapLiftedFlowSet {
	
	public Map<IConfigRep, FlowSet> getMapping() {
		return this.map;
	}

	public LazyMapLiftedFlowSet(Set<IConfigRep> configs) {
		this.map = new HashMap<IConfigRep, FlowSet>();
		for (IConfigRep config : configs) {
			this.map.put(config, new ArraySparseSet());
		}
	}

	public LazyMapLiftedFlowSet(Map<IConfigRep, FlowSet> map) {
		this.map = new HashMap<IConfigRep, FlowSet>(map);
	}
	
	public LazyMapLiftedFlowSet(ILazyConfigRep seed) {
		this.map = new HashMap<IConfigRep, FlowSet>();
		this.map.put(seed, new ArraySparseSet());
	}

	@Override
	public LazyMapLiftedFlowSet clone() {
		Set<Entry<IConfigRep, FlowSet>> entrySet = map.entrySet();
		Map<IConfigRep, FlowSet> newMap = new HashMap<IConfigRep, FlowSet>();
		for (Entry<IConfigRep, FlowSet> entry : entrySet) {
			newMap.put(entry.getKey(), entry.getValue().clone());
		}
		return new LazyMapLiftedFlowSet(newMap);
	}

	@Override
	public void intersection(FlowSet aOther, FlowSet aDest) {
		AbstractMapLiftedFlowSet other = (AbstractMapLiftedFlowSet) aOther;
		AbstractMapLiftedFlowSet dest = (AbstractMapLiftedFlowSet) aDest;

		Set<Entry<IConfigRep, FlowSet>> entrySet = this.map.entrySet();
		Set<Entry<IConfigRep, FlowSet>> otherEntrySet = other.map.entrySet();

		HashMap<IConfigRep, FlowSet> destMap = new HashMap<IConfigRep, FlowSet>();

		for (Entry<IConfigRep, FlowSet> entry : entrySet) {
			for (Entry<IConfigRep, FlowSet> otherEntry : otherEntrySet) {
				ILazyConfigRep key = (ILazyConfigRep) entry.getKey();
				ILazyConfigRep otherKey = (ILazyConfigRep) otherEntry.getKey();

				ILazyConfigRep intersection = (ILazyConfigRep) key.intersection(otherKey);
				if (intersection.size() != 0) {
					FlowSet otherFlowSet = otherEntry.getValue();
					ArraySparseSet destFlowSet = new ArraySparseSet();
					entry.getValue().intersection(otherFlowSet, destFlowSet);
					destMap.put(intersection, destFlowSet);
					
					if (intersection.equals(key)) {
						break;
					}
				}
			}
		}

		dest.map = destMap;
	}
	
	@Override
	public void union(FlowSet aOther, FlowSet aDest) {
		AbstractMapLiftedFlowSet other = (AbstractMapLiftedFlowSet) aOther;
		AbstractMapLiftedFlowSet dest = (AbstractMapLiftedFlowSet) aDest;

		Set<Entry<IConfigRep, FlowSet>> entrySet = this.map.entrySet();
		Set<Entry<IConfigRep, FlowSet>> otherEntrySet = other.map.entrySet();

		HashMap<IConfigRep, FlowSet> destMap = new HashMap<IConfigRep, FlowSet>();

		for (Entry<IConfigRep, FlowSet> entry : entrySet) {
			for (Entry<IConfigRep, FlowSet> otherEntry : otherEntrySet) {
				ILazyConfigRep key = (ILazyConfigRep) entry.getKey();
				ILazyConfigRep otherKey = (ILazyConfigRep) otherEntry.getKey();

				ILazyConfigRep intersection = (ILazyConfigRep) key.intersection(otherKey);
				if (intersection.size() != 0) {
					FlowSet otherFlowSet = otherEntry.getValue();
					ArraySparseSet destFlowSet = new ArraySparseSet();
					entry.getValue().union(otherFlowSet, destFlowSet);
					destMap.put(intersection, destFlowSet);
					
					if (intersection.equals(key)) {
						break;
					}
				}
			}
		}

		dest.map = destMap;
	}

	public FlowSet getLattice(ILazyConfigRep lazyConfig) {
		return this.map.get(lazyConfig);
	}

}
