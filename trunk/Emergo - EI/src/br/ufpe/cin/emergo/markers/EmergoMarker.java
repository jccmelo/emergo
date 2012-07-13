package br.ufpe.cin.emergo.markers;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/*
 * XXX: Rename to a more meaningful name. This name suggests that instances
 * of this class represent the actual IMarker, and that is not true.
 * 
 * Suggestion: EmergoMarkerFactory
 */
public class EmergoMarker {
	
	private static Set<MarkerContentWrapper> markers = new HashSet<MarkerContentWrapper>();

	public static final String EMERGO_MARKER_ID = "Emergo.emergomarker";

	public static IMarker createMarker(FeatureDependency fd) {
		try {
			IMarker marker = fd.getFile().createMarker(EMERGO_MARKER_ID);
			marker.setAttribute(IMarker.MESSAGE, fd.getMessage());
			//System.out.println(fd.getMessage());
			marker.setAttribute(IMarker.LINE_NUMBER, fd.getLineNumber());
			//System.out.println(fd.getLineNumber());
			//System.out.println(marker.getAttribute(IMarker.LINE_NUMBER));
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
	
	public static void clearMarkers(IFile file) {
		try {
			file.deleteMarkers(EmergoMarker.EMERGO_MARKER_ID, true, IResource.DEPTH_INFINITE);
			markers.clear();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	private static class MarkerContentWrapper {
		String msg;
		FeatureDependency fd;

		public MarkerContentWrapper(String msg, FeatureDependency fd) {
			this.msg = msg;
			this.fd = fd;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fd == null) ? 0 : fd.hashCode());
			result = prime * result + ((msg == null) ? 0 : msg.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MarkerContentWrapper other = (MarkerContentWrapper) obj;
			if (fd == null) {
				if (other.fd != null)
					return false;
			} else if (!fd.equals(other.fd))
				return false;
			if (msg == null) {
				if (other.msg != null)
					return false;
			} else if (!msg.equals(other.msg))
				return false;
			return true;
		}
	}
}