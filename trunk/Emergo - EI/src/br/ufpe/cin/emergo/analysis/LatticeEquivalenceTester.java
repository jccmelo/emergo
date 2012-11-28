package br.ufpe.cin.emergo.analysis;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import soot.Body;
import soot.BodyTransformer;
import soot.Unit;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import br.ufpe.cin.emergo.analysis.reachingdefs.LazyLiftedReachingDefinitions;
import br.ufpe.cin.emergo.analysis.reachingdefs.LiftedReachingDefinitions;
import br.ufpe.cin.emergo.analysis.reachingdefs.ReversedLazyLiftedReachingDefinitions;
import br.ufpe.cin.emergo.analysis.reachingdefs.UnliftedReachingDefinitions;
import br.ufpe.cin.emergo.instrument.EagerConfigTag;
import br.ufpe.cin.emergo.instrument.IConfigRep;
import br.ufpe.cin.emergo.instrument.ILazyConfigRep;
import br.ufpe.cin.emergo.instrument.LazyConfigTag;
import br.ufpe.cin.emergo.instrument.bitrep.BitConfigRep;
import br.ufpe.cin.emergo.instrument.bitrep.BitVectorConfigRep;
import br.ufpe.cin.emergo.util.FixedPoint;
import br.ufpe.cin.emergo.util.FixedPoint.Function;

import com.google.common.collect.BiMap;

public class LatticeEquivalenceTester extends BodyTransformer {
	
	private static Function<FlowSet> mergeFlowSets = new Function<FlowSet>() {
		@Override
		public Collection<FlowSet> apply(Collection<FlowSet> data) {
			Iterator<FlowSet> iterator = data.iterator();
			FlowSet fs1 = iterator.next();
			FlowSet fs2 = iterator.next();
			FlowSet clone = fs1.clone();
			fs1.union(fs2, clone);
			return Collections.singleton(clone);
		}
		
		@Override
		public boolean should(Collection<FlowSet> data) {
			Iterator<FlowSet> iterator = data.iterator();
			FlowSet fs1 = iterator.next();
			FlowSet fs2 = iterator.next();
			return fs1.equals(fs2);
		}
	};

	private static Function<IConfigRep> mergeConfigReps = new Function<IConfigRep>() {
		@Override
		public Collection<IConfigRep> apply(Collection<IConfigRep> data) {
			Iterator<IConfigRep> iterator = data.iterator();
			IConfigRep cr1 = iterator.next();
			IConfigRep cr2 = iterator.next();
			return Collections.singleton(cr1.union(cr2));
		}
		
		@Override
		public boolean should(Collection<IConfigRep> data) {
			Iterator<IConfigRep> iterator = data.iterator();
			IConfigRep rep1 = iterator.next();
			IConfigRep rep2 = iterator.next();
//			return (rep1.equals(rep2) || rep1.intersection(rep2).equals(rep1));	
			return (rep1.equals(rep2));	
		}
	};

	@Override
	protected void internalTransform(Body body, String phase, Map options) {
		UnitGraph bodyGraph = new BriefUnitGraph(body);
		EagerConfigTag eagerConfigTag = (EagerConfigTag) body.getTag(EagerConfigTag.TAG_NAME);
		LazyConfigTag lazyConfigTag = (LazyConfigTag) body.getTag(LazyConfigTag.TAG_NAME);
		
		if (eagerConfigTag == null) {
			throw new IllegalStateException("No EagerConfigTag found on the body of method " + body.getMethod());
		}
		
		if (lazyConfigTag == null) {
			throw new IllegalStateException("No LazyConfigTag found on the body of method " + body.getMethod());
		}
		
		ILazyConfigRep lazyConfig = lazyConfigTag.getLazyConfig();
		
		if (lazyConfig.size() <= 0) {
			// TODO log this instead
			System.err.println("Method " + body.getMethod() + " has " + lazyConfig.size() + " configurations");
			return;
		}
		
		Map<IConfigRep,  ForwardFlowAnalysis<Unit, FlowSet>> consecutiveAnalyses = new HashMap<IConfigRep,  ForwardFlowAnalysis<Unit, FlowSet>>();
		Set<IConfigRep> configReps = eagerConfigTag.getConfigReps();
		
//		for (IConfigRep config : configReps) {
//			consecutiveAnalyses.put(config, new UnliftedUnitializedVariablesAnalysis(bodyGraph, config));
//		}
//		LiftedUninitializedVariableAnalysis liftedUV = new LiftedUninitializedVariableAnalysis(bodyGraph, configReps);
//		checkConsecutiveAndSimultaneousEquivalence(body, consecutiveAnalyses, liftedUV);		
//		consecutiveAnalyses.clear();
		
		for (IConfigRep config : configReps) {
			consecutiveAnalyses.put(config, new UnliftedReachingDefinitions(bodyGraph, config));
		}
		LiftedReachingDefinitions liftedRD = new LiftedReachingDefinitions(bodyGraph, configReps);
		checkConsecutiveAndSimultaneousEquivalence(body, consecutiveAnalyses, liftedRD);
		
//		LazyLiftedUninitializedVariableAnalysis lazyUV = new LazyLiftedUninitializedVariableAnalysis(bodyGraph, lazyConfig);
//		checkSimultaneousAndLazyEquivalence(body, liftedUV, lazyUV);
		
		LazyLiftedReachingDefinitions lazyRD = new LazyLiftedReachingDefinitions(bodyGraph, lazyConfig);
		checkSimultaneousAndLazyEquivalence(body, liftedRD, lazyRD);
		
		ReversedLazyLiftedReachingDefinitions reversedRD = new ReversedLazyLiftedReachingDefinitions(bodyGraph, lazyConfig);
		checkLazyAndReversedEquivalence(body, lazyRD, reversedRD);
		
//		ReversedLazyLiftedUninitializedVariables reversedUV = new ReversedLazyLiftedUninitializedVariables(bodyGraph, lazyConfig);
//		checkLazyAndReversedEquivalence(body, lazyUV, reversedUV);
	}

