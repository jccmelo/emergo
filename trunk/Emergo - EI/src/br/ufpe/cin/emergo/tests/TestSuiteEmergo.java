package br.ufpe.cin.emergo.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BlockTextSelection;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.jgrapht.DirectedGraph;

import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.core.DependencyFinder;
import br.ufpe.cin.emergo.core.EmergoException;
import br.ufpe.cin.emergo.core.SelectionPosition;
import br.ufpe.cin.emergo.graph.DependencyNode;
import br.ufpe.cin.emergo.graph.ValueContainerEdge;
import br.ufpe.cin.emergo.handlers.CommandCompilationUnit;
import br.ufpe.cin.emergo.handlers.CompilationUnitGroovy;
import br.ufpe.cin.emergo.preprocessor.ContextManager;
import br.ufpe.cin.emergo.util.GraphResult;
import br.ufpe.cin.emergo.util.PositionXML;

public class TestSuiteEmergo {
	
	/**
	 * This method stores the dependency graph in .txt file to make regression tests later. 
	 */
	public void testTool(String projectName, String classpath, String fileExtension, PositionXML position) {
		int startLine = position.getStartLine();
		int endLine = position.getEndLine();
		int startColumn = position.getStartColumn();
		int endColumn = position.getEndColumn();
		int offset = position.getOffSet();
		int length = position.getLength();
		
		String className = position.getClassName();
		String methodName = position.getMethodName();
		
		String filePath = classpath+className+"."+fileExtension;
		
		ContextManager context = ContextManager.getContext();
		context.setSrcfile(filePath);
		
		SelectionPosition selectionPosition = SelectionPosition.builder().length(length).offSet(offset).startLine(startLine).startColumn(startColumn).endLine(endLine).endColumn(endColumn).filePath(filePath).build();
		final Map<Object, Object> options = new HashMap<Object, Object>();
		
//		IPath path = new Path(filePath);
		IDocument document = new Document(filePath);
		
//		IWorkspace workspace = ResourcesPlugin.getWorkspace();
//		IFile textSelectionFile = workspace.getRoot().getFileForLocation(path);
		
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		IProject project = ws.getRoot().getProject(projectName);
		if (!project.exists())
			try {
				project.create(null);
			} catch (CoreException e1) {
				e1.printStackTrace();
			}
		if (!project.isOpen())
			try {
				project.open(null);
			} catch (CoreException e1) {
				e1.printStackTrace();
			}

		IPath location = new Path(filePath);
		IFile textSelectionFile = project.getFile(location.lastSegment());
		
		ITextSelection blockTextSelection = new BlockTextSelection(document, selectionPosition.getStartLine(), selectionPosition.getStartColumn(), selectionPosition.getEndLine(), selectionPosition.getEndColumn(), 0);
    	
		ArrayList<String> cp = new ArrayList<String>();
		cp.add(classpath);
		
		options.put("classpath", cp);
		options.put("correspondentClasspath", classpath.replaceAll("src", "bin"));
		options.put("fileExtension", fileExtension);
		options.put("methodName", methodName);
		options.put("selectionFile", filePath);
		
		options.put("featureDependence", true);

		
		CommandCompilationUnit cuGroovy = new CompilationUnitGroovy();
		cuGroovy.markNodesFromSelection(textSelectionFile, blockTextSelection, options);
		
		try {
			DirectedGraph<DependencyNode,ValueContainerEdge<ConfigSet>> dependencyGraph = DependencyFinder.findFromSelection(selectionPosition, options);
			GraphResult result = new GraphResult();
			result.saveGraph(dependencyGraph);
			
		} catch (EmergoException e) {
			e.printStackTrace();
		}
	}
}
