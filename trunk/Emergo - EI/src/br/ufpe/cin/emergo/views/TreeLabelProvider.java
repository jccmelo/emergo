package br.ufpe.cin.emergo.views;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.LabelProvider;

public class TreeLabelProvider extends LabelProvider{

	private int columnCount = 0;
	private MarkerGrouping compareGrouping;
	
	public TreeLabelProvider() {
		super();
	}
	public String getText(Object element){
		if(columnCount ==5){
			columnCount=0;
		}
		columnCount++;
		if(element instanceof IMarker && ((IMarker)element).exists()){ // if the marker doesnt exist, it does nothing at all.
			try {
				switch (columnCount){
					case 1:
						if(compareGrouping.getName().charAt(0)!='(' && !compareGrouping.getName().equals("true"))
							return "";
						return ((IMarker)element).getAttribute(IMarker.MESSAGE).toString();
					case 2:
						return ((IMarker)element).getAttribute(IMarker.TEXT).toString();
					case 3:
						return ((IMarker)element).getAttribute(IMarker.LINE_NUMBER).toString();
					case 4:
						String message = ((IMarker)element).getAttribute(IMarker.TASK).toString();
						if(message.equals("true"))
							message = "None";
						return message;
					case 5:
						return ((IMarker)element).getResource().getName().toString();
				}
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if(element instanceof MarkerGrouping){
			compareGrouping= (MarkerGrouping) element;
			if(columnCount>1)
				return "";
		}
		return element.toString();
	}
}
