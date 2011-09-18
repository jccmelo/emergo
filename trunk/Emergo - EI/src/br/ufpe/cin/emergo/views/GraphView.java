package br.ufpe.cin.emergo.views;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.core.widgets.IContainer;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.SpringLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm;
import org.jgrapht.DirectedGraph;

import br.ufpe.cin.emergo.core.SelectionPosition;
import br.ufpe.cin.emergo.graph.ValueContainerEdge;

public class GraphView extends ViewPart {
	public static final String ID = "br.ufpe.cin.emergo.view.GraphView";
	private Graph graph;
	private ITextEditor editor;
	private ICompilationUnit cu;

	public void createPartControl(Composite parent) {
		// Graph will hold all other objects
		graph = new Graph(parent, SWT.NONE);

		graph.setLayoutAlgorithm(new SpringLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);
		// For a different layout algorith, comment the live above and uncomment the one below.
		// graph.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);

		// Adds a simple listener for a selection in the graph. Use this to link to the line number in the file.
		graph.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editor.selectAndReveal(1, 100);
			}
		});
	}

	/**
	 * Redraws the graph that is in the view.
	 */
	public void redraw() {
		this.graph.redraw();
	}

	/**
	 * XXX
	 * 
	 * @param dependencyGraph
	 * @param compilationUnit
	 * @param editor
	 * @param spos
	 */
	public void adaptTo(DirectedGraph<Object, ValueContainerEdge> dependencyGraph, ICompilationUnit compilationUnit, ITextEditor editor, SelectionPosition spos) {
		this.cu = compilationUnit;
		this.editor = editor;

		// TODO: make this configurable for the user.
		graph.setLayoutAlgorithm(new SpringLayoutAlgorithm(LayoutStyles.ENFORCE_BOUNDS | LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);

		/*
		 * The Graph from the Zest toolkit will gladly add objects that are are equal by the JAVA Object#equals(..)
		 * contract into the graph. This inspired the workaround using the Map objectNodeMapping to keep track of which
		 * nodes were already added.
		 */
		Map<Object, GraphNode> objectNodeMapping = new HashMap<Object, GraphNode>();
		Set<ValueContainerEdge> edgeSet = dependencyGraph.edgeSet();
		for (ValueContainerEdge valueContainerEdge : edgeSet) {
			Object edgeSrc = dependencyGraph.getEdgeSource(valueContainerEdge);
			Object edgeTgt = dependencyGraph.getEdgeTarget(valueContainerEdge);

			GraphNode src = objectNodeMapping.get(edgeSrc);
			GraphNode tgt = objectNodeMapping.get(edgeTgt);

			if (src == null) {
				src = new MyGraphNode(graph, SWT.NONE, edgeSrc.toString(), edgeSrc);
				objectNodeMapping.put(edgeSrc, src);
			}
			if (tgt == null) {
				tgt = new MyGraphNode(graph, SWT.NONE, edgeTgt.toString(), edgeTgt);
				objectNodeMapping.put(edgeTgt, tgt);
			}
			GraphConnection graphConnection = new GraphConnection(graph, ZestStyles.CONNECTIONS_DIRECTED, src, tgt);
			// XXX replace this by a propper string representation of a feature expresssion.
			graphConnection.setText("true");
		}
	}

	/**
	 * A simple class that only implements equals/hashCode to be used in the Map on the adaptTo method.
	 * 
	 * @author Társis
	 * 
	 */
	class MyGraphNode extends GraphNode {

		public MyGraphNode(IContainer graphModel, int style, String text, Object data) {
			super(graphModel, style, text, data);
			// TODO Auto-generated constructor stub
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof GraphNode) {
				if (this == obj) {
					return true;
				} else {
					GraphNode other = (GraphNode) obj;
					return this.getGraphModel().equals(other.getGraphModel()) && this.getData().equals(other.getData()) && this.getText().equals(other.getText());
				}
			}
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((this.getGraphModel() == null) ? 0 : this.getGraphModel().hashCode());
			result = prime * result + ((this.getData() == null) ? 0 : this.getData().hashCode());
			result = prime * result + ((this.getText() == null) ? 0 : this.getText().hashCode());
			return result;
		}

	}

	@Override
	public void setFocus() {
		// XXX don't know what to do with this.
	}
}