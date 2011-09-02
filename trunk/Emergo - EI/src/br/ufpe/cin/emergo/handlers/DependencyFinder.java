package br.ufpe.cin.emergo.handlers;

import java.io.FileNotFoundException;
import java.util.Map;

public class DependencyFinder {

	public static void findFromSelection(DependencyFinderID finder, SelectionPosition selectionPosition, Map<Object, Object> options) throws FileNotFoundException {
		switch (finder) {
		case JWCOMPILER:
			new JWCompilerDependencyFinder(selectionPosition, options);
			break;
		case SOOT:
		default:
			throw new UnsupportedOperationException("Depency finder unavailable");
		}
	}
}
