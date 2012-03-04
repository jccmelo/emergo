package br.ufpe.cin.emergo.views;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.LabelProvider;

public class TreeLabelProvider extends LabelProvider {

	private int columnCount = 0;
	private MarkerGrouping compareGrouping;

	public TreeLabelProvider() {
		super();
	}

	public String getText(Object element) {
		/* TODO: the parametrization of this amount has to be made
		*  It should receive the amount of table columns that exist in the tableView.
		*  Maybe a global constant should solve this problem
		*/
		if (columnCount == 4) { // antes tinha um 5 aqui
			columnCount = 0;
		}
		columnCount++;
		if (element instanceof IMarker && ((IMarker) element).exists()) { // if the marker doesn't exist, it does nothing at all.
			try {
				/*	The switch depends on the amount of columns from the TableView (Not yet parametrizated)
				* 	TODO: The switch case should work with a mapping that contains the right
				* 	amount of columns and the what is written in each column.
				*/
				switch (columnCount) {
				case 1:
					if (compareGrouping.getName().charAt(0) != '('
							&& !compareGrouping.getName().equals("true")) {
						return "";
					}
					return ((IMarker) element).getAttribute(IMarker.MESSAGE).toString();
				//case 2:
				//	return ((IMarker) element).getAttribute(IMarker.TEXT).toString();
				case 2://case 3:
					return ((IMarker) element).getAttribute(IMarker.LINE_NUMBER).toString();
				case 3://case 4:
					String message = ((IMarker) element).getAttribute(IMarker.TASK).toString();
					if (message.equals("true"))
						message = "None";
					return message;
				case 4: //case 5:
					return ((IMarker) element).getResource().getName().toString();
				}
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (element instanceof MarkerGrouping) {
			compareGrouping = (MarkerGrouping) element;
			if (columnCount > 1)
				return "";
		}
		return element.toString();
	}

}