	/**
	 * Checks that the lattices from a simultaneous analysis is equivalent to
	 * the lattices of a lazy shared analysis.
	 * 
	 * For instance:
	 * 
	 * lazy = {A^B => l1}
	 * 
	 * simu = {A => l1, B = l1}
	 * 
	 * Then lazy.equals(simu)
	 * 
	 * @param body
	 * @param liftedAnalysis
	 * @param lazyAnalysis
	 */
	private void checkSimultaneousAndLazyEquivalence(
			Body body,
			ForwardFlowAnalysis<Unit, EagerMapLiftedFlowSet> liftedAnalysis,
			ForwardFlowAnalysis<Unit, LazyMapLiftedFlowSet> lazyAnalysis) {
		
		for (Unit unit : body.getUnits()) {
			EagerMapLiftedFlowSet simultaneousLattice = liftedAnalysis.getFlowAfter(unit);
			Map<IConfigRep, FlowSet> simultaneousMapping = simultaneousLattice.getMapping();
			
			Map<IConfigRep, FlowSet> mergedSimultaneousMapping = replaceWithLazyConfigReps(simultaneousMapping);
			mergedSimultaneousMapping = FixedPoint.mapMerge(mergedSimultaneousMapping, LatticeEquivalenceTester.mergeConfigReps, LatticeEquivalenceTester.mergeFlowSets);
			LazyMapLiftedFlowSet mergedSimultaneousFlowSet = new LazyMapLiftedFlowSet(mergedSimultaneousMapping);
			
			LazyMapLiftedFlowSet lazyLattice = lazyAnalysis.getFlowAfter(unit);
			Map<IConfigRep, FlowSet> lazyMapping = lazyLattice.getMapping();
			Map<IConfigRep, FlowSet> mergedLazyMapping = FixedPoint.mapMerge(lazyMapping, LatticeEquivalenceTester.mergeConfigReps, LatticeEquivalenceTester.mergeFlowSets);
			LazyMapLiftedFlowSet mergedLazyFlowSet = new LazyMapLiftedFlowSet(mergedLazyMapping);
			
			if (!mergedSimultaneousFlowSet.equals(mergedLazyFlowSet)) {
				System.err.println("Merged A3 lattice for unit " + unit + " on method " 
						+ body.getMethod() + " is inequivalent to the respective A4 lattice:\nA3:"
						+ simultaneousLattice + "\nmerged A3: " 
						+ mergedSimultaneousFlowSet + "\nA4: " + lazyLattice + "\nmerged A4:"
						+ mergedLazyFlowSet);
			throw new RuntimeException();
			}
		}
	}

	private Map<IConfigRep, FlowSet> replaceWithLazyConfigReps(Map<IConfigRep, FlowSet> simultaneousMapping) {
		HashMap<IConfigRep, FlowSet> map = new HashMap<IConfigRep, FlowSet>();
		Set<Entry<IConfigRep,FlowSet>> entrySet = simultaneousMapping.entrySet();
		for (Entry<IConfigRep, FlowSet> entry : entrySet) {
			BitConfigRep key = (BitConfigRep) entry.getKey();
			map.put(BitVectorConfigRep.convert(key, key.getAtoms()), entry.getValue());
		}
		return map;
	}

