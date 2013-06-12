package br.ufpe.cin.emergo.instrument;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.collections.bidimap.UnmodifiableBidiMap;

import soot.Body;
import soot.BodyTransformer;
import soot.PatchingChain;
import soot.Unit;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceLineNumberTag;
import soot.tagkit.SourceLnPosTag;
import soot.tagkit.Tag;
import br.ufpe.cin.emergo.features.FeatureSetChecker;
import br.ufpe.cin.emergo.instrument.bitrep.BitConfigRep;
import br.ufpe.cin.emergo.instrument.bitrep.BitVectorConfigRep;
import br.ufpe.cin.emergo.instrument.bitrep.BitVectorFeatureRep;
import br.ufpe.cin.emergo.preprocessor.ContextManager;

public class FeatureInstrumentor extends BodyTransformer {
	
	private DualHashBidiMap emptyVectorFeatureMap;
	
	protected FeatureSetChecker fmChecker;
	
	private Map options = new HashMap();
	
	// TODO: implement reset method for the singleton instance.
	
	public FeatureInstrumentor(Map options){
		this.options = options;
	}
	
	/**
	 * A (constant) IFeatureRep representing the absence of features.
	 * 
	 * @return
	 */
	protected IFeatureRep emptyFeatureRep() {
		this.emptyVectorFeatureMap = new DualHashBidiMap();
		return new BitVectorFeatureRep(Collections.<String>emptySet(), emptyVectorFeatureMap);
	}
	
	/**
	 * Return an IFeatureRep that represents the set {@code features}.
	 * enabled in the {@code context}
	 * 
	 * @param features enabled features
	 * @param context context that containst the features
	 * @return the IFeatureRep
	 */
	protected IFeatureRep featureRep(Set<String> features, BidiMap context) {
		return new BitVectorFeatureRep(features, context);
	}

	@Override
	/**
	 * Iterate over all units, look up for their colors and add a new FeatureTag to each of them, and also compute
	 * all the colors found in the whole body. Units with no colors receive an empty FeatureTag.
	 */
	protected void internalTransform(Body body, String phase, Map options) {
		//preTransform(body);
		IFeatureRep emptyFeatureRep = emptyFeatureRep();
		FeatureTag emptyFeatureTag = new FeatureTag(emptyFeatureRep);

		
		Iterator<Unit> unitIt = body.getUnits().iterator();

		// XXX will break when there are more than 32 in a method. use a bitvector?
		// String->Integer
		BidiMap allFeaturesSoFar = new DualHashBidiMap();

		int idGen = 1;
		while (unitIt.hasNext()) {
			Unit nextUnit = unitIt.next();
			Object lineTag = (LineNumberTag) nextUnit.getTag("LineNumberTag"); //SourceLnPosTag
			
			String fileExt = (String) this.options.get("fileExtension");
			if(fileExt.equals("java")){
				lineTag = (SourceLnPosTag) nextUnit.getTag("SourceLnPosTag");
			}
			
			if (lineTag == null) {
				nextUnit.addTag(emptyFeatureTag);
			} else {
				int unitLine;
				if(fileExt.equals("java")){
					unitLine = ((SourceLnPosTag)lineTag).startLn();
				} else {
					unitLine = ((LineNumberTag)lineTag).getLineNumber();
				}
//				int unitLine = lineTag.getLineNumber(); // lineTag.startLn();
				Set<String> nextUnitFeatures = (Set<String>) ContextManager.getContext().getMap().get(unitLine);
//				Set<String> nextUnitFeatures = currentColorMap.get(unitLine);
				
				if (nextUnitFeatures != null) {
					for (String featureName : nextUnitFeatures) {
						if (!allFeaturesSoFar.containsKey(featureName)) {
							allFeaturesSoFar.put(featureName, idGen);
							idGen = idGen << 1;
						}
					}

					IFeatureRep featureRep = featureRep(nextUnitFeatures, allFeaturesSoFar);
					nextUnit.addTag(new FeatureTag(featureRep));
				} else {
					nextUnit.addTag(emptyFeatureTag);
				}
			}
		}
		
		UnmodifiableBidiMap unmodAllPresentFeaturesId = (UnmodifiableBidiMap) UnmodifiableBidiMap.decorate(allFeaturesSoFar);
		
		emptyVectorFeatureMap.putAll(allFeaturesSoFar);
		if (emptyFeatureRep instanceof BitVectorFeatureRep) {
			((BitVectorFeatureRep) emptyFeatureRep).computeBitVector();
		}
		
		endBodyInstrumentation(body, phase, options, unmodAllPresentFeaturesId);
	
	}

	/**
	 * Called(back) at the end of the instrumentation process.
	 * 
	 * @param featuresId a String -> Integer mapping
	 * @param body 
	 */
	protected void endBodyInstrumentation(Body body, String phase, Map options, UnmodifiableBidiMap featuresId) {
		Integer highestId;
        if (!featuresId.isEmpty()) {
            highestId = (Integer) Collections.max(featuresId.values()) << 1;
        } else {
            highestId = 1;
        }

        PatchingChain<Unit> units = body.getUnits();
        for (Unit unit : units) {
            FeatureTag tag = (FeatureTag) unit.getTag(FeatureTag.FEAT_TAG_NAME);
            BitVectorFeatureRep bitVectorFeatureRep = (BitVectorFeatureRep) tag.getFeatureRep();
            bitVectorFeatureRep.computeBitVector();
        }

        /* Generates power set of the local configurations */
        
        BitVectorConfigRep localConfigurations;
        EagerConfigTag eagerConfigTag;
//        Boolean useFeatureModel = (Boolean) options.get(Constants.USE_FEATURE_MODEL);
//        if (useFeatureModel) {
//            localConfigurations = BitVectorConfigRep.localConfigurations(highestId, featuresId, this.fmChecker);
//            eagerConfigTag = new EagerConfigTag(BitConfigRep.localConfigurations(highestId, featuresId, this.fmChecker).getConfigs());
//        } else {
            eagerConfigTag = new EagerConfigTag(BitConfigRep.localConfigurations(highestId, featuresId).getConfigs());
            localConfigurations = BitVectorConfigRep.localConfigurations(highestId, featuresId);
//        }
        
        LazyConfigTag lazyConfigTag = new LazyConfigTag(localConfigurations);
        body.addTag(lazyConfigTag);
        body.addTag(eagerConfigTag);
	}
	
	/**
	 * This method gets the line number from an unit
	 * 
	 * @param u - unit
	 * @return line number
	 */
	private int getLineNumberForUnit(Unit u) {
		List tags = u.getTags();
		int ln = -1;
		Iterator it = tags.iterator();
		
		while (it.hasNext() && ln == -1) {
			Tag tag = (Tag) it.next();

			if (tag instanceof LineNumberTag) {
				byte[] value = tag.getValue();
				ln = ((value[0] & 0xff) << 8) | (value[1] & 0xff);
			} else if (tag instanceof SourceLnPosTag) {
				ln = ((SourceLnPosTag) tag).startLn();
			} else if (tag instanceof SourceLineNumberTag) {
				ln = ((SourceLineNumberTag) tag).getLineNumber();
			}
		}

		return ln;
	}

}
