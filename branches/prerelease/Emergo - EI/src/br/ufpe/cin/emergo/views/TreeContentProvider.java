package br.ufpe.cin.emergo.views;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class TreeContentProvider extends ArrayContentProvider implements
		ITreeContentProvider {

	@Override
	public void dispose(){}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput){}

	@Override
	public Object[] getElements(Object inputElement) {
		return ((ArrayList) inputElement).toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof MarkerGrouping)
			return ((MarkerGrouping) parentElement).getChildren().toArray();
		return null;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof MarkerGrouping)
			return ((MarkerGrouping) element).getChildren().size() > 0;
		return false;
	}

}