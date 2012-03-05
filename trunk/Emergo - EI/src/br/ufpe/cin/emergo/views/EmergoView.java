package br.ufpe.cin.emergo.views;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
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
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
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

public class EmergoView extends ViewPart {
	public static final String ID = "br.ufpe.cin.emergo.views.EmergoView";
	private TreeViewer viewer;
	private static final int MAX_PATHS = 16;
	private List<IMarker> markerList;
	public static final String EMERGO_MARKER_ID = Activator.PLUGIN_ID + ".emergomarker";
	private static final String MARKER_FIELD = "MARKER_FIELD"; //$NON-NLS-1$
	
	TreeViewerColumn tc;
	//CONFIGURATIONCOLUMN
//	TreeViewerColumn tc2;
	TreeViewerColumn tc3;
	TreeViewerColumn tc4;
	TreeViewerColumn tc5;
	
	private static String textColumnOne = "Description";
	//CONFIGURATIONCOLUMN
//	private static String textColumnTwo = "Configuration";
	private static String textColumnThree = "Location";
	private static String textColumnFour = "Feature";
	private static String textColumnFive = "Resource";
	private static String sortingType = IMarker.TASK;
	private int auxiliary=0;
	
	Action addItemAction, deleteItemAction, selectAllAction;

	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		viewer = new TreeViewer(parent, SWT.FULL_SELECTION);
		viewer.getTree().setLinesVisible(true);
		viewer.setSelection(new TreeSelection());
		viewer.getTree().addListener(SWT.MouseDown, new Listener() {

			@Override
			public void handleEvent(Event event) {
				Point point = new Point(event.x, event.y);
				TreeItem clickedItem = viewer.getTree().getItem(point);
				if (clickedItem != null) {
					if (event.button == MouseEvent.BUTTON1 && event.count == 2) {
						Object data = clickedItem.getData();
						if (data instanceof IMarker) {
							IMarker marker = (IMarker) data;
							try {
								IDE.openEditor(getSite().getPage(), marker);
							} catch (PartInitException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} 
						}
					}
				}
			}
		});
		
		TableLayout layout = new TableLayout();
		tc = new TreeViewerColumn(viewer, SWT.FULL_SELECTION);
		tc.getColumn().addSelectionListener(getHeaderListener());
		//CONFIGURATIONCOLUMN
//		tc2 = new TreeViewerColumn(viewer, SWT.NONE);
//		tc2.getColumn().addSelectionListener(getHeaderListener());
		tc3 = new TreeViewerColumn(viewer, SWT.NONE);
		tc3.getColumn().addSelectionListener(getHeaderListener());
		tc4 = new TreeViewerColumn(viewer, SWT.NONE);
		tc4.getColumn().addSelectionListener(getHeaderListener());
		tc5 = new TreeViewerColumn(viewer, SWT.NONE);
		tc5.getColumn().addSelectionListener(getHeaderListener());
		
		tc.getColumn().setText(EmergoView.textColumnOne);
		//CONFIGURATIONCOLUMN
//		tc2.getColumn().setText(EmergoView.textColumnTwo);
		tc3.getColumn().setText(EmergoView.textColumnThree);
		tc4.getColumn().setText(EmergoView.textColumnFour);
		tc5.getColumn().setText(EmergoView.textColumnFive);
		
		tc.getColumn().setToolTipText("Tooltip one");
		//CONFIGURATIONCOLUMN
//		tc2.getColumn().setToolTipText("Tooltip two");
		tc3.getColumn().setToolTipText("Tooltip three");
		tc4.getColumn().setToolTipText("Tooltip four");
		tc4.getColumn().setToolTipText("Tooltip five");
		
