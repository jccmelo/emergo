package br.ufpe.cin.emergo.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.views.markers.MarkerItem;

import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.editor.IfDefJavaEditor;

public class HideFeaturesHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection) HandlerUtil.getActiveMenuSelection(event);
		Object marker = ((IStructuredSelection) selection).getFirstElement();
				
			try {
				
				if (marker instanceof MarkerItem) {
					
					/*
					 * Code which gets the document on the annotations will be placed.
					 */
					IfDefJavaEditor editor = (IfDefJavaEditor) HandlerUtil.getActiveEditor(event).getAdapter(IfDefJavaEditor.class);
					IDocument d = editor.getDocument();
					
					/*
					 * Get the file which will be analysed by the visitor.
					 */
					IFile file = (IFile) HandlerUtil.getActiveEditorChecked(event).getEditorInput().getAdapter(IFile.class);
					
					//TODO Francisco: instead of using Liborio's below code, we need to defined #ifdef blocks and then
					//create the features and featuresLineNumbers objects to pass to the Projection manager.
					
//					/*
//					 * Informations received from the selected market.
//					 */
//					String feature = (String) ((MarkerItem) marker).getMarker().getAttribute(IMarker.TEXT);
					
					ConfigSet configuration = (ConfigSet) ((MarkerItem) marker).getMarker().getAttribute(IMarker.TEXT);
					
					System.out.println("===> " + configuration);

//					/*
//					 * This visitor selects the features that will be collapsed.
//					 */
//					Set<String> configuration = this.stringToSet(feature);
//					SupplementaryConfigurationVisitor supplementaryConfigurationVisitor = new SupplementaryConfigurationVisitor(configuration,file);
//					
//					ICompilationUnit compilationUnit = JavaCore.createCompilationUnitFrom(file);
//					ASTParser parser = ASTParser.newParser(AST.JLS3);
//					parser.setSource(compilationUnit);
//					parser.setKind(ASTParser.K_COMPILATION_UNIT);
//					parser.setResolveBindings(true);
//					CompilationUnit jdtCompilationUnit = (CompilationUnit) parser.createAST(null);
//					
//					jdtCompilationUnit.accept(supplementaryConfigurationVisitor);
//					HashMap<String,Set<ASTNode>> featureLines = supplementaryConfigurationVisitor.getFeatureLines();
//					
//					/*
//					 * According to the visitor's results, the annotations will be created and added to the document.
//					 */
//					Set<String> features = supplementaryConfigurationVisitor.getFeatureNames();
//
//					Iterator<String> featureNames = features.iterator();
//					
//					HashMap<String, TreeSet<Integer>> featuresLineNumbers = convertFromNodesToLines(
//							jdtCompilationUnit, featureLines, featureNames);

					//TODO Temporary objects (features and featuresLineNumbers):
					Set<String> features = new HashSet<String>();
					features.add("COPY");
					
					Map<String, Set<Integer>> featuresToLineNumbers;
					
					Set<Integer> lines = new TreeSet<Integer>();
					lines.add(new Integer(20));
					lines.add(new Integer(21));
					lines.add(new Integer(22));
					lines.add(new Integer(23));
					lines.add(new Integer(24));
					
					featuresToLineNumbers = new HashMap<String, Set<Integer>>();
					featuresToLineNumbers.put("COPY", lines);
					//TODO (end)
					
					ArrayList<Position> positions = createPositions(d, features, featuresToLineNumbers);
					
					/*
					 * The action which updates the editor to show the folding areas.
					 */
					ArrayList<Position> positionsEmpty = new ArrayList<Position>();
					
					if (editor instanceof IfDefJavaEditor) {
						((IfDefJavaEditor) editor).expandAllAnnotations(d.getLength());
						((IfDefJavaEditor) editor).removeAllAnnotations();
						
						((IfDefJavaEditor) editor).updateFoldingStructure(positionsEmpty);
						((IfDefJavaEditor) editor).updateFoldingStructure(positions);
					}
				}
			
//			} catch (CoreException e) {
//				e.printStackTrace();
			} catch (BadLocationException e1) {
				e1.printStackTrace();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
			
		return null;
	}
	
	private ArrayList<Position> createPositions(
			IDocument d, Set<String> features, Map<String, Set<Integer>> featuresLineNumbers)
					throws BadLocationException {
		
		Iterator<String> featureNames;
		String featureName;
		Set<Integer> lines;
		ArrayList<Position> positions = new ArrayList<Position>();
		
		Iterator<Integer> iteratorInteger = null;
		int line = 0;
		int previousLine = 0;
		int length = 0;
		int offset = 0;
		boolean newAnnotation = false;
		boolean first = true;
		featureNames = features.iterator();
		
		while (featureNames.hasNext()) {
			
			line = 0;
			previousLine = 0;
			length = 0;
			offset = 0;
			first = true;
			
			featureName = featureNames.next();
			lines = featuresLineNumbers.get(featureName);
			
			if (lines.size() > 1) {
			
				iteratorInteger = lines.iterator();
										
				while (iteratorInteger.hasNext()) {
					if (newAnnotation == true) {
						try {
							offset = d.getLineOffset(line - 1);
							length = d.getLineLength(line - 1);
						} catch(BadLocationException e) {
							length = 0;
							offset = d.getLineOffset(line);
						}
						//offset = d.getLineOffset(line);
						newAnnotation = false;
					} else {
						line = iteratorInteger.next().intValue() - 1;
						if (first == true) {
							try {
								offset = d.getLineOffset(line - 1);
								length = d.getLineLength(line - 1);
							} catch (BadLocationException e) {
								length = 0;
								offset = d.getLineOffset(line);
							}
							//offset = d.getLineOffset(line);
							first = false;
						}
					}
					length = length + d.getLineLength(line);
					if (previousLine > 0 && line > previousLine + 1) {
						previousLine = line;
						newAnnotation = true;
						positions.add(new Position(offset,length));
						break;
					}
					previousLine = line;							
				}
				positions.add(new Position(offset,length));
			}
		}
		return positions;
	}

	private HashMap<String, TreeSet<Integer>> convertFromNodesToLines(
			CompilationUnit jdtCompilationUnit,
			HashMap<String, Set<ASTNode>> featureLines,
			Iterator<String> featureNames) {
		
		ASTNode node = null;
		String featureName = null;
		Set<Integer> lines = null;
		Iterator<ASTNode> iteratorNodes = null;
		Set<ASTNode> nodes = null;
		HashMap<String, TreeSet<Integer>> featuresLineNumbers = new HashMap<String, TreeSet<Integer>>();
		
		while (featureNames.hasNext()) {
			
			featureName = featureNames.next();
			nodes = featureLines.get(featureName);
			lines = new TreeSet<Integer>();
			iteratorNodes = nodes.iterator();
			
			while (iteratorNodes.hasNext()) {
				node = iteratorNodes.next();
				lines.add(new Integer(jdtCompilationUnit.getLineNumber(node.getStartPosition())));
			}
			
			featuresLineNumbers.put(featureName, (TreeSet<Integer>) lines);
		}
		return featuresLineNumbers;
	}
	
	private Set<String> stringToSet(String markerConfigColumn) {
		
		String feature = markerConfigColumn.substring(1, markerConfigColumn.length() - 1);
		String[] featuresArray = feature.split(",");
		Set<String> configuration = new HashSet<String>();
		for (String string : featuresArray) {
			configuration.add(string);
		}
		return configuration;
	}
	
}