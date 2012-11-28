package br.ufpe.cin.emergo.instrument;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class LazyConfigTag implements Tag, ISizeable {

	public static final String TAG_NAME = LazyConfigTag.class.getSimpleName();
	private final ILazyConfigRep lazyConfig;

	public LazyConfigTag(ILazyConfigRep lazyConfig) {
		this.lazyConfig = lazyConfig;
	}

	public ILazyConfigRep getLazyConfig() {
		return lazyConfig;
	}

	@Override
	public String getName() {
		return TAG_NAME;
	}

	@Override
	public byte[] getValue() throws AttributeValueException {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public long getSize() {
		return lazyConfig.size();
	}

}