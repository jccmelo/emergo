package br.ufpe.cin.emergo.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;

import br.ufpe.cin.emergo.preprocessor.ContextManager;

public class GraphResult {

	/**
	 * This method saves the dependency graph in text file.
	 * 
	 * @param graph
	 */
	public static void saveResult(Graph graph) {
		
		String className = retrieveClassName(ContextManager.getContext().getSrcfile());

		String path = settingDirectory(className) + className + "-" + new Date().getTime() + ".txt";

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(path));
			
			// Iterate over the graph
			List<GraphConnection> graphConnections = graph.getConnections();
			
			System.out.println("Dependency graph:");
			for (GraphConnection connection : graphConnections) {
				System.out.println(connection.getSource().getText() + " =" + connection.getText() + "=> " + connection.getDestination().getText());
				
				writer.write(connection.getSource().getText() + " =" + connection.getText() + "=> " + connection.getDestination().getText());
				writer.newLine();
			}
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String settingDirectory(String className){
		//XXX fix that!
		String path = "/Users/paolaaccioly/Documents/Working Copies/emergo/trunk/Emergo - EI/results/"+ className +"/";
		
		File dir = new File(path);
		dir.mkdirs();
		
//		if (!dir.exists()) {
//			path = "../Preprocessor4SPL/results/"+ className + new Date().getDate() +"/";
//			
//			dir = new File(path);
//			dir.mkdirs();
//		}
		
		return path;
	}
	
	/**
     * This method retrieves the classname through
     * attribute classCompleteName.
     * @return the classname
     */
    private static String retrieveClassName(String completeName) {
    	//Getting all parts of the path+classname+extension
    	String[] parts = completeName.split("/");
    	//Getting the classname without extension as well as path
    	String className = parts[parts.length-1].split("\\.")[0];
    	
    	return className;
	}
}
