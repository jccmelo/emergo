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
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.ide.dialogs.InternalErrorDialog;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import br.ufpe.cin.emergo.properties.SystemProperties;
import br.ufpe.cin.emergo.util.ResourceUtil;

public abstract class AbstractEmergoHandler extends AbstractHandler {

	private static boolean interprocedural = true;

	/*
	 * Mechanism for passing through information that could make the dependency finder easier/faster to
	 * implement.
	 */
	private final Map<Object, Object> options = new HashMap<Object, Object>();

	protected void handleSelection(ExecutionEvent event) throws ExecutionException {

		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
		Shell shell = HandlerUtil.getActiveShellChecked(event);

		// XXX Try block for debugging only.
		try {
	
			if (!(selection instanceof ITextSelection))
				throw new ExecutionException("Not a textual selection");
	
			ITextEditor editor = (ITextEditor) HandlerUtil.getActiveEditorChecked(event);
			IFile textSelectionFile = (IFile) editor.getEditorInput().getAdapter(IFile.class);
	
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
			
			//XXX remove this line?!?! Test without it.
			interprocedural = javaProject.getResource().getPersistentProperty(SystemProperties.INTERPROCEDURAL_PROPKEY).toString().equals("true");
			
			options.put("rootpath", javaProject.getResource().getLocation().toFile().getAbsolutePath());
			
			List<File> classpath = generateClasspath(javaProject);
			
		} catch (Throwable e) {
			String message = e.getMessage() == null ? "No message specified" : e.getMessage();
			InternalErrorDialog internalErrorDialog = new InternalErrorDialog(shell, "An error has occurred", ResourceUtil.getEmergoIcon(), message, e, MessageDialog.ERROR, new String[] { "Ok", "Details" }, 0);
			internalErrorDialog.setDetailButton(1);
			internalErrorDialog.open();
			e.printStackTrace();
		}
	}
	
	private List<File> generateClasspath(IJavaProject javaProject) throws JavaModelException {

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

		return classpath;
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