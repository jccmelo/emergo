package br.ufpe.cin.emergo.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.stmt.Statement;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BlockTextSelection;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.ide.dialogs.InternalErrorDialog;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.jgrapht.DirectedGraph;

import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.core.DependencyFinder;
import br.ufpe.cin.emergo.core.SelectionPosition;
import br.ufpe.cin.emergo.graph.DependencyNode;
import br.ufpe.cin.emergo.graph.ValueContainerEdge;
import br.ufpe.cin.emergo.graph.transform.GraphTransformer;
import br.ufpe.cin.emergo.preprocessor.ContextManager;
import br.ufpe.cin.emergo.preprocessor.Tag;
import br.ufpe.cin.emergo.properties.SystemProperties;
import br.ufpe.cin.emergo.util.MethodDeclarationSootMethodBridge;
import br.ufpe.cin.emergo.util.ResourceUtil;
import br.ufpe.cin.emergo.util.SelectionNodesGroovyVisitor;
import br.ufpe.cin.emergo.util.SelectionNodesVisitor;
import br.ufpe.cin.emergo.views.EmergoGraphView;
import br.ufpe.cin.emergo.views.EmergoView;


/**
 * Handler for the br.ufal.cideei.commands.DoCompute extension command.
 * 
 * @author Társis Toledo
 * 
 */
public class GenerateEmergentInterfaceHandler extends AbstractHandler {
	
	public List<DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>>> dependencyGraphs = new ArrayList<DirectedGraph<DependencyNode,ValueContainerEdge<ConfigSet>>>();

	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
		Shell shell = HandlerUtil.getActiveShellChecked(event);