	/**
	 * Checks that the lattices from a set of consecutive analyses is equivalent
	 * to the lattices of a single simultaneous analysis.
	 * 
	 * This is done by:
	 * 
	 * Taking every lattice for a single unit from all the consecutive analysis
	 * and merging it into a single simultaneous lattice.
	 * 
	 * Checking if the merged lattices equals the simultaneous one.
	 * 
	 * @param body
	 * @param configReps
	 * @param consecutiveAnalyses
	 * @param liftedAnalysis
	 */
	private void checkConsecutiveAndSimultaneousEquivalence(
			Body body,
			Map<IConfigRep, ForwardFlowAnalysis<Unit, FlowSet>> consecutiveAnalyses,
			ForwardFlowAnalysis<Unit, EagerMapLiftedFlowSet> liftedAnalysis) {
		for (Unit unit : body.getUnits()) {
			EagerMapLiftedFlowSet accLiftedFlowSet = new EagerMapLiftedFlowSet(consecutiveAnalyses.keySet());
			Map<IConfigRep, FlowSet> mapping = accLiftedFlowSet.getMapping();
			for (Entry<IConfigRep, ForwardFlowAnalysis<Unit, FlowSet>> entry : consecutiveAnalyses.entrySet()) {
				ForwardFlowAnalysis<Unit, FlowSet> analysis = entry.getValue();
				FlowSet flowAfter = analysis.getFlowAfter(unit);
				if (mapping.containsKey(entry.getKey())) {
					Iterator iterator = flowAfter.iterator();
					while (iterator.hasNext()) {
						Object object = (Object) iterator.next();
						mapping.get(entry.getKey()).add(object);
					}
				}
			}

			EagerMapLiftedFlowSet flowAfter = liftedAnalysis.getFlowAfter(unit);
			if (!liftedAnalysis.getFlowAfter(unit).equals(accLiftedFlowSet)) {
					System.err.println("Accumulated A2 lattice for unit " + unit + " on method " 
							+ body.getMethod() + " is inequivalent to the respective A3 lattice:\nacc A2: " 
							+ accLiftedFlowSet + "\nA3: " + flowAfter);
				throw new RuntimeException();
			}
		}
	}
	
	private void checkLazyAndReversedEquivalence(
			Body body,
			ForwardFlowAnalysis<Unit, LazyMapLiftedFlowSet> lazyAnalysis,
			ForwardFlowAnalysis<Unit, ReversedMapLiftedFlowSet> reversedAnalysis) {
		
		for (Unit unit : body.getUnits()) {
			LazyMapLiftedFlowSet lazyAfter = lazyAnalysis.getFlowAfter(unit);
			Map<IConfigRep, FlowSet> lazyMapping = flattenMap(lazyAfter.getMapping());
			
			ReversedMapLiftedFlowSet reversedAfter = reversedAnalysis.getFlowAfter(unit);
			BiMap<FlowSet, IConfigRep> reversedMapping = reversedAfter.getMapping();
			Map<IConfigRep, FlowSet> inversedReversedMapping = flattenMap(reversedMapping.inverse());
			
			if (!lazyMapping.equals(inversedReversedMapping)) {
				throw new RuntimeException("Lazy mapping:\n" + lazyAfter + "\n\nReversed mapping:\n" + reversedMapping + "\n\nInversed reversed mapping:\n" + inversedReversedMapping);
			} 
		}
	}

	// TODO: move this method to the *MapLiftedFlowSets.
	private Map<IConfigRep, FlowSet> flattenMap(Map<IConfigRep, FlowSet> map) {
		Map<IConfigRep, FlowSet> result = new HashMap<IConfigRep, FlowSet>();

		Set<Entry<IConfigRep, FlowSet>> entrySet = map.entrySet();
		for (Entry<IConfigRep ,FlowSet> entry : entrySet) {
			IConfigRep configs = entry.getKey();
			FlowSet flowSet = entry.getValue();
			for (IConfigRep config : configs) {
				FlowSet resultFlowSet = result.get(config);
				if (resultFlowSet == null) {
					result.put(config, flowSet);
				} else {
					FlowSet union = (FlowSet) flowSet.emptySet();
					flowSet.union(resultFlowSet, union);
					result.put(config, union);
				}
			}
		}

		return result;
	}
	
}


