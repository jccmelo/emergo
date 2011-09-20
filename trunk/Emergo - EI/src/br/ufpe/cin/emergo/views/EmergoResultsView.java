package br.ufpe.cin.emergo.views;

import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.markers.MarkerSupportView;
import org.jgrapht.DirectedGraph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.KShortestPaths;

import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.core.SelectionPosition;
import br.ufpe.cin.emergo.graph.DependencyNode;
import br.ufpe.cin.emergo.graph.ValueContainerEdge;
import br.ufpe.cin.emergo.markers.EmergoMarker;
import br.ufpe.cin.emergo.markers.FeatureDependency;

public class EmergoResultsView extends MarkerSupportView {

	public static final String ID = "br.ufpe.cin.emergo.view.EmergoResultsView";
	private static final int MAX_PATHS = 3;

	public EmergoResultsView() {
		super("emergoResultsSupport");
	}

	public static void adaptTo(DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> dependencyGraph, ICompilationUnit compilationUnit, ITextEditor editor, SelectionPosition spos, IFile textSelectionFile) {
		Set<DependencyNode> vertexSet = dependencyGraph.vertexSet();
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
					String accString = configAccumulator == null ? "" : configAccumulator.toString();
					EmergoMarker.createMarker("Line " + srcNode.getPosition().getStartLine(), new FeatureDependency().setConfiguration(accString).setFile(textSelectionFile).setFeature(tgtNode.getConfigSet().toString()).setLineNumber(tgtNode.getPosition().getStartLine()));
				}
			}
		}
	}
}