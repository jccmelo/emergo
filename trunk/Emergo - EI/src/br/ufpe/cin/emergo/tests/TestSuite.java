package br.ufpe.cin.emergo.tests;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BlockTextSelection;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;

import br.ufpe.cin.emergo.core.DependencyFinder;
import br.ufpe.cin.emergo.core.EmergoException;
import br.ufpe.cin.emergo.core.SelectionPosition;
import br.ufpe.cin.emergo.handlers.CommandCompilationUnit;
import br.ufpe.cin.emergo.handlers.CompilationUnitGroovy;
import br.ufpe.cin.emergo.preprocessor.ContextManager;
import br.ufpe.cin.emergo.util.Diff;

public class TestSuite {

	/**
	 * This method tests if the results returned 
	 * by the tool are correct.
	 * 
	 * @param originalDir - where the correct results are.
	 * @param newDir - where the new results are.
	 */
	public void doTest(String originalDir, String newDir) {
		String dirname = "/Users/paolaaccioly/Documents/Working Copies/emergo/trunk/Emergo - EI/results/";

		File dir = new File(dirname + originalDir +"/");
		File anotherDir = new File(dirname + newDir +"/");

		if (dir.exists()) {
			// compares the result with original data
			// dir = original data

			String s[] = dir.list();
			String anotherStr[] = anotherDir.list();
			
			for (int i = 0; i < s.length; i++) {
				// gets original result and new result
				File oldFile = new File(dirname + originalDir +"/" + s[i]);
				
				
				for (int j = 0; j < anotherStr.length; j++) {
					File newFile = new File(dirname + newDir +"/" + s[j]);
					
					if (oldFile.getName().equals(newFile.getName())) {
						System.out.println("Testing analysis "+oldFile.getName());
						// tests if there is diff between the files
						String diff = Diff.getDiff(oldFile, newFile);
						if (diff.isEmpty()){
							System.out.println("Sucessfully!");
						} else { 
							System.out.println("Ops! Error occurred in the analysis "+oldFile.getName());
							System.out.println("DIFF:\n\n"+diff);
						}
					}
				}

				
			}
		}
	}
	
	public static void main(String[] args) {
		TestSuite suite = new TestSuite();
//		suite.doTest(originalDir, newDir);
		
		String filePath = "\\Users\\paolaaccioly\\Documents\\runtime-EclipseApplication\\Test-1.0\\src\\Test1.groovy";
		
		ContextManager context = ContextManager.getContext();
		context.setSrcfile(filePath);
		
		SelectionPosition selectionPosition = SelectionPosition.builder().length(12).offSet(12).startLine(5).startColumn(3).endLine(5).endColumn(15).filePath(filePath).build();
		final Map<Object, Object> options = new HashMap<Object, Object>();
		
		IPath path = new Path(filePath);
		IDocument document = new Document(filePath);
		
//		IWorkspace workspace = ResourcesPlugin.getWorkspace();
//		IFile textSelectionFile = workspace.getRoot().getFileForLocation(path);
		
//		IWorkspace ws = ResourcesPlugin.getWorkspace();
//		IProject project = ws.getRoot().getProject("Test-1.0");
//		if (!project.exists())
//			try {
//				project.create(null);
//			} catch (CoreException e1) {
//				e1.printStackTrace();
//			}
//		if (!project.isOpen())
//			try {
//				project.open(null);
//			} catch (CoreException e1) {
//				e1.printStackTrace();
//			}
//
//		IPath location = new Path(filePath);
//		IFile file = project.getFile(location.lastSegment());
		
		IFileStore fileStore;
		try {
			fileStore = EFS.getStore(new File(filePath).toURI());
			System.out.println(fileStore.getName());
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		
		
		ITextSelection blockTextSelection = new BlockTextSelection(document, selectionPosition.getStartLine(), selectionPosition.getStartColumn(), selectionPosition.getEndLine(), selectionPosition.getEndColumn(), 0);
    	
		
		options.put("correspondentClasspath", "\\Users\\paolaaccioly\\Documents\\runtime-EclipseApplication\\Test-1.0\\bin");
		options.put("fileExtension", "groovy");
		options.put("methodName", "main");
		
		CommandCompilationUnit cuGroovy = new CompilationUnitGroovy();
//		cuGroovy.markNodesFromSelection(textSelectionFile, blockTextSelection, options);
		
		
//		try {
//			DependencyFinder.findFromSelection(selectionPosition, options);
//		} catch (EmergoException e) {
//			e.printStackTrace();
//		}
		
		//TODO salva o resultado do grafo de dependencia num .txt
		
		//TODO pego esse arquivo gerado e comparo com o original(certo)
		
	}
}
