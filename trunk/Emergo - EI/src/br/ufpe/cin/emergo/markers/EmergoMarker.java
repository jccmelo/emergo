package br.ufpe.cin.emergo.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import br.ufpe.cin.emergo.activator.Activator;

public class EmergoMarker {

	public static final String EMERGO_MARKER_ID = Activator.PLUGIN_ID + ".emergomarker";

	public static void createMarker(String message, FeatureDependency fd) {
		try {
			IMarker marker = fd.getFile().createMarker(EMERGO_MARKER_ID);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.LINE_NUMBER, fd.getLineNumber());
			marker.setAttribute(IMarker.TEXT, fd.getConfiguration());
			marker.setAttribute(IMarker.TASK, fd.getFeature());
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

}