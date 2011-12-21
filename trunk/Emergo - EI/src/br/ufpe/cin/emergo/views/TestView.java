package br.ufpe.cin.emergo.views;

import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
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
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
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

public class TestView extends ViewPart{
	public static final String ID = "br.ufpe.cin.emergo.views.TestView";
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
	private MarkedLinesAction lCustomAction;
	private CellLabelProvider cl;
	
	public TestView() {
		// TODO Auto-generated constructor stub
		selectedFiles = new ArrayList<IFile>();
	}
	
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		viewer = new TreeViewer(parent, SWT.FULL_SELECTION);
//		viewer = new MarkersTreeViewer(new Tree(parent, SWT.H_SCROLL
//				/*| SWT.VIRTUAL */| SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION));
		viewer.getTree().setLinesVisible(true);
		//viewer.setUseHashlookup(true);
		viewer.setSelection(new TreeSelection());
		viewer.getTree().addListener(SWT.MouseDown, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				 Point point = new Point (event.x, event.y);
			        TreeItem clickedItem = viewer.getTree().getItem (point);
			        if (clickedItem != null) 
			        {
			            // Rechtsklick
			            if(event.button == MouseEvent.BUTTON3)
			            {
			                System.out.println("Right click (context menu) on element: " + clickedItem);
			                //showContextMenu(tree, clickedItem);
			            }else if(event.button == MouseEvent.BUTTON1){
			            	if(event.count == 2){	
			            		System.out.println("Double Click, call the go to line method: "+ clickedItem.getData());
			            		File fileToOpen = new File("MediaControler.java");
			            		if (fileToOpen.exists() && fileToOpen.isFile()) {
			            		    IFileStore fileStore = EFS.getLocalFileSystem().getStore(fileToOpen.toURI());
			            		    IFile page = (IFile) PlatformUI.getWorkbench().getAdapter(IFile.class);
			            		    
			            		    //IFile textSelectionFile = (IFile) part.getAdapter(IFile.class);
			            		    //System.out.println(page);
//			            		    try {
//			            		        //IDE.openEditorOnFileStore( page, fileStore );
//			            		    } catch ( PartInitException e ) {
//			            		        //Put your exception handler here if you wish to
//			            		    }
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
			            			System.out.println("shit");
			            		}
			            	}
			            }
			            else
			            {
			                System.out.println("event.button != "+clickedItem.getText());
			            }
			        }
			        else
			        {
			            System.out.println("selected item = null");
			            //this.mgr.removeAll();
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
		
		tc.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				System.out.println("Table Selection");
				System.out.println(event);
			}
		});
		
		tc.getColumn().setText(TestView.textColumnOne);
		
		tc2.getColumn().setText(TestView.textColumnTwo);
		tc3.getColumn().setText(TestView.textColumnThree);
		tc4.getColumn().setText(TestView.textColumnFour);
		tc5.getColumn().setText(TestView.textColumnFive);
		
		tc.getColumn().setToolTipText("Tooltip one");
		
		tc2.getColumn().setToolTipText("Tooltip two");
		tc3.getColumn().setToolTipText("Tooltip three");
		tc4.getColumn().setToolTipText("Tooltip four");
		tc4.getColumn().setToolTipText("Tooltip five");
		
		//tc.setLabelProvider(new TreeLabelProvider());
		Tree tree = viewer.getTree();
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);
		tree.layout(true);
		layout.addColumnData(new ColumnPixelData(100, true));
		layout.addColumnData(new ColumnPixelData(100, true));
		layout.addColumnData(new ColumnPixelData(100, true));
		layout.addColumnData(new ColumnPixelData(100, true));
		layout.addColumnData(new ColumnPixelData(100, true));
