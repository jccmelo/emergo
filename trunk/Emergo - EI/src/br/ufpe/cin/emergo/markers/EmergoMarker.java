package br.ufpe.cin.emergo.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

/*
 * XXX: Rename to a more meaningful name. This name suggests that instances
 * of this class represent the actual IMarker, and that is not true.
 * 
 * Suggestion: EmergoMarkerFactory
 */
public class EmergoMarker {

	public static final String EMERGO_MARKER_ID = "Emergo.emergomarker";

	public static IMarker createMarker(FeatureDependency fd) {
		try {
			IMarker marker = fd.getFile().createMarker(EMERGO_MARKER_ID);
			marker.setAttribute(IMarker.MESSAGE, fd.getMessage());
			marker.setAttribute(IMarker.LINE_NUMBER, fd.getLineNumber());
			marker.setAttribute(IMarker.TEXT, fd.getConfiguration());
			marker.setAttribute(IMarker.TASK, fd.getFeature());
			return marker;
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}
}