package br.ufpe.cin.emergo.listeners;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

public class TableSelection implements ISelectionChangedListener{

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		System.out.println("Table Selection");
		System.out.println(event);
	}

}