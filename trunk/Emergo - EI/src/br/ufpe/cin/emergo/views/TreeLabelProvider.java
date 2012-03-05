package br.ufpe.cin.emergo.views;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.LabelProvider;

public class TreeLabelProvider extends LabelProvider {
	
	private static final int totalColumns = 4;
	private static final int messageColumn = 1;
	private static final int lineNumberColumn = 2;
	private static final int taskColumn = 3;
	private static final int resourceColumn = 4;
	
	private int currColumn = 0;
	private MarkerGrouping compareGrouping;

	public TreeLabelProvider() {
		super();
	}

	public String getText(Object element) {
		/* TODO: the parametrization of this amount has to be made
		*  It should receive the amount of table columns that exist in the tableView.
		*  Maybe a global constant should solve this problem
		*/
		if (currColumn == totalColumns) {
			currColumn = 0;
		}
		currColumn++;
		if (element instanceof IMarker) { 
			IMarker marker = (IMarker) element;
			// if the marker doesn't exist, it does nothing at all.
			if (!marker.exists())
				return "";
			try {
				 /* 
				  * The switch depends on the amount of columns from the TableView (Not yet parametrizated)
				 * 	TODO: The switch case should work with a mapping that contains the right
				 * 	amount of columns and the what is written in each column.
				 */
				switch (currColumn) {
				case messageColumn:
					/* 
					 * TODO: This requires some explanation 
					 */
					if (compareGrouping.getName().charAt(0) != '('
							&& !compareGrouping.getName().equals("true")) {
						return "";
					}
					return marker.getAttribute(IMarker.MESSAGE).toString();
				case lineNumberColumn:
					return marker.getAttribute(IMarker.LINE_NUMBER).toString();
				case taskColumn:
					return marker.getAttribute(IMarker.TASK).toString();
				case resourceColumn:
					return marker.getResource().getName().toString();
				}
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (element instanceof MarkerGrouping) {
			compareGrouping = (MarkerGrouping) element;
			/*
			 * TODO: This also requires some explanation
			 */
			if (currColumn > 1)
				return "";
		}
		return element.toString();
	}

}