package br.ufpe.cin.emergo.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.ide.dialogs.InternalErrorDialog;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.jgrapht.DirectedGraph;

import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.core.DependencyFinder;
import br.ufpe.cin.emergo.core.DependencyFinderID;
import br.ufpe.cin.emergo.core.EmergoException;
import br.ufpe.cin.emergo.core.SelectionPosition;
import br.ufpe.cin.emergo.graph.DependencyNode;
import br.ufpe.cin.emergo.graph.ValueContainerEdge;
import br.ufpe.cin.emergo.properties.SystemProperties;
import br.ufpe.cin.emergo.util.ResourceUtil;
import br.ufpe.cin.emergo.views.LineOfCode;
import br.ufpe.cin.emergo.views.MarkedLinesView;
import br.ufpe.cin.emergo.views.TestView;

public class SelectLinesHandler extends AbstractHandler {

	public static String chooseID = "br.ufpe.cin.emergo.command.chooseLines";
	public static String generateFromID = "br.ufpe.cin.emergo.command.generateForLines";

	static List<LineOfCode> linesOffset;
	static List<Map<Object, Object>> allOptions;
	static boolean interprocedural = true;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		emergoHandlerMethod(event);
		return trialMethod(event);
	}

	private Object emergoHandlerMethod(ExecutionEvent event)
			throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
		Shell shell = HandlerUtil.getActiveShellChecked(event);

		// XXX Try block for debugging only.
		try {
			/*
			 * Mechanism for passing through information that could make the
			 * dependency finder easier/faster to implement.
			 */
			if (allOptions == null) {
				allOptions = new ArrayList<Map<Object, Object>>();
			}
			if (linesOffset == null) {
				linesOffset = new ArrayList<LineOfCode>();
			}
			final Map<Object, Object> options = new HashMap<Object, Object>();

			if (!(selection instanceof ITextSelection))
				throw new ExecutionException("Not a textual selection");

			ITextEditor editor = (ITextEditor) HandlerUtil
					.getActiveEditorChecked(event);
			IFile textSelectionFile = (IFile) editor.getEditorInput()
					.getAdapter(IFile.class);

			IDocumentProvider provider = editor.getDocumentProvider();
			IDocument document = provider.getDocument(editor.getEditorInput());
			ITextSelection textSelection = (ITextSelection) editor.getSite()
					.getSelectionProvider().getSelection();

			if (textSelection.getLength() == -1) {
				new MessageDialog(shell, "Emergo Message",
						ResourceUtil.getEmergoIcon(),
						"The selection is invalid.", MessageDialog.WARNING,
						new String[] { "Ok" }, 0).open();
			}

			// The project that contains the file in which the selection
			// happened.
			IProject project = textSelectionFile.getProject();

			/*
			 * Finds out the (partial) project's classpath as a list of Files.
			 * Each File either points to a source folder, or an archive like a
			 * jar.
			 */
			IJavaProject javaProject = JavaCore.create(project);
			
			if (javaProject.getResource().getPersistentProperty(SystemProperties.INTERPROCEDURAL_PROPKEY) != null) {
				interprocedural = javaProject.getResource().getPersistentProperty(
						SystemProperties.INTERPROCEDURAL_PROPKEY).toString().equals("true");
			} else {
				interprocedural = false;
			}
			
			options.put("rootpath", javaProject.getResource().getLocation()
					.toFile().getAbsolutePath());
			IClasspathEntry[] resolvedClasspath = javaProject
					.getResolvedClasspath(true);
			List<File> classpath = new ArrayList<File>();
			for (IClasspathEntry cpEntry : resolvedClasspath) {
				switch (cpEntry.getEntryKind()) {
				case IClasspathEntry.CPE_CONTAINER:
					classpath.add(cpEntry.getPath().makeAbsolute().toFile());
					break;
				case IClasspathEntry.CPE_SOURCE:
					classpath.add(ResourcesPlugin.getWorkspace().getRoot()
							.getFolder(cpEntry.getPath()).getLocation()
							.toFile());
					break;
				case IClasspathEntry.CPE_LIBRARY:
					IPath ipath = makePathAbsolute(cpEntry.getPath());
					classpath.add(ipath.toFile());
					break;
				}
			}

			if (event.getCommand().getId().equals(SelectLinesHandler.chooseID)) {
				options.put("classpath", classpath);
				allOptions.add(options);

				/*
				 * This instance of SelectionPosition holds the textual
				 * selection information that needs to br passed along to the
				 * underlying compiler infrastructure
				 */
				if (this.linesOffset == null) {
					System.out.println("es ist immer null");
					this.linesOffset = new ArrayList<LineOfCode>();
				}
				LineOfCode lineArguments = new LineOfCode(textSelection,
						textSelectionFile);
				linesOffset.add(lineArguments);
			}

			if (event.getCommand().getId().equals(SelectLinesHandler.generateFromID)) {
				callDependendyGraphs(event, options, editor, document);
			}
			/*
			 * There is not enough information on the graph to be shown.
			 * Instead, show an alert message to the user.
			 */
			/*
			 * if (dependencyGraph == null || dependencyGraph.vertexSet().size()
			 * < 2) { // XXX cannot find path to icon! new MessageDialog(shell,
			 * "Emergo Message", ResourceUtil.getEmergoIcon(),
			 * "No dependencies found!", MessageDialog.INFORMATION, new String[]
			 * { "Ok" }, 0).open(); // TODO clear the views! return null; }
			 */

		} catch (Throwable e) {
			String message = e.getMessage() == null ? "No message specified"
					: e.getMessage();
			InternalErrorDialog internalErrorDialog = new InternalErrorDialog(
					shell, "An error has occurred",
					ResourceUtil.getEmergoIcon(), message, e,
					MessageDialog.ERROR, new String[] { "Ok", "Details" }, 0);
			internalErrorDialog.setDetailButton(1);
			internalErrorDialog.open();
			e.printStackTrace();
		}
		return null;
	}

	public static void callDependendyGraphs(ExecutionEvent event,
			final Map<Object, Object> options, ITextEditor editor,
			IDocument document) throws EmergoException {
		List<DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>>> dependencyGraphs = new ArrayList<DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>>>();
		for (int i = 0; i < linesOffset.size(); i++) {
			ITextSelection textSelection = linesOffset.get(i)
					.getTextSelection();
			System.out.println("------------> Es ist nicht null"
					+ linesOffset.get(i));
			String selectionFileString = linesOffset.get(i)
					.getTextSelectionFile().getLocation().toOSString();
			final SelectionPosition selectionPosition = SelectionPosition
					.builder()
					.length(textSelection.getLength())
					.offSet(textSelection.getOffset())
					.startLine(textSelection.getStartLine())
					.startColumn(
							GenerateEmergentInterfaceHandler.calculateColumnFromOffset(document,
									textSelection.getOffset()))
					.endLine(textSelection.getEndLine())
					.endColumn(
							GenerateEmergentInterfaceHandler.calculateColumnFromOffset(
									document,
									textSelection.getOffset()
											+ textSelection.getLength()))
					.filePath(selectionFileString).build();
			DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> dependencyGraph = DependencyFinder
					.findFromSelection(DependencyFinderID.JWCOMPILER,
							selectionPosition, allOptions.get(i),
							interprocedural);
			dependencyGraphs.add(dependencyGraph);

			updateViews(event, editor, linesOffset.get(i)
					.getTextSelectionFile(), dependencyGraph);
		}
	}

	private static void updateViews(ExecutionEvent event, ITextEditor editor, IFile textSelectionFile,
			DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> dependencyGraph) {
		// TODO: make this a list of things to update instead of hardcoding.
		// Update the graph view
		/*
		 * IViewPart findGraphView =
		 * HandlerUtil.getActiveWorkbenchWindow(event).
		 * getActivePage().findView(GraphView.ID); if (findGraphView instanceof
		 * GraphView) { GraphView view = (GraphView) findGraphView;
		 * view.adaptTo(dependencyGraph, editor, selectionPosition); }
		 */

		// Update the tree view.

		IViewPart testView = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().findView(TestView.ID);
		// for (DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>>
		// dependencyGraph : dependencyGraphs) {
		TestView.adaptTo(dependencyGraph, editor, textSelectionFile, false);
		// }
		((TestView) testView).updateTree();

		// Updates Line view
		IViewPart markedLinesView = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().findView(MarkedLinesView.ID);
		((MarkedLinesView) markedLinesView).update(linesOffset);
	}

	private Object trialMethod(ExecutionEvent event) throws ExecutionException {

		final Shell s = HandlerUtil.getActiveShellChecked(event);
		ITextEditor editor = (ITextEditor) HandlerUtil
				.getActiveEditorChecked(event);
		IFile textSelectionFile = (IFile) editor.getEditorInput().getAdapter(
				IFile.class);
		IDocumentProvider provider = editor.getDocumentProvider();
		IDocument document = provider.getDocument(editor.getEditorInput());
		ITextSelection textSelection = (ITextSelection) editor.getSite()
				.getSelectionProvider().getSelection();

		System.out.println("Begin of execution");
		System.out.println(textSelection.getText());

		System.out.println("End of execution");
		IProject project = textSelectionFile.getProject();

		// linesOffset.add(new LineOfCode(textSelection, textSelectionFile));
		editor.getTitle();
		// Updates Line view
		IViewPart markedLinesView = HandlerUtil.getActiveWorkbenchWindow(event)
				.getActivePage().findView(MarkedLinesView.ID);
		((MarkedLinesView) markedLinesView).update(linesOffset);
		return null;
	}

	public Map getLines() {
		return this.getLines();
	}

	public static void deleteAllMarkers() {
		allOptions = new ArrayList<Map<Object, Object>>();
		linesOffset = new ArrayList<LineOfCode>();
	}

	public static void deleteMarkers(String message) {
		List<Map<Object, Object>> allOptionsAux = allOptions;
		for (Map<Object, Object> map : allOptionsAux) {
			System.out.println(map);
		}
		int removePosition = 0;
		boolean found = false;
		for (int i = 0; i < linesOffset.size(); i++) {
			String one = linesOffset.get(i).toString().trim();
			if (linesOffset.get(i).toString().trim().equals(message)) {
				removePosition = i;
				found = true;
			}
		}
		if (found) {
			linesOffset.remove(removePosition);
			allOptions.remove(removePosition);
		}
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
