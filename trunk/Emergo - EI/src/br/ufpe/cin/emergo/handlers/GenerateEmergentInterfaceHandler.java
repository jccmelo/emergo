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
import org.eclipse.jface.text.BadLocationException;
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
import br.ufpe.cin.emergo.core.SelectionPosition;
import br.ufpe.cin.emergo.graph.DependencyNode;
import br.ufpe.cin.emergo.graph.ValueContainerEdge;
import br.ufpe.cin.emergo.properties.SystemProperties;
import br.ufpe.cin.emergo.util.ResourceUtil;
import br.ufpe.cin.emergo.views.EmergoGraphView;
import br.ufpe.cin.emergo.views.EmergoView;

/**
 * Handler for the br.ufal.cideei.commands.DoCompute extension command.
 * 
 * @author T�rsis
 * 
 */
public class GenerateEmergentInterfaceHandler extends AbstractHandler {

	private static boolean interprocedural = true;
	
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

			if (javaProject.getResource().getPersistentProperty(SystemProperties.INTERPROCEDURAL_PROPKEY) != null) {
				interprocedural = javaProject.getResource().getPersistentProperty(
						SystemProperties.INTERPROCEDURAL_PROPKEY).toString().equals("true");
			} else {
				interprocedural = false;
			}
			options.put("rootpath", javaProject.getResource().getLocation().toFile().getAbsolutePath());

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

			/*
			 * This instance of SelectionPosition holds the textual selection information that needs to br passed along
			 * to the underlying compiler infrastructure
			 */
			String selectionFileString = textSelectionFile.getLocation().toOSString();
			final SelectionPosition selectionPosition = SelectionPosition.builder().length(textSelection.getLength()).offSet(textSelection.getOffset()).startLine(textSelection.getStartLine()).startColumn(calculateColumnFromOffset(document, textSelection.getOffset())).endLine(textSelection.getEndLine()).endColumn(calculateColumnFromOffset(document, textSelection.getOffset() + textSelection.getLength())).filePath(selectionFileString).build();

			final DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> dependencyGraph = DependencyFinder.findFromSelection(DependencyFinderID.JWCOMPILER, selectionPosition, options, interprocedural);

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