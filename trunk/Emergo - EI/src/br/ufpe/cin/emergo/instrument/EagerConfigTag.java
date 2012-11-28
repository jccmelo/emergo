package br.ufpe.cin.emergo.instrument;

import java.util.Set;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class EagerConfigTag implements Tag, ISizeable {
	/** The Constant CONFIG_TAG_NAME. */
    public static final String TAG_NAME = EagerConfigTag.class.getSimpleName();
    private Set<IConfigRep> reps;

    public EagerConfigTag(Set<IConfigRep> localConfigs) {
        this.reps = localConfigs;
    }

    @Override
    public String getName() {
        return EagerConfigTag.TAG_NAME;
    }

    @Override
    public byte[] getValue() throws AttributeValueException {
        return null;
    }

    public Set<IConfigRep> getConfigReps() {
        return this.reps;
    }

    public int size(){
        return reps.size();
    }

    @Override
    public long getSize() {
        return reps.size();
    }

}
