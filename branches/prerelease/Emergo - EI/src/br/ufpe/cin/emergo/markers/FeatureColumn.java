package br.ufpe.cin.emergo.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.views.markers.MarkerField;
import org.eclipse.ui.views.markers.MarkerItem;

public class FeatureColumn extends MarkerField {

	@Override
	public String getValue(MarkerItem item) {
		return item.getAttributeValue(IMarker.TASK, "");
	}

}