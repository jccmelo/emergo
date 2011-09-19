package br.ufpe.cin.emergo.views;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.SpringLayoutAlgorithm;
import org.jgrapht.DirectedGraph;

import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.core.SelectionPosition;
import br.ufpe.cin.emergo.graph.DependencyNode;
import br.ufpe.cin.emergo.graph.ValueContainerEdge;

public class GraphView extends ViewPart {
	public static final String ID = "br.ufpe.cin.emergo.view.GraphView";
	private Graph graph;
	private ITextEditor editor;
	private ICompilationUnit cu;
	private Composite parent;

	public void createPartControl(Composite parent) {
		this.parent = parent;

		// Graph will hold all other objects
		graph = new Graph(parent, SWT.NONE);

		graph.setLayoutAlgorithm(new SpringLayoutAlgorithm(LayoutStyles.ENFORCE_BOUNDS), true);
		// For a different layout algorith, comment the live above and uncomment the one below.
		// graph.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);

		// Adds a simple listener for a selection in the graph. Use this to link to the line number in the file.
		graph.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				System.out.println(e);
				Graph source = (Graph) e.getSource();
				List selection = source.getSelection();
				if (!selection.isEmpty()) {
					Object selectionObj = selection.get(0);
					if (selectionObj instanceof GraphNode) {
						GraphNode selectedNode = (GraphNode) selectionObj;
						int startLine = ((DependencyNode) selectedNode.getData()).getPosition().getStartLine();
						int lineLength = 0, offset = 0;
						try {
							IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
							lineLength = document.getLineLength(startLine - 1);
							offset = document.getLineOffset(startLine - 1);
						} catch (Throwable e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						editor.selectAndReveal(offset, lineLength);
					}
				}
			}
		});
	}

	/**
	 * XXX
	 * 
	 * @param dependencyGraph
	 * @param compilationUnit
	 * @param editor
	 * @param spos
	 */
	public void adaptTo(DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> dependencyGraph, ICompilationUnit compilationUnit, ITextEditor editor, SelectionPosition spos) {
		this.cu = compilationUnit;
		this.editor = editor;
		clearGraph();

		Display display = parent.getDisplay();

		// TODO: make this configurable for the user.
		graph.setLayoutAlgorithm(new SpringLayoutAlgorithm(LayoutStyles.ENFORCE_BOUNDS), true);

		/*
		 * The Graph from the Zest toolkit will gladly add objects that are are equal by the JAVA Object#equals(..)
		 * contract into the graph. This inspired the workaround using the Map objectNodeMapping to keep track of which
		 * nodes were already added.
		 */
		Map<DependencyNode, GraphNode> objectNodeMapping = new HashMap<DependencyNode, GraphNode>();
		Set<ValueContainerEdge<ConfigSet>> edgeSet = dependencyGraph.edgeSet();
		for (ValueContainerEdge valueContainerEdge : edgeSet) {
			DependencyNode edgeSrc = dependencyGraph.getEdgeSource(valueContainerEdge);
			DependencyNode edgeTgt = dependencyGraph.getEdgeTarget(valueContainerEdge);

			GraphNode src = objectNodeMapping.get(edgeSrc);
			GraphNode tgt = objectNodeMapping.get(edgeTgt);

			if (edgeSrc.equals(edgeTgt))
				continue;

			if (src == null) {
				src = new GraphNode(graph, SWT.NONE, edgeSrc.toString(), edgeSrc);
				objectNodeMapping.put(edgeSrc, src);
				if (edgeSrc.isInSelection()) {
					src.setBorderWidth(2);
					src.setBackgroundColor(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
					src.setForegroundColor(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
				}
			}
			if (tgt == null) {
				tgt = new GraphNode(graph, SWT.NONE, edgeTgt.toString(), edgeTgt);
				objectNodeMapping.put(edgeTgt, tgt);
				if (edgeTgt.isInSelection()) {
					tgt.setBorderWidth(2);
					tgt.setBackgroundColor(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
					tgt.setForegroundColor(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
				}
			}

			GraphConnection graphConnection = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, src, tgt);
			// XXX replace this by a propper string representation of a feature expresssion.
			graphConnection.setText("true");
		}
	}

	private void clearGraph() {

		// remove all the connections
		Object[] objects = graph.getConnections().toArray();
		for (int x = 0; x < objects.length; x++) {
			((GraphConnection) objects[x]).dispose();
		}

		// remove all the nodes
		objects = graph.getNodes().toArray();
		for (int x = 0; x < objects.length; x++) {
			((GraphNode) objects[x]).dispose();
		}
	}

	@Override
	public void setFocus() {
		// XXX don't know what to do with this.
	}
}