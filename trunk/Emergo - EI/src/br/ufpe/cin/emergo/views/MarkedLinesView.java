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
import org.eclipse.jface.viewers.IContentProvider;
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
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.ViewPart;

import br.ufpe.cin.emergo.handlers.ChooseLines;

public class MarkedLinesView extends ViewPart{
	public static final String ID = "br.ufpe.cin.emergo.views.MarkedLinesView";
	private TreeViewer viewer;
	private List<LineOfCode> baseLines;
	private MarkedLinesAction lCustomAction;
	Action deleteItemAction, deleteAllAction;
	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		viewer = new TreeViewer(parent, SWT.FULL_SELECTION);
		if(baseLines == null)
			baseLines = new ArrayList<LineOfCode>();
		List<Integer> lineNumbers = new ArrayList<Integer>();
		viewer.setContentProvider(new ITreeContentProvider() {
			
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {	
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
				System.out.println("input Element");
				return ((ArrayList) inputElement).toArray();
			}
			
			@Override
			public Object[] getChildren(Object parentElement) {
				return null;
			}
		});
		createAction();
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
                    	System.out.println("DELETE ADD");
                    	System.out.println(viewer.getSelection());
                    	ISelection selection = viewer.getSelection();
                    	String auxSelectionText = selection.toString().substring(1);
                    	auxSelectionText = auxSelectionText.substring(0, auxSelectionText.length()-1);
                    	String[] strings = auxSelectionText.split(";");
                    	for (int i = 0; i < strings.length; i++) {
                    		System.out.println(strings[i]);							
						}
                    	String lineText = "";
                    	boolean begin = false;
                    	for (int j = 0; j < strings[0].length(); j++) {
                    		if(begin){
                    			lineText = lineText+ strings[0].charAt(j);
                    			
                    		}
                    		System.out.println(strings[0].charAt(j));
							if(strings[0].charAt(j)=='('){
								begin = true;
								System.out.println("Begun");
							}
						}
                    	lineText= lineText+";";
                    	System.out.println(lineText);
                    	TestView.deleteMarkers(lineText);
                    	ChooseLines.deleteMarkers(auxSelectionText);
                    	Object[] baseLinesClone =  baseLines.toArray();
                    	for (int i = 0; i < baseLinesClone.length; i++) {
                    		if(((LineOfCode)baseLinesClone[i]).getSelection().trim().equals(lineText)){
                    			baseLines.remove(i);
                    			
                    		}
						}
                    	actualisate(new ArrayList<LineOfCode>());
                    }
            };
            deleteAllAction = new Action("Delete All") {
                    public void run() {
                    	baseLines= new ArrayList<LineOfCode>();
                    	actualisate(new ArrayList<LineOfCode>());
                    	TestView.deleteAllMarkers();
                    	ChooseLines.deleteAllMarkers();
                    }
            };
            
            // Add selection listener.
         viewer.addSelectionChangedListener(new ISelectionChangedListener() {
                    public void selectionChanged(SelectionChangedEvent event) {
                           System.out.println("UPDATE ACTION ELEMENT!!");
                    }
            });
    }
	
	public void createAction(){
		lCustomAction = new MarkedLinesAction();  
		lCustomAction.setText("Open Dialog Box");  
		getViewSite().getActionBars().getMenuManager().add(lCustomAction);  
		getViewSite().getActionBars().getMenuManager().add(new Separator()); //Add a horizontal separator  
//		getViewSite().getActionBars().getMenuManager().add(lCustomAction);  
//		getViewSite().getActionBars().getMenuManager().add(lCustomAction);  
	}
	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		
	}

	public void actualisate(List<LineOfCode> linesToInser) {
		for (LineOfCode lineOfCode : linesToInser) {
			
		}
		for (LineOfCode lineOfCode : linesToInser) {
			boolean insert = true;
			for (LineOfCode baseLine : baseLines) {
				if(lineOfCode.equals(baseLine)){
					insert=false;
				}
			}
			if(insert){
				baseLines.add(lineOfCode);
			}
			this.lCustomAction.setLinesOfCode(this.baseLines);
		}
		
		Collections.sort(baseLines, new LineComparator());
		
		viewer.setInput(baseLines);
		viewer.refresh();
	}
	
	private class LineComparator implements Comparator<LineOfCode>{
		@Override
		public int compare(LineOfCode arg0, LineOfCode arg1) {
			return arg0.getLine() - arg1.getLine();
		}
		
	}
}
