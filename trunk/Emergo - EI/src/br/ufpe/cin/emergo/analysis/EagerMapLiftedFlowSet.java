package br.ufpe.cin.emergo.analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import soot.toolkits.scalar.FlowSet;
import br.ufpe.cin.emergo.instrument.IConfigRep;

public class EagerMapLiftedFlowSet extends AbstractMapLiftedFlowSet {

	public Map<IConfigRep, FlowSet> getMapping() {
		return this.map;
	}
	
	public EagerMapLiftedFlowSet(Set<IConfigRep> configs) {
		this.map = new HashMap<IConfigRep, FlowSet>();
		for (IConfigRep config : configs) {
			this.map.put(config, new ArraySparseSet());
		}
	}

	public EagerMapLiftedFlowSet(Map<IConfigRep, FlowSet> map) {
		this.map = new HashMap<IConfigRep, FlowSet>(map);
	}

	@Override
	public EagerMapLiftedFlowSet clone() {
		Set<Entry<IConfigRep, FlowSet>> entrySet = map.entrySet();
		Map<IConfigRep, FlowSet> newMap = new HashMap<IConfigRep, FlowSet>();
		for (Entry<IConfigRep, FlowSet> entry : entrySet) {
			newMap.put(entry.getKey(), entry.getValue().clone());
		}
		return new EagerMapLiftedFlowSet(newMap);
	}

	@Override
	public void union(FlowSet aOther, FlowSet aDest) {
		EagerMapLiftedFlowSet otherLifted = (EagerMapLiftedFlowSet) aOther;
		EagerMapLiftedFlowSet destLifted = (EagerMapLiftedFlowSet) aDest;

		Set<Entry<IConfigRep, FlowSet>> entrySet = map.entrySet();
		for (Entry<IConfigRep, FlowSet> entry : entrySet) {
			IConfigRep config = entry.getKey();
			FlowSet thisNormal = entry.getValue();

			FlowSet otherNormal = otherLifted.map.get(config);

			ArraySparseSet destNewFlowSet = new ArraySparseSet();
			destLifted.map.put(config, destNewFlowSet);
			thisNormal.union(otherNormal, destNewFlowSet);
		}
	}

	@Override
	public void intersection(FlowSet aOther, FlowSet aDest) {
		EagerMapLiftedFlowSet otherLifted = (EagerMapLiftedFlowSet) aOther;
		EagerMapLiftedFlowSet destLifted = (EagerMapLiftedFlowSet) aDest;

		Set<Entry<IConfigRep, FlowSet>> entrySet = map.entrySet();
		for (Entry<IConfigRep, FlowSet> entry : entrySet) {
			IConfigRep config = entry.getKey();
			FlowSet thisNormal = entry.getValue();

			FlowSet otherNormal = otherLifted.map.get(config);

			ArraySparseSet destNewFlowSet = new ArraySparseSet();
			destLifted.map.put(config, destNewFlowSet);
			thisNormal.intersection(otherNormal, destNewFlowSet);
		}
	}

	public Collection<IConfigRep> getConfigurations() {
		return this.map.keySet();
	}

	public FlowSet getLattice(IConfigRep config) {
		return this.map.get(config);
	}

}