//		tc.setLabelProvider(new TreeLabelProvider());
		viewer.setLabelProvider(new TreeLabelProvider());
		viewer.getTree().setLayout(layout);
		
		//viewer.setSorter(sorter);
		
		
		this.createActions();
		this.createContextMenu();
		getSite().setSelectionProvider(viewer);

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
					 mgr.add(addItemAction);
		                mgr.add(deleteItemAction);
		                mgr.add(new Separator());
		                mgr.add(selectAllAction);
				}
         });
         
         // Create menu.
      Menu menu = menuMgr.createContextMenu(viewer.getControl());
         viewer.getControl().setMenu(menu);
         
         // Register menu for extension.
         createAction();
      getSite().registerContextMenu(menuMgr, viewer);
 }

	 public void createActions() {
         addItemAction = new Action("Add...") {
                 public void run() { 
                            System.out.println("ACTION ADD");
                    }
            };
            deleteItemAction = new Action("Delete") {
                    public void run() {
                    	System.out.println("DELETE ADD");
                    }
            };
            selectAllAction = new Action("Select All") {
                    public void run() {
                    	System.out.println("Select All");
                    }
            };
            
            // Add selection listener.
         viewer.addSelectionChangedListener(new ISelectionChangedListener() {
                    public void selectionChanged(SelectionChangedEvent event) {
                           System.out.println("UPDATE ACTION ELEMENT!!");
                    }
            });
    }
	
	private void createColumns(TreeColumn[] currentColumns) {

		Tree tree = viewer.getTree();
		TableLayout layout = new TableLayout();
		
		MarkerField mk = null;
		
		MarkerField[] fields = new MarkerField[]{mk};

		IMemento columnWidths = null;
		
		for (int i = 0; i < fields.length; i++) {
			MarkerField markerField = fields[i];

			TreeViewerColumn column;
			if (i < currentColumns.length)
				column = new TreeViewerColumn(viewer, currentColumns[i]);
			else {
				column = new TreeViewerColumn(viewer, SWT.NONE);
				column.getColumn().setResizable(true);
				column.getColumn().setMoveable(true);
				//column.getColumn().addSelectionListener(getHeaderListener());
			}

			//column.getColumn().setData(MARKER_FIELD, markerField);
			// Show the help in the first column
			//column.setLabelProvider(new MarkerColumnLabelProvider(markerField));
			column.getColumn().setText(markerField.getColumnHeaderText());
			column.getColumn().setToolTipText(
					markerField.getColumnTooltipText());
			column.getColumn().setImage(markerField.getColumnHeaderImage());

			EditingSupport support = markerField.getEditingSupport(viewer);
			if (support != null)
				column.setEditingSupport(support);

//			if (builder.getPrimarySortField().equals(markerField))
//				updateDirectionIndicator(column.getColumn(), markerField);

			int columnWidth = -1;

			if (i == 0) {
				// Compute and store a font metric
				GC gc = new GC(tree);
				gc.setFont(tree.getFont());
				FontMetrics fontMetrics = gc.getFontMetrics();
				gc.dispose();
				columnWidth = Math.max(markerField.getDefaultColumnWidth(tree),
						fontMetrics.getAverageCharWidth() * 5);
			}

			if (columnWidths != null) {
//				Integer value = columnWidths.getInteger(getFieldId(column
//						.getColumn()));
//
//				// Make sure we get a useful value
//				if (value != null && value.intValue() > 0)
//					columnWidth = value.intValue();
			}

			// Take trim into account if we are using the default value, but not
			// if it is restored.
			if (columnWidth < 0)
				layout.addColumnData(new ColumnPixelData(markerField
						.getDefaultColumnWidth(tree), true, true));
			else
				layout.addColumnData(new ColumnPixelData(columnWidth, true));

		}

		// Remove extra columns
		if (currentColumns.length > fields.length) {
			for (int i = fields.length; i < currentColumns.length; i++) {
				currentColumns[i].dispose();

			}
		}

		viewer.getTree().setLayout(layout);
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);
		tree.layout(true);

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
			//updateTableCell();
			int a = 3;
			//((TreeContentProvider)viewer.getContentProvider()).
			viewer.refresh();
			viewer.setContentProvider(new TreeContentProvider());
			
			//MarkerGrouping mkg = new MarkerGrouping(test);
			//mkg.setName("markers");
			//goupins.add(mkg);
			viewer.setInput(goupins);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	private List<MarkerGrouping> generateMarkerList(IMarker[] markers)
			throws CoreException {
		List<MarkerGrouping> goupins = new ArrayList<MarkerGrouping>();
		String markerType = sortingType;
		for (int i = 0; i < markers.length; i++) {
//				viewer.add(markers[i], markers[i]);
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
			System.out.println("adding..................");
		}
		Collections.sort(goupins, new MarkerGroupingComparable());
		return goupins;
	}

	private void updateTableCell() {
		// column 1
		
		cl = new CellLabelProvider() {
			private List<IMarker> results = test;
			private int index = 0;
			@Override
			public void update(ViewerCell cell) {
				System.out.println("updates Cell");
				System.out.println(cell);
				try {
					cell.setText(results.get(index++).getAttribute(IMarker.MESSAGE).toString());
					
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					index=0;
				}
			}
		};
		//tc.setLabelProvider(new TreeCellProvider());
		
		//End of column 1
		// column 2
//		cl = new CellLabelProvider() {
//			private List<IMarker> results = test;
//			private int index = 0;
//			@Override
//			public void update(ViewerCell cell) {
//				System.out.println("updates Cell");
//				System.out.println(cell);
//				try {
//					cell.setText(results.get(index++).getAttribute(IMarker.LINE_NUMBER).toString());
//				} catch (CoreException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		};
//		tc2.setLabelProvider(cl);
//		
//		//End of column 2
//		// column 3
//		cl = new CellLabelProvider() {
//			private List<IMarker> results = test;
//			private int index = 0;
//			@Override
//			public void update(ViewerCell cell) {
//				System.out.println("updates Cell");
//				System.out.println(cell);
//				try {
//					cell.setText(results.get(index++).getAttribute(IMarker.TEXT).toString());
//				} catch (CoreException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		};
//		tc3.setLabelProvider(cl);
//		
//		//End of column 3
//		// column 4
//		cl = new CellLabelProvider() {
//			private List<IMarker> results = test;
//			private int index = 0;
//			@Override
//			public void update(ViewerCell cell) {
//				System.out.println("updates Cell");
//				System.out.println(cell);
//				try {
//					cell.setText(results.get(index++).getAttribute(IMarker.TASK).toString());
//				} catch (CoreException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		};
//		tc4.setLabelProvider(cl);
//		//End of column 4
	}
	
	public static void adaptTo(DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> dependencyGraph, ITextEditor editor, IFile textSelectionFile, boolean delete) {
		
		/*
		 * Delete markers of all previously selected files.
		 */
		if(delete){
			for (IFile file : selectedFiles) {
				System.out.println(".....doesnt matter, they were cleaned.....");
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
				
				// If not paths between the nodes were found, then just move on to the next pair of nodes.
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

//					String accString = configAccumulator == null ? "" : configAccumulator.toString();
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
					FeatureDependency auxFeature = new FeatureDependency().setConfiguration(configAccumulator).setFile(textSelectionFile).setFeature(tgtNode.getConfigSet().toString()).setLineNumber(tgtNode.getPosition().getStartLine());
					fd = auxFeature;
					System.out.println("-----> Markers:"+auxFeature);
					EmergoMarker.createMarker(message, auxFeature);
					//EmergoMarker.createMarker(message, new FeatureDependency().setConfiguration(accString).setFile(textSelectionFile).setFeature(tgtNode.getConfigSet().toString()).setLineNumber(tgtNode.getPosition().getStartLine()));
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
				// TODO Auto-generated catch block
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
			System.out.println(views);
			if(views[i].getId().equals("br.ufpe.cin.emergo.views.TestView")){
				((TestView) views[i].getView(true)).updateTree();
			}
		}
	}
	public static void deleteAllMarkers(){
		if(selectedFiles != null){	// Has nothing to delete
			for (IFile file : selectedFiles) {
				System.out.println(".....doesnt matter, they were cleaned.....");
				EmergoMarker.clearMarkers(file);
			}
		}
		IViewReference[] views = Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences();
		for (int i = 0; i < views.length; i++) {
			System.out.println(views);
			if(views[i].getId().equals("br.ufpe.cin.emergo.views.TestView")){
				((TestView) views[i].getView(true)).updateTree();
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
				System.out.println(column);
				System.out.println(field);
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
//		builder.setPrimarySortField(field);

		IWorkbenchSiteProgressService service = (IWorkbenchSiteProgressService) getViewSite()
				.getAdapter(IWorkbenchSiteProgressService.class);
		
		System.out.println("Column Text"+column.getText());
		Comparator<IMarker> comparable = null;
		if(column.getText().equals(TestView.textColumnOne)){
			comparable = new MarkerMessageComparable();
		}else if(column.getText().equals(TestView.textColumnTwo)){
			comparable = new MarkerLineComparable();
		}if(column.getText().equals(TestView.textColumnThree)){
			comparable = new MarkerTextComparable();
		}if(column.getText().equals(TestView.textColumnFour)){
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Object[] expanded = viewer.getExpandedElements();
		TreePath[] paths = viewer.getExpandedTreePaths();
//		builder.refreshContents(service);
		

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
				// TODO Auto-generated catch block
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
				// TODO Auto-generated catch block
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
		lCustomAction = new MarkedLinesAction();  
		lCustomAction.setText("Open Dialog Box");  
		getViewSite().getActionBars().getMenuManager().add(getSortAction(0));  
		getViewSite().getActionBars().getMenuManager().add(getSortAction(1));  
		getViewSite().getActionBars().getMenuManager().add(getSortAction(2));  
		getViewSite().getActionBars().getMenuManager().add(new Separator()); //Add a horizontal separator  
//		getViewSite().getActionBars().getMenuManager().add(lCustomAction);  
//		getViewSite().getActionBars().getMenuManager().add(lCustomAction);  
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
						System.out.println(sortingType);
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
						System.out.println(sortingType);
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
						System.out.println(sortingType);
					}
				};
				sortAction.setText("Group by Feature");
				break;
		}
		
		
		return sortAction;
	}
}
