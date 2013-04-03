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
	
	public static Set<ASTNode> selectionNodes;
	public static CompilationUnit jdtCompilationUnit;
	public static Statement stmt;
	

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
			
			SelectionPosition selectionPosition = null;
			
			ContextManager context = ContextManager.getContext();
			context.setSrcfile(selectionFileString); // input class
			
			//====================================================================
			
			if (textSelection.getText().matches(Tag.ifdefRegex)) {
				selectionPosition = getSelectionPosFromFeature(document, textSelection, selectionFileString, selectionPosition,	context);
			} else {
				selectionPosition = SelectionPosition.builder().length(textSelection.getLength()).offSet(textSelection.getOffset()).startLine(textSelection.getStartLine()).startColumn(calculateColumnFromOffset(document, textSelection.getOffset())).endLine(textSelection.getEndLine()).endColumn(calculateColumnFromOffset(document, textSelection.getOffset() + textSelection.getLength())).filePath(selectionFileString).build();
			}
			
			ITextSelection blockTextSelection = new BlockTextSelection(document, selectionPosition.getStartLine(), selectionPosition.getStartColumn(), selectionPosition.getEndLine(), selectionPosition.getEndColumn(), 0);
			
			//====================================================================
			
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
			    
			    if(fileExtension.equals("java")){
					SelectionNodesVisitor selectionNodesVisitor = new SelectionNodesVisitor(blockTextSelection);
					jdtCompilationUnit = GraphTransformer.getCompilationUnit(textSelectionFile);

			        jdtCompilationUnit.accept(selectionNodesVisitor);
			        selectionNodes = selectionNodesVisitor.getNodes();

			        for (ASTNode astNode : selectionNodes) {
			        	GraphTransformer.lineNumbers.add(jdtCompilationUnit.getLineNumber(astNode.getStartPosition()));
			        }
			        
			        options.put("selectionNodes", selectionNodes);
			        
				} else { // groovy
					SelectionNodesGroovyVisitor selectionNodesVisitor = new SelectionNodesGroovyVisitor(blockTextSelection);
					
					MethodNode methodNode = GraphTransformer.getGroovyCompilationUnit(textSelectionFile, options);
					stmt = methodNode.getCode();
					selectionNodesVisitor.visitStatement(stmt);
					
			        Set<org.codehaus.groovy.ast.ASTNode> nodes = selectionNodesVisitor.getNodes();
			        for (org.codehaus.groovy.ast.ASTNode astNode : nodes) {
			        	GraphTransformer.lineNumbers.add(astNode.getLineNumber());
			        }
			        
			        options.put("selectionNodes", nodes);
				}
			    
			}
			options.put("unitsInSelection", textSelection.getText());
			
//			String[] split = textSelection.getText().replaceAll("//[^\r\n]+", "").replaceAll("/\\*.*\\*/", "").split("\n");
//			for (int i = 0; i < split.length; i++) {
//				if (!split[i].trim().matches("^\\s*")) {
//					System.out.println(split[i].trim());
//				}
//			}
			
			
			final DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> dependencyGraph = DependencyFinder.findFromSelection(selectionPosition, options);

			/*
			 * There is not enough information on the graph to be shown. Instead, show an alert message to the user.
			 */
			if (dependencyGraph == null || dependencyGraph.vertexSet().size() < 2) {
				// XXX cannot find path to icon!
				new MessageDialog(shell, "Emergo Message", ResourceUtil.getEmergoIcon(), "No dependencies found!", MessageDialog.INFORMATION, new String[] { "Ok" }, 0).open();
				// TODO clear the views!
				return null;
			}

			// TODO: make this a list of things to update instead of hardcoding.
			// Update the graph view
			IViewPart findGraphView = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(EmergoGraphView.ID);
			if (findGraphView instanceof EmergoGraphView) {
				final EmergoGraphView view = (EmergoGraphView) findGraphView;
				new Runnable() {
					public void run() {
						view.adaptTo(dependencyGraph);
					}
				}.run();
			}

			// Update the tree view.
			IViewPart treeView = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(EmergoView.ID);
			if (treeView instanceof EmergoView) {
				final EmergoView emergoView = (EmergoView) treeView;
				new Runnable() {
					public void run() {
						emergoView.adaptTo(dependencyGraph, true);
					}
				}.run();
			}
			
		} catch (Throwable e) {
			String message = e.getMessage() == null ? "No message specified" : e.getMessage();
			InternalErrorDialog internalErrorDialog = new InternalErrorDialog(shell, "An error has occurred", ResourceUtil.getEmergoIcon(), message, e, MessageDialog.ERROR, new String[] { "Ok", "Details" }, 0);
			internalErrorDialog.setDetailButton(1);
			internalErrorDialog.open();
			e.printStackTrace();
		}
		return null;
	}

	private SelectionPosition getSelectionPosFromFeature(IDocument document,
			ITextSelection textSelection, String selectionFileString,
			SelectionPosition selectionPosition, ContextManager context)
			throws FileNotFoundException, IOException {
		BufferedReader br = new BufferedReader(new FileReader(context.getSrcfile()));
		Pattern pattern = Pattern.compile(Tag.regex, Pattern.CASE_INSENSITIVE);
		
		String line;
		int lineNumber = -1;
		while ((line = br.readLine()) != null) {
			lineNumber++;
			/**
			 * Creates a matcher that will match the given input against
			 * this pattern.
			 */
			Matcher matcher = pattern.matcher(textSelection.getText());

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

				
				if (textSelection.getStartLine() <= lineNumber) {
					if (line.contains(Tag.IFDEF)) {
						// adds ifdef X on the stack
						context.addDirective(dir + " "+ param.replaceAll("\\s", ""));
						
						continue;
					} else if (line.contains(Tag.ENDIF)) {
						
						context.removeTopDirective();
						
						if (context.stackIsEmpty()) {
							selectionPosition = SelectionPosition.builder().length(textSelection.getLength()).offSet(textSelection.getOffset()).startLine(textSelection.getStartLine()).startColumn(calculateColumnFromOffset(document, textSelection.getOffset())).endLine(lineNumber).endColumn(calculateColumnFromOffset(document, textSelection.getOffset() + textSelection.getLength())).filePath(selectionFileString).build();
							break;
						}
						
						continue;
					} else {
						if (!line.trim().matches("^\\s*")) {
							System.out.println(line.trim()); //set selectionPosition
						}
					}
				}
			}
		}
		br.close();
		return selectionPosition;
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

	public static Set<ASTNode> getSelectionNodes() {
		return selectionNodes;
	}

	public static CompilationUnit getJdtCompilationUnit() {
		return jdtCompilationUnit;
	}

}