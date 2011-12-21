package br.ufpe.cin.emergo.views;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;

public class TreeSelectionManager implements ITreeSelection{

	@Override
	public boolean isEmpty() {
		System.out.println("Called Is Empty selectin");
		return false;
	}

	@Override
	public Object getFirstElement() {
		System.out.println("FirstElement");
		return null;
	}

	@Override
	public Iterator iterator() {
		System.out.println("Iterator");
		return null;
	}

	@Override
	public int size() {
		System.out.println("Size");
		return 0;
	}

	@Override
	public Object[] toArray() {
		System.out.println("toArray");
		return null;
	}

	@Override
	public List toList() {
		System.out.println("To List");
		return null;
	}

	@Override
	public TreePath[] getPaths() {
		System.out.println("GetPathts");
		return null;
	}

	@Override
	public TreePath[] getPathsFor(Object element) {
		System.out.println("GetPathsFor: "+element);
		return null;
	}

}
