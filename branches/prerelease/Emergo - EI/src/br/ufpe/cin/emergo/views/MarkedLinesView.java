package br.ufpe.cin.emergo.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.part.ViewPart;

import br.ufpe.cin.emergo.handlers.SelectLinesHandler;

public class MarkedLinesView extends ViewPart {
	
	public static final String ID = "br.ufpe.cin.emergo.views.MarkedLinesView";
	private TreeViewer viewer;
	private List<LineOfCode> baseLines;
	Action deleteItemAction, deleteAllAction;

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		viewer = new TreeViewer(parent, SWT.FULL_SELECTION);
		if (baseLines == null)
			baseLines = new ArrayList<LineOfCode>();
		viewer.setContentProvider(new ITreeContentProvider() {

			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
			}

			@Override
			public void dispose() {
			}

			@Override
			public boolean hasChildren(Object element) {
				return false;
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public Object[] getElements(Object inputElement) {
				return ((ArrayList) inputElement).toArray();
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				return null;
			}
		});
		createActions();
		createContextMenu();

	}

	private void createContextMenu() {
		// Create menu manager.
		MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager mgr) {
				fillContextMenu(mgr);
			}

			private void fillContextMenu(IMenuManager mgr) {
				mgr.add(deleteItemAction);
				mgr.add(new Separator());
				mgr.add(deleteAllAction);
			}
		});
		// Create menu.
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		// Register menu for extension.
		getSite().registerContextMenu(menuMgr, viewer);
	}

	public void createActions() {
		deleteItemAction = new Action("Delete") {
			public void run() {
				ISelection selection = viewer.getSelection();
				String auxSelectionText = selection.toString().substring(1);
				auxSelectionText = auxSelectionText.substring(0,
						auxSelectionText.length() - 1);
				String[] strings = auxSelectionText.split(";");
				String lineText = "";
				boolean begin = false;
				for (int j = 0; j < strings[0].length(); j++) {
					if (begin) {
						lineText = lineText + strings[0].charAt(j);

					}
					if (strings[0].charAt(j) == '(') {
						begin = true;
					}
				}
				lineText = lineText + ";";
//				EmergoView.deleteMarkers(lineText);
				SelectLinesHandler.deleteMarkers(auxSelectionText);
				Object[] baseLinesClone = baseLines.toArray();
				for (int i = 0; i < baseLinesClone.length; i++) {
					if (((LineOfCode) baseLinesClone[i]).getSelection().trim()
							.equals(lineText)) {
						baseLines.remove(i);

					}
				}
				update(new ArrayList<LineOfCode>());
			}
		};
		deleteAllAction = new Action("Delete All") {
			public void run() {
				baseLines = new ArrayList<LineOfCode>();
				update(new ArrayList<LineOfCode>());
//				EmergoView.deleteAllMarkers();
				SelectLinesHandler.deleteAllMarkers();
			}
		};
		// Add selection listener.
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				System.out.println("UPDATE ACTION ELEMENT!!");
			}
		});
	}


	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}

	public void update(List<LineOfCode> linesToInser) {
		for (LineOfCode lineOfCode : linesToInser) {
			boolean insert = true;
			for (LineOfCode baseLine : baseLines) {
				if (lineOfCode.equals(baseLine)) {
					insert = false;
				}
			}
			if (insert) {
				baseLines.add(lineOfCode);
			}
		}
		Collections.sort(baseLines, new LineComparator());
		viewer.setInput(baseLines);
		viewer.refresh();
	}

	private class LineComparator implements Comparator<LineOfCode> {
		@Override
		public int compare(LineOfCode arg0, LineOfCode arg1) {
			return arg0.getLine() - arg1.getLine();
		}
	}

}