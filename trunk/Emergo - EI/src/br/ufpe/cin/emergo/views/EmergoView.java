package br.ufpe.cin.emergo.views;

import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.markers.MarkerField;
import org.jgrapht.DirectedGraph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.KShortestPaths;

import br.ufpe.cin.emergo.activator.Activator;
import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.graph.DependencyNode;
import br.ufpe.cin.emergo.graph.ValueContainerEdge;
import br.ufpe.cin.emergo.markers.EmergoMarker;
import br.ufpe.cin.emergo.markers.FeatureDependency;
import br.ufpe.cin.emergo.util.ResourceUtil;

public class EmergoView extends ViewPart{
	public static final String ID = "br.ufpe.cin.emergo.views.EmergoView";
	private TreeViewer viewer;
	private static final int MAX_PATHS = 16;
	private static List<IFile> selectedFiles;
	private static List<IMarker> test;
	public static final String EMERGO_MARKER_ID = Activator.PLUGIN_ID + ".emergomarker";
	private static FeatureDependency fd;
	private static final String MARKER_FIELD = "MARKER_FIELD"; //$NON-NLS-1$
	
	TreeViewerColumn tc;
	TreeViewerColumn tc2;
	TreeViewerColumn tc3;
	TreeViewerColumn tc4;
	TreeViewerColumn tc5;
	
	private static String textColumnOne = "Description";
	private static String textColumnTwo = "Configuration";
	private static String textColumnThree = "Location";
	private static String textColumnFour = "Feature";
	private static String textColumnFive = "Resource";
	private static String sortingType = IMarker.TASK;
	private int auxiliary=0;
	
	Action addItemAction, deleteItemAction, selectAllAction;
	
	public EmergoView() {
		// TODO Auto-generated constructor stub
		selectedFiles = new ArrayList<IFile>();
	}
	
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		viewer = new TreeViewer(parent, SWT.FULL_SELECTION);
		viewer.getTree().setLinesVisible(true);
		viewer.setSelection(new TreeSelection());
		viewer.getTree().addListener(SWT.MouseDown, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				 Point point = new Point (event.x, event.y);
			        TreeItem clickedItem = viewer.getTree().getItem (point);
			        if (clickedItem != null) 
			        {
			            
			            if(event.button == MouseEvent.BUTTON1){
			            	if(event.count == 2){	
			            		File fileToOpen = new File("MediaControler.java");
			            		if (fileToOpen.exists() && fileToOpen.isFile()) {
			            		    // TODO: Make the double click on a marker to open the line of the resource
			            		}else{
			            			IWorkbenchPage page = getSite().getPage();
			            			IEditorPart part = page.getActiveEditor();
			            			 if (!(part instanceof AbstractTextEditor))
			            				      return;
			            			 ITextEditor editor = (ITextEditor)part;
			            			   IDocumentProvider dp = editor.getDocumentProvider();
			            			   IDocument doc = dp.getDocument(editor.getEditorInput());
			            			   IRegion lineInfo;
			            			   lineInfo = null;
			            			try {
			            				int line = (Integer) ((IMarker)clickedItem.getData()).getAttribute(IMarker.LINE_NUMBER);
										lineInfo = doc.getLineInformation(line - 1);
									} catch (BadLocationException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (CoreException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
			            			   if(lineInfo!= null){
			            				   editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
			            			   }
			            		}
			            	}
			            }
			        }
			    }
		});
		
		
		TableLayout layout = new TableLayout();
		tc = new TreeViewerColumn(viewer, SWT.FULL_SELECTION);
		tc.getColumn().addSelectionListener(getHeaderListener());
		tc2 = new TreeViewerColumn(viewer, SWT.NONE);
		tc2.getColumn().addSelectionListener(getHeaderListener());
		tc3 = new TreeViewerColumn(viewer, SWT.NONE);
		tc3.getColumn().addSelectionListener(getHeaderListener());
		tc4 = new TreeViewerColumn(viewer, SWT.NONE);
		tc4.getColumn().addSelectionListener(getHeaderListener());
		tc5 = new TreeViewerColumn(viewer, SWT.NONE);
		tc5.getColumn().addSelectionListener(getHeaderListener());
		
