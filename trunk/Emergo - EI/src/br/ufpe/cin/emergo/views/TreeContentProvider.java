package br.ufpe.cin.emergo.views;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class TreeContentProvider extends ArrayContentProvider implements ITreeContentProvider{

	@Override
	public void dispose() {
		System.out.println("Dispose Was Called");
		
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		System.out.println("Beggining of Input Change");
		System.out.println(viewer);
		System.out.println(oldInput);
		System.out.println(newInput);
		System.out.println("END");
		
	}

	@Override
	public Object[] getElements(Object inputElement) {
		System.out.println("getElementsBegin");
		System.out.println(inputElement);
		System.out.println(inputElement.getClass());
		System.out.println("getElementsEnd");
		return ((ArrayList) inputElement).toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if(parentElement instanceof MarkerGrouping)
			return  ((MarkerGrouping) parentElement).getChildren().toArray();
		return null;
	}

	@Override
	public Object getParent(Object element) {
		System.out.println("getParent");
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if(element instanceof MarkerGrouping)
			return  ((MarkerGrouping) element).getChildren().size() > 0;
		return false;
	}

}