		// XXX Try block for debugging only.
		try {
			/*
			 * Mechanism for passing through information that could make the dependency finder easier/faster to
			 * implement.
			 */
			final Map<Object, Object> options = new HashMap<Object, Object>();

			if (!(selection instanceof ITextSelection))
				throw new ExecutionException("Not a textual selection");

			final ITextEditor editor = (ITextEditor) HandlerUtil.getActiveEditorChecked(event);
			final IFile textSelectionFile = (IFile) editor.getEditorInput().getAdapter(IFile.class);

			IDocumentProvider provider = editor.getDocumentProvider();
			IDocument document = provider.getDocument(editor.getEditorInput());
			ITextSelection textSelection = (ITextSelection) editor.getSite().getSelectionProvider().getSelection();

			if (textSelection.getLength() == -1) {
				new MessageDialog(shell, "Emergo Message", ResourceUtil.getEmergoIcon(), "The selection is invalid.", MessageDialog.WARNING, new String[] { "Ok" }, 0).open();
			}
			

			// The project that contains the file in which the selection happened.
			IProject project = textSelectionFile.getProject();

			/*
			 * Finds out the (partial) project's classpath as a list of Files. Each File either points to a source
			 * folder, or an archive like a jar.
			 */
			IJavaProject javaProject = JavaCore.create(project);

			options.put("interprocedural", SystemProperties.getInterprocedural(javaProject.getResource()));
			options.put("interprocedural-depth", SystemProperties.getInterproceduralDepth(javaProject.getResource()));
			options.put("interprocedural-inline", SystemProperties.getInterproceduralInline(javaProject.getResource()));
			options.put("rootpath", javaProject.getResource().getLocation().toFile().getAbsolutePath());
			
			options.put("featureDependence", SystemProperties.getFeatureDependence(javaProject.getResource()));

			IClasspathEntry[] resolvedClasspath = javaProject.getResolvedClasspath(true);
			List<File> classpath = new ArrayList<File>();
			for (IClasspathEntry cpEntry : resolvedClasspath) {
				switch (cpEntry.getEntryKind()) {
					case IClasspathEntry.CPE_CONTAINER:
						classpath.add(cpEntry.getPath().makeAbsolute().toFile());
						break;
					case IClasspathEntry.CPE_SOURCE:
						classpath.add(ResourcesPlugin.getWorkspace().getRoot().getFolder(cpEntry.getPath()).getLocation().toFile());
						break;
					case IClasspathEntry.CPE_LIBRARY:
						IPath ipath = makePathAbsolute(cpEntry.getPath());
						classpath.add(ipath.toFile());
						break;
				}
			}

			options.put("classpath", classpath);
			
			String correspondentClasspath = MethodDeclarationSootMethodBridge.getCorrespondentClasspath(textSelectionFile);
			options.put("correspondentClasspath", getBinPath(correspondentClasspath));
	        
			/*
			 * This instance of SelectionPosition holds the textual selection information that needs to br passed along
			 * to the underlying compiler infrastructure
			 */
			String selectionFileString = textSelectionFile.getLocation().toOSString();
			options.put("selectionFile", selectionFileString);
			options.put("textSelection", textSelection);
			
			List<SelectionPosition> selectionPositions = new ArrayList<SelectionPosition>();
			
			ContextManager context = ContextManager.getContext();
			context.setSrcfile(selectionFileString); // input class

			String fileExtension = "";
			
			IEditorInput editorInput = editor.getEditorInput();
			IJavaElement elem = JavaUI.getEditorInputJavaElement(editorInput);
			if (elem instanceof ICompilationUnit) {
			    ICompilationUnit unit = (ICompilationUnit) elem;
			    IJavaElement selected = unit.getElementAt(textSelection.getOffset());

			    // gets the file extension
			    fileExtension = selected.getResource().getFileExtension();
			    // sets in options to be used later
			    options.put("methodName", selected.getElementName());
			    options.put("fileExtension", fileExtension);
			    
				
				//====================================================================
				if (textSelection.getText().matches(Tag.ifdefRegex)) {
					selectionPositions = getSelectionPosFromFeatureWithinOneMethod(document, textSelection, selectionFileString, context);
				} else {
					SelectionPosition selectionPosition = SelectionPosition.builder().length(textSelection.getLength()).offSet(textSelection.getOffset()).startLine(textSelection.getStartLine()).startColumn(calculateColumnFromOffset(document, textSelection.getOffset())).endLine(textSelection.getEndLine()).endColumn(calculateColumnFromOffset(document, textSelection.getOffset() + textSelection.getLength())).filePath(selectionFileString).build();
					selectionPositions.add(selectionPosition);
				}
				
				for (SelectionPosition position : selectionPositions) {
					System.out.println("Position: "+position);
				}
				//====================================================================
			    
			    for (int i = 0; i < selectionPositions.size(); i++) {
			    	ITextSelection blockTextSelection = new BlockTextSelection(document, selectionPositions.get(i).getStartLine(), selectionPositions.get(i).getStartColumn(), selectionPositions.get(i).getEndLine(), selectionPositions.get(i).getEndColumn(), 0);
			    	
					if (fileExtension.equals("java")) {
						CommandCompilationUnit cu = new CompilationUnitJava();
						cu.markNodesFromSelection(textSelectionFile, blockTextSelection, options);

					} else { // groovy
						CommandCompilationUnit cuGroovy = new CompilationUnitGroovy();
						cuGroovy.markNodesFromSelection(textSelectionFile, blockTextSelection, options);

					}
			    	 
			    	dependencyGraphs.add(DependencyFinder.findFromSelection(selectionPositions.get(i), options));
				}
			    
			}
			
			/*
			 * There is not enough information on the graph to be shown. Instead, show an alert message to the user.
			 */
			if (!textSelection.getText().matches(Tag.ifdefRegex)) {
				for (int j = 0; j < dependencyGraphs.size(); j++) {
					DirectedGraph<DependencyNode,ValueContainerEdge<ConfigSet>> graph = dependencyGraphs.get(j);
					
					if (graph == null || graph.vertexSet().size() < 2) {
						// XXX cannot find path to icon!
						new MessageDialog(shell, "Emergo Message", ResourceUtil.getEmergoIcon(), "No dependencies found!", MessageDialog.INFORMATION, new String[] { "Ok" }, 0).open();
						// TODO clear the views!
						return null;
					}
				}
			}

			updateViews(event);
			
			dependencyGraphs.clear();
		} catch (Throwable e) {
			String message = e.getMessage() == null ? "No message specified" : e.getMessage();
			InternalErrorDialog internalErrorDialog = new InternalErrorDialog(shell, "An error has occurred", ResourceUtil.getEmergoIcon(), message, e, MessageDialog.ERROR, new String[] { "Ok", "Details" }, 0);
			internalErrorDialog.setDetailButton(1);
			internalErrorDialog.open();
			e.printStackTrace();
		}
		return null;
	}

	private void updateViews(ExecutionEvent event) {
		// TODO: make this a list of things to update instead of hardcoding.
		// Update the graph view
		IViewPart findGraphView = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(EmergoGraphView.ID);
		if (findGraphView instanceof EmergoGraphView) {
			final EmergoGraphView view = (EmergoGraphView) findGraphView;
			new Runnable() {
				public void run() {
					view.adaptTo2(dependencyGraphs);
				}
			}.run();
		}

		// Update the tree view.
		IViewPart treeView = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(EmergoView.ID);
		if (treeView instanceof EmergoView) {
			final EmergoView emergoView = (EmergoView) treeView;
			new Runnable() {
				public void run() {
					emergoView.adaptTo2(dependencyGraphs, true);
				}
			}.run();
		}
	}

	private List<SelectionPosition> getSelectionPosFromFeatureWithinOneMethod(IDocument document,
			ITextSelection textSelection, String selectionFileString, ContextManager context)
			throws FileNotFoundException, IOException {
		
		List<SelectionPosition> positions = new ArrayList<SelectionPosition>();
		
		BufferedReader br = new BufferedReader(new FileReader(context.getSrcfile()));
		Pattern pattern = Pattern.compile(Tag.regex, Pattern.CASE_INSENSITIVE);
		
		String rg = "(public|protected|private|static|\\s)+\\w+ +\\w+ *\\([^\\)]*\\) *\\{";//+[\\w<>[]]+\\s+(\\w+)*([^)]*)*({?|[^;])";
		
		int methodStartLine = getMethodStartLine(textSelection);
		
		String line;
		int lineNumber = 0;
		int startLine = 0;
		while ((line = br.readLine()) != null) {
			lineNumber++;
			
			if (methodStartLine < lineNumber) { // textSelection.getStartLine() < lineNumber
				
				/**
				 * Creates a matcher that will match the given input against
				 * this pattern.
				 */
				Matcher matcher = pattern.matcher(line);

				/**
				 * Matches the defined pattern with the current line
				 */
				if (matcher.matches()) {
					/**
					 * MatchResult is unaffected by subsequent operations
					 */
					MatchResult result = matcher.toMatchResult();
					// preprocessor directives
					String dir = result.group(1).toLowerCase();
					// feature's name
					String param = result.group(2);

					if (line.contains(Tag.IFDEF)) {
						// adds ifdef X on the stack
						context.addDirective(dir + " " + param.replaceAll("\\s", ""));
						startLine = lineNumber;

						continue;
					} else if (line.contains(Tag.ENDIF)) {
						String ifdef = context.getTopDirective();
						System.out.println("Top directive "+ifdef+" will be removed.");
						
						context.removeTopDirective();
						
						if (context.stackIsEmpty() && textSelection.getText().contains(ifdef)) {
							SelectionPosition selectionPosition = SelectionPosition.builder().length(textSelection.getLength()).offSet(textSelection.getOffset()).startLine(startLine).startColumn(calculateColumnFromOffset(document, textSelection.getOffset())).endLine(lineNumber).endColumn(calculateColumnFromOffset(document, textSelection.getOffset() + textSelection.getLength())).filePath(selectionFileString).build();
							positions.add(selectionPosition);
						}
						
						continue;
					} else if (line.contains(Tag.ELSE)) {
						context.addDirective(dir + " ~" + param.replaceAll("\\s", ""));
						startLine = lineNumber;

						continue;
					}
				} else {
					
					//XXX: This is necessary at the moment because the dataflow analysis is intraprocedural.
					if (line.matches(rg)) {
						break; //end method body
					}
				}
			}
		}
		br.close();
		return positions;
	}

	private int getMethodStartLine(ITextSelection textSelection) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(ContextManager.getContext().getSrcfile()));
		
		String rg = "(public|protected|private|static|\\s)+\\w+ +\\w+ *\\([^\\)]*\\) *\\{";//+[\\w<>[]]+\\s+(\\w+)*([^)]*)*({?|[^;])";
		
		String methodSignature = "";
		String line;
		int currentLine = 0;
		int startLine = 0;
		while ((line = br.readLine()) != null) {
			currentLine++;
			
			if (line.matches(rg) && currentLine <= textSelection.getStartLine()) {
				methodSignature = line;
				startLine = currentLine;
			}
		}
		
		return startLine;
	}

	private String getBinPath(String path) {
		path = path.replace("src", "bin");
		return path;
	}

	/**
	 * Calculates the column number of the {@code offset} in the {@code Document doc}
	 * 
	 * @param doc
	 * @param offset
	 * @return the column number
	 */
	public static int calculateColumnFromOffset(IDocument doc, int offset) {
		int sumpos = 0;
		int i = 0;
		try {
			while (sumpos + doc.getLineLength(i) - 1 < offset) {
				sumpos += doc.getLineLength(i);
				++i;
			}
		} catch (BadLocationException e) {
			// XXX What to do here?
			e.printStackTrace();
		}

		return offset - sumpos + 1;
	}

    private IPath makePathAbsolute(IPath path) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource workspaceResource = root.findMember(path);
        if (workspaceResource != null) {
            path = workspaceResource.getRawLocation();
        }
        return path;
    }

}