		tc.getColumn().setText(EmergoView.textColumnOne);
		
		tc2.getColumn().setText(EmergoView.textColumnTwo);
		tc3.getColumn().setText(EmergoView.textColumnThree);
		tc4.getColumn().setText(EmergoView.textColumnFour);
		tc5.getColumn().setText(EmergoView.textColumnFive);
		
		tc.getColumn().setToolTipText("Tooltip one");
		
		tc2.getColumn().setToolTipText("Tooltip two");
		tc3.getColumn().setToolTipText("Tooltip three");
		tc4.getColumn().setToolTipText("Tooltip four");
		tc4.getColumn().setToolTipText("Tooltip five");
		
		Tree tree = viewer.getTree();
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);
		tree.layout(true);
		layout.addColumnData(new ColumnPixelData(100, true));
		layout.addColumnData(new ColumnPixelData(100, true));
		layout.addColumnData(new ColumnPixelData(100, true));
		layout.addColumnData(new ColumnPixelData(100, true));
		layout.addColumnData(new ColumnPixelData(100, true));
		viewer.setLabelProvider(new TreeLabelProvider());
		viewer.getTree().setLayout(layout);
		
		this.createContextMenu();
		getSite().setSelectionProvider(viewer);

	}
	
	 private void createContextMenu() {
         // Create menu manager.
		 MenuManager menuMgr = new MenuManager();
         menuMgr.setRemoveAllWhenShown(true);
         // Create menu.
         Menu menu = menuMgr.createContextMenu(viewer.getControl());
         viewer.getControl().setMenu(menu);
         // Register menu for extension.
         createAction();
         getSite().registerContextMenu(menuMgr, viewer);
	 }

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}
	
	public void updateTree() {
		try {
			IMarker[] markers  = fd.getFile().findMarkers(EMERGO_MARKER_ID, false, IResource.DEPTH_INFINITE);
			test = new ArrayList<IMarker>();
			List<MarkerGrouping> goupins = generateMarkerList(markers);
			viewer.refresh();
			viewer.setContentProvider(new TreeContentProvider());
			viewer.setInput(goupins);
		} catch (CoreException e) {
			e.printStackTrace();
		} 
	}

	private List<MarkerGrouping> generateMarkerList(IMarker[] markers)
			throws CoreException {
		List<MarkerGrouping> goupins = new ArrayList<MarkerGrouping>();
		String markerType = sortingType;
		for (int i = 0; i < markers.length; i++) {
			boolean wasAdded = false;
			for (int j = 0; j < goupins.size(); j++) {
				if(markers[i].getAttribute(markerType).toString().equals(goupins.get(j).getName())){
						goupins.get(j).addChildren(markers[i]);
						wasAdded = true;
				}
			}
			if(!wasAdded){
				MarkerGrouping mkg = new MarkerGrouping(markers[i].getAttribute(markerType).toString());
				mkg.addChildren(markers[i]);
				goupins.add(mkg);
			}
			test.add(markers[i]);
		}
		Collections.sort(goupins, new MarkerGroupingComparable());
		return goupins;
	}

	public static void adaptTo(DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> dependencyGraph, ITextEditor editor, IFile textSelectionFile, boolean delete) {
		/*
		 * Delete markers of all previously selected files.
		 */
		if(delete){
			for (IFile file : selectedFiles) {
				test.clear();
				EmergoMarker.clearMarkers(file);
			}			
		}
		
		/*
		 * Then, add the file being selected...
		 */
		selectedFiles.add(textSelectionFile);
		
		if (dependencyGraph.vertexSet().size() < 2) {
			return;
		}

		Set<DependencyNode> vertexSet = dependencyGraph.vertexSet();
		IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		for (DependencyNode srcNode : vertexSet) {

			if (!srcNode.isInSelection()) {
				continue;
			}
			KShortestPaths<DependencyNode, ValueContainerEdge<ConfigSet>> shortestPaths = new KShortestPaths<DependencyNode, ValueContainerEdge<ConfigSet>>(dependencyGraph, srcNode, MAX_PATHS);
			Set<DependencyNode> vertexSet2 = dependencyGraph.vertexSet();
			for (DependencyNode tgtNode : vertexSet2) {
				if (tgtNode == srcNode) {
					continue;
				}

				List<GraphPath<DependencyNode, ValueContainerEdge<ConfigSet>>> paths = shortestPaths.getPaths(tgtNode);
				// If no paths between the nodes were found, then just move on to the next pair of nodes.
				if (paths == null) {
					continue;
				}
				
				for (GraphPath<DependencyNode, ValueContainerEdge<ConfigSet>> path : paths) {
					ConfigSet configAccumulator = null;
					List<ValueContainerEdge<ConfigSet>> edgeList = path.getEdgeList();
					for (ValueContainerEdge<ConfigSet> edge : edgeList) {
						ConfigSet value = edge.getValue();
						if (configAccumulator == null) {
							configAccumulator = value;
						} else {
							configAccumulator = configAccumulator.and(value);
						}
					}
					int startLine = srcNode.getPosition().getStartLine() - 1;
					String message = null;
					try {
						message = document.get(document.getLineOffset(startLine), document.getLineLength(startLine)).toString().trim();
					} catch (BadLocationException e) {
						/*
						 * Something must have went very wrong here, because the line at issue is not a valid location
						 * in the document.
						 */
						message = "Unknown";
					}
					FeatureDependency auxFeature = new FeatureDependency().setConfiguration(configAccumulator).setFile(ResourceUtil.getIFile(tgtNode.getPosition().getFilePath())).setFeature(tgtNode.getConfigSet().toString()).setLineNumber(tgtNode.getPosition().getStartLine());
					fd = auxFeature;
					EmergoMarker.createMarker(message, auxFeature);
				}
			}
		}
	}

	public static void deleteMarkers(String message){
		Object[] markersToDelete = test.toArray();
		for(int i = 0; i < markersToDelete.length; i++){
			try {
				if (((IMarker)markersToDelete[i]).getAttribute(IMarker.MESSAGE).toString().equals(message)) {
					test.remove(markersToDelete[i]);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		if(selectedFiles != null){ // Has nothing to delete			
			for (IFile file : selectedFiles) {
				EmergoMarker.clearMarkers(file, message);
				}
		}
		IViewReference[] views = Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences();
		for (int i = 0; i < views.length; i++) {
			if(views[i].getId().equals("br.ufpe.cin.emergo.views.TestView")){
				((EmergoView) views[i].getView(true)).updateTree();
			}
		}
	}
	public static void deleteAllMarkers(){
		if(selectedFiles != null){	// Has nothing to delete
			for (IFile file : selectedFiles) {
				EmergoMarker.clearMarkers(file);
			}
		}
		IViewReference[] views = Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences();
		for (int i = 0; i < views.length; i++) {
			if(views[i].getId().equals("br.ufpe.cin.emergo.views.TestView")){
				((EmergoView) views[i].getView(true)).updateTree();
			}
		}
	}
	
	/**
	 * Return the listener that updates sort values on selection.
	 * 
	 * @return SelectionListener
	 */
	private SelectionListener getHeaderListener() {

		return new SelectionAdapter() {
			/**
			 * Handles the case of user selecting the header area.
			 */
			public void widgetSelected(SelectionEvent e) {

				final TreeColumn column = (TreeColumn) e.widget;
				final MarkerField field = (MarkerField) column
						.getData(MARKER_FIELD);
				setPrimarySortField(field, column);
			}

		};

	}
	
	/**
	 * Set the primary sort field to field and update the column.
	 * 
	 * @param field
	 * @param column
	 */
	private void setPrimarySortField(MarkerField field, TreeColumn column) {
		Comparator<IMarker> comparable = null;
		if(column.getText().equals(EmergoView.textColumnOne)){
			comparable = new MarkerMessageComparable();
		}else if(column.getText().equals(EmergoView.textColumnTwo)){
			comparable = new MarkerLineComparable();
		}if(column.getText().equals(EmergoView.textColumnThree)){
			comparable = new MarkerTextComparable();
		}if(column.getText().equals(EmergoView.textColumnFour)){
			comparable = new MarkerTextComparable();
		}
		if(auxiliary==0){
			Collections.sort(test, comparable);			
		}
		else{
			Collections.sort(test, comparable);
			Collections.reverse(test);			
		}
		updateDirectionIndicator(column, field);
		reOriginateTree();
	}

	private void reOriginateTree() {
		IMarker[] markers = new IMarker[test.size()];
		markers = test.toArray(markers);
		List<MarkerGrouping> goupins = null;
		try {
			test = new ArrayList<IMarker>();
			goupins = generateMarkerList(markers);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		Object[] expanded = viewer.getExpandedElements();
		TreePath[] paths = viewer.getExpandedTreePaths();
		viewer.setInput(goupins);
		viewer.setExpandedElements(expanded);
		viewer.setExpandedTreePaths(paths);
	}
	/**
	 * Update the direction indicator as column is now the primary column.
	 * 
	 * @param column
	 * @field {@link MarkerField}
	 */
	void updateDirectionIndicator(TreeColumn column, MarkerField field) {
		viewer.getTree().setSortColumn(column);
		if (auxiliary==0){
			viewer.getTree().setSortDirection(SWT.UP);
			auxiliary=1;
		}
		else{
			viewer.getTree().setSortDirection(SWT.DOWN);
			auxiliary=0;
		}
	}
	
	private class MarkerLineComparable implements Comparator<IMarker>{

		@Override
		public int compare(IMarker arg0, IMarker arg1) {
			try {
				int line0 = Integer.valueOf(arg0.getAttribute(IMarker.LINE_NUMBER).toString());
				int line1 = Integer.valueOf(arg1.getAttribute(IMarker.LINE_NUMBER).toString());
				return line0-line1;
			} catch (CoreException e) {
				e.printStackTrace();
			}
			return 0;
		}
		
	}
	
	private class MarkerMessageComparable implements Comparator<IMarker>{
		@Override
		public int compare(IMarker arg0, IMarker arg1) {
			try {
				String line0 = (arg0.getAttribute(IMarker.MESSAGE).toString());
				String line1 = (arg1.getAttribute(IMarker.MESSAGE).toString());
				return line0.compareTo(line1);
			} catch (CoreException e) {
				e.printStackTrace();
			}
			return 0;
		}
		
	}
	
	private class MarkerTextComparable implements Comparator<IMarker>{

		@Override
		public int compare(IMarker arg0, IMarker arg1) {
			try {
				String line0 = (arg0.getAttribute(IMarker.TEXT).toString());
				String line1 = (arg1.getAttribute(IMarker.TEXT).toString());
				return line0.compareTo(line1);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return 0;
		}
		
	}
	
	private class MarkerGroupingComparable implements Comparator<MarkerGrouping>{
		@Override
		public int compare(MarkerGrouping arg0, MarkerGrouping arg1) {
			return arg0.getName().compareTo(arg1.getName());
		}
		
	}
	
	public void createAction(){
		getViewSite().getActionBars().getMenuManager().add(getSortAction(0));  
		getViewSite().getActionBars().getMenuManager().add(getSortAction(1));  
		getViewSite().getActionBars().getMenuManager().add(getSortAction(2));  
		getViewSite().getActionBars().getMenuManager().add(new Separator()); //Add a horizontal separator  
	}
	public Action getSortAction(int type){
		/* Types are: 0 (Default, line text)
		 * 1 (line number)
		 * 2 (feature)
		 */
		Action sortAction = null;
		switch(type){
			case 0:
				sortAction =  new Action() {
					@Override
					public void run() {
						sortingType = IMarker.MESSAGE;
						reOriginateTree();
					}
				};
				sortAction.setText("Group by Description");
				break;
			case 1:
				sortAction =  new Action() {
					@Override
					public void run() {
						sortingType = IMarker.TEXT;
						reOriginateTree();
					}
				};
				sortAction.setText("Group by Configuration");
				break;
			case 2:
				sortAction =  new Action() {
					@Override
					public void run() {
						sortingType = IMarker.TASK;
						reOriginateTree();
					}
				};
				sortAction.setText("Group by Feature");
				break;
		}
		
		
		return sortAction;
	}
}
