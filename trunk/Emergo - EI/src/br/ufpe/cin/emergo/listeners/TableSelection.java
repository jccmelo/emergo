package br.ufpe.cin.emergo.listeners;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

public class TableSelection implements ISelectionChangedListener{

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		System.out.println("Table Selection");
		System.out.println(event);
	}

}
