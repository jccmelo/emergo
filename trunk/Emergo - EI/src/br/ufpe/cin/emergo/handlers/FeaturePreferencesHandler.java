package br.ufpe.cin.emergo.handlers;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import br.ufpe.cin.emergo.popup.SampleDialog;
import br.ufpe.cin.emergo.properties.SystemProperties;

public class FeaturePreferencesHandler extends AbstractHandler {
	   
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Shell s = HandlerUtil.getActiveShellChecked(event);
		ITextEditor editor = (ITextEditor) HandlerUtil.getActiveEditorChecked(event);
		IFile textSelectionFile = (IFile) editor.getEditorInput().getAdapter(IFile.class);
		IProject project = textSelectionFile.getProject();

		/*
		 * Finds out the (partial) project's classpath as a list of Files. Each File either points to a source
		 * folder, or an archive like a jar.
		 */
		IJavaProject javaProject = JavaCore.create(project);
		// For a test Purpose
		try {
			String[] features = this.getFeaturesFromFile(project);
			String choosenFeatures = javaProject.getResource().getPersistentProperty(SystemProperties.CHOOSEN_FEATURES);
			if(choosenFeatures ==null) choosenFeatures = ";";
			String [] choosenFeaturesArray = choosenFeatures.split(";");
			SampleDialog sp = new SampleDialog(s, features, choosenFeaturesArray);
			sp.open();
			int returnCode = sp.getReturnCode();
			if(returnCode==1){
				
			}else if(returnCode==9998){//cancel
				
			}else if(returnCode == 9999){ // save
				features = sp.getSelectedFeatures();
				String allFeatures = "";
				for (int i = 0; i < features.length; i++) {
					allFeatures= allFeatures + features[i]+";";
				}
				javaProject.getResource().setPersistentProperty(SystemProperties.CHOOSEN_FEATURES, allFeatures);
				//IWorkbenchPage page = getSite().getPage();
    			//IEditorPart part = page.getActiveEditor();
    			// if (!(part instanceof AbstractTextEditor))
    			//	      return;
    			 //ITextEditor editor = (ITextEditor)part;
    			   IDocumentProvider dp = editor.getDocumentProvider();
    			   IDocument doc = dp.getDocument(editor.getEditorInput());
    			   IRegion lineInfo;
    			   lineInfo = null;
    			try {
    				int line = 1;
					lineInfo = doc.getLineInformation(line - 1);
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
    			   if(lineInfo!= null){
    				   editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
    			   }
    		
			}
	
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private String[] getFeaturesFromFile(IProject project) {
		ArrayList<String> features = new ArrayList<String>();
		
		String rootpath = (String) project.getPathVariableManager().getURIValue("PROJECT_LOC").toASCIIString();
		rootpath = rootpath.substring(6);
		File ifdefSpecFile = new File(rootpath + File.separator + "ifdef.txt");
		try {
			if (!ifdefSpecFile.exists()) {
				throw new RuntimeException(
						"The ifdef.txt of the project was not found at " + rootpath);
			}
			FileInputStream ifdeffile = new FileInputStream(ifdefSpecFile);
			DataInputStream ifdefData = new DataInputStream(ifdeffile);
			BufferedReader ifdefReader = new BufferedReader(new InputStreamReader(ifdefData));
			String strLine; 
			
			int stage = 0;
			while ((strLine = ifdefReader.readLine()) != null)   {
				  // Print the content on the console
				  if(strLine.trim().equals("features = {")){
					  stage = 1;
					  continue;
				  }else if(strLine.trim().equals("}")){
					  stage = 2;
					  continue;
				  }
				  if(stage==1){
					  features.add(strLine.trim());
				  }
				  
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] result = new String[features.size()];
		for (int i = 0; i < features.size(); i++) {
			result[i] = features.get(i);
		}
		return result;
	}

}