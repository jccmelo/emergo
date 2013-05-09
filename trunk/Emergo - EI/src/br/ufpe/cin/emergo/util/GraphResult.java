package br.ufpe.cin.emergo.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.jgrapht.DirectedGraph;

import soot.Unit;
import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.core.DependencyFinder;
import br.ufpe.cin.emergo.graph.DependencyNode;
import br.ufpe.cin.emergo.graph.ValueContainerEdge;
import br.ufpe.cin.emergo.graph.transform.DependencyNodeWrapper;
import br.ufpe.cin.emergo.preprocessor.ContextManager;
import br.ufpe.cin.emergo.util.ResourceUtil;

public class GraphResult {

	/**
	 * This method saves the dependency graph in text file.
	 * 
	 * @param directedGraph
	 */
	public void saveGraph(DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> dependencyGraph) {
		
		String className = DependencyFinder.retrieveClassName(ContextManager.getContext().getSrcfile());

		String path = settingDirectory(className) + className + ".txt";
		
		int i = 1;
		path = verifyFileExistance(className, settingDirectory(className), i);

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(path));
			
			Set<ValueContainerEdge<ConfigSet>> edgeSet = dependencyGraph.edgeSet();
			for (ValueContainerEdge<ConfigSet> valueContainerEdge : edgeSet) {
				DependencyNode edgeSrc = dependencyGraph.getEdgeSource(valueContainerEdge);
				DependencyNode edgeTgt = dependencyGraph.getEdgeTarget(valueContainerEdge);
				
				Object unitSrc = ((DependencyNodeWrapper<Unit>) edgeSrc).getData();
				Object unitTgt = ((DependencyNodeWrapper<Unit>) edgeTgt).getData();
				
				System.out.println(((DependencyNodeWrapper<Unit>) edgeSrc).getData() + " =" + valueContainerEdge.getValue() + "=> " + ((DependencyNodeWrapper<Unit>) edgeTgt).getData());
				
				writer.write(unitSrc + " =" + valueContainerEdge.getValue() + "=> " + unitTgt);
				writer.newLine();
			}
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String verifyFileExistance(String className, String pathDir, int i) {
		File dir = new File(pathDir);
		File[] files = dir.listFiles();
		
		pathDir += className + "-" + files.length + ".txt";
		return pathDir;
	}
	
	private static String settingDirectory(String className){
//		String path = "C:\\Users\\JEAN\\workspace-emergo\\emergo\\trunk\\Emergo - EI\\results\\"+ className + "-" + new Date().getDate() +"\\";
		String path = ".\\results\\"+ className + "-" + new Date().getDate() +"\\";
		File dir = new File(path);
		dir.mkdirs();
		
		return path;
	}
	
}
