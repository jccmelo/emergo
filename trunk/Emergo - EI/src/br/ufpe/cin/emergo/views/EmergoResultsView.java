package br.ufpe.cin.emergo.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
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
	private static final int MAX_PATHS = 16;
	private static List<IFile> selectedFiles = new ArrayList<IFile>();
	
	
	public EmergoResultsView() {
		super("emergoResultsSupport");
	}
	
	
	public static void adaptTo(DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> dependencyGraph, ITextEditor editor, SelectionPosition spos, IFile textSelectionFile) {
		
		/*
		 * Delete markers of all previously selected files.
		 */
		for (IFile file : selectedFiles) {
			EmergoMarker.clearMarkers(file);
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
					EmergoMarker.createMarker(message, auxFeature);
					//EmergoMarker.createMarker(message, new FeatureDependency().setConfiguration(accString).setFile(textSelectionFile).setFeature(tgtNode.getConfigSet().toString()).setLineNumber(tgtNode.getPosition().getStartLine()));
				}
			}
		}
	}
}