		Tree tree = viewer.getTree();
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);
		tree.layout(true);
		layout.addColumnData(new ColumnPixelData(100, true));
		//CONFIGURATIONCOLUMN
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

	public void setFocus() {
		// TODO Auto-generated method stub
	}
	
	public void updateTree() {
		try {
			IMarker[] markers = ResourcesPlugin.getWorkspace().getRoot().findMarkers(EMERGO_MARKER_ID, false, IResource.DEPTH_INFINITE);
			markerList = new ArrayList<IMarker>();
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
		List<MarkerGrouping> groupings = new ArrayList<MarkerGrouping>();
		String markerType = sortingType;
		for (int i = 0; i < markers.length; i++) {
			boolean wasAdded = false;
			for (int j = 0; j < groupings.size(); j++) {
				if(markers[i].getAttribute(markerType).toString().equals(groupings.get(j).getName())){
						groupings.get(j).addChildren(markers[i]);
						wasAdded = true;
				}
			}
			if(!wasAdded){
				MarkerGrouping mkg = new MarkerGrouping(markers[i].getAttribute(markerType).toString());
				mkg.addChildren(markers[i]);
				groupings.add(mkg);
			}
			markerList.add(markers[i]);
		}
		Collections.sort(groupings, new MarkerGroupingComparable());
		return groupings;
	}
	
	public void clearView() throws CoreException {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		workspaceRoot.deleteMarkers(EmergoMarker.EMERGO_MARKER_ID, true,
				IResource.DEPTH_INFINITE);
	}

	public void adaptTo(DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> dependencyGraph, boolean delete) {
		/*
		 * Delete markers of all previously selected files.
		 */
		if (delete){
			try {
				clearView();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		
		if (dependencyGraph.vertexSet().size() < 2) {
			return;
		}
		
		/*
		 * Store a reference to every FeatureDependency so that IMarkers created
		 * based on them can be checked for duplicates.
		 */
		Set<FeatureDependency> featureDependencySet = new HashSet<FeatureDependency>();
		
		Set<DependencyNode> sourceVertexSet = dependencyGraph.vertexSet();
		for (DependencyNode srcNode : sourceVertexSet) {

			if (!srcNode.isInSelection()) {
				continue;
			}
			KShortestPaths<DependencyNode, ValueContainerEdge<ConfigSet>> shortestPaths = new KShortestPaths<DependencyNode, ValueContainerEdge<ConfigSet>>(dependencyGraph, srcNode, MAX_PATHS);
			Set<DependencyNode> targetVertexSet = dependencyGraph.vertexSet();
			for (DependencyNode tgtNode : targetVertexSet) {
				
				if (tgtNode.equals(srcNode)  || tgtNode.getConfigSet().isTrueSet()) {
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
						IDocument document = getDocument(srcNode.getPosition().getFilePath());
						message = document.get(document.getLineOffset(startLine), document.getLineLength(startLine)).toString().trim();
						message = document.get(document.getLineOffset(startLine), document.getLineLength(startLine)).toString().trim();
					} catch (BadLocationException e) {
						e.printStackTrace();
						message = "Unknown";
					} catch (CoreException e) {
						e.printStackTrace();
						message = "Unknown";
					}
					
					/*
					 * Do not create an IMarker if an equivalent FeatureDependency already exists.
					 */
					FeatureDependency auxFeature = new FeatureDependency().setFile(ResourceUtil.getIFile(tgtNode.getPosition().getFilePath())).setFeature(tgtNode.getFeatureSet().toString()).setLineNumber(tgtNode.getPosition().getStartLine()).setMessage(message);
					if (!featureDependencySet.add(auxFeature)){
						continue;
					}
					
					/*
					 * If the IMarker could not be created, remove the corresponding
					 * FeatureDependency from the set.
					 */
					IMarker createdMarker = EmergoMarker.createMarker(auxFeature);
					if (createdMarker == null) {
						featureDependencySet.remove(auxFeature);
					}
				}
			}
		}
		updateTree();
	}
	
	///XXX DUPLICATED METHOD FROM EmergoGraphView
	private IDocument getDocument(String filename) throws CoreException {
		IFile file = ResourceUtil.getIFile(filename);
	    
	    //XXX Does the method disconnect need to be called? When?
	    ITextFileBufferManager.DEFAULT.connect(file.getFullPath(), LocationKind.IFILE, null);
		return FileBuffers.getTextFileBufferManager().getTextFileBuffer(file.getFullPath(), LocationKind.IFILE).getDocument();
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
		}
		//CONFIGURATIONCOLUMN
		/*else if(column.getText().equals(EmergoView.textColumnTwo)){
			comparable = new MarkerLineComparable();
		}*/
		if(column.getText().equals(EmergoView.textColumnThree)){
			comparable = new MarkerTextComparable();
		}if(column.getText().equals(EmergoView.textColumnFour)){
			comparable = new MarkerTextComparable();
		}
		if(auxiliary==0){
			Collections.sort(markerList, comparable);			
		}
		else{
			Collections.sort(markerList, comparable);
			Collections.reverse(markerList);			
		}
		updateDirectionIndicator(column, field);
		reOriginateTree();
	}

	private void reOriginateTree() {
		IMarker[] markers = new IMarker[markerList.size()];
		markers = markerList.toArray(markers);
		List<MarkerGrouping> goupins = null;
		try {
			markerList = new ArrayList<IMarker>();
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
	//CONFIGURATIONCOLUMN
//	private class MarkerLineComparable implements Comparator<IMarker>{
//
//		@Override
//		public int compare(IMarker arg0, IMarker arg1) {
//			try {
//				int line0 = Integer.valueOf(arg0.getAttribute(IMarker.LINE_NUMBER).toString());
//				int line1 = Integer.valueOf(arg1.getAttribute(IMarker.LINE_NUMBER).toString());
//				return line0-line1;
//			} catch (CoreException e) {
//				e.printStackTrace();
//			}
//			return 0;
//		}
//		
//	}
	
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
		//CONFIGURATIONCOLUMN
//		getViewSite().getActionBars().getMenuManager().add(getSortAction(1));
		getViewSite().getActionBars().getMenuManager().add(getSortAction(2));  
//		getViewSite().getActionBars().getMenuManager().add(new Separator()); //Add a horizontal separator  
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
				//CONFIGURATIONCOLUMN
			/*case 1:
				sortAction =  new Action() {
					@Override
					public void run() {
						sortingType = IMarker.TEXT;
						reOriginateTree();
					}
				};
				sortAction.setText("Group by Configuration");
				break;*/
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
