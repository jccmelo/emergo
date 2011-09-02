package br.ufpe.cin.emergo.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import br.ufpe.cin.emergo.markers.EmergoMarker;
import br.ufpe.cin.emergo.markers.FeatureDependency;

/**
 * Handler for the br.ufal.cideei.commands.DoCompute extension command.
 * 
 * @author Társis
 * 
 */
public class EmergoHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		Map<Object, Object> options = new HashMap<Object, Object>();

		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
		Shell shell = HandlerUtil.getActiveShellChecked(event);

		if (!(selection instanceof ITextSelection))
			throw new ExecutionException("Not a text selection");

		/*
		 * used to find out the project name and later to create a compilation unit from it
		 */
		IFile textSelectionFile = (IFile) HandlerUtil.getActiveEditorChecked(event).getEditorInput().getAdapter(IFile.class);

		/*
		 * this visitor will compute the ASTNodes that were selected by the user
		 */
		ITextSelection textSelection = (ITextSelection) selection;
		
		textSelection.getStartLine();
		textSelection.getOffset();

		//TODO: pass the file, start line and offset position to the underlying dependency finder.
		Logger.getAnonymousLogger().log(Level.INFO, "Generating emergent interface...");
		testingEmergoResultsView(textSelectionFile);

		return null;
	}

	//TODO: temporary method. Just to test the populate the view to test Emergo GUI functionalities. However, we can reuse part of this method!!!
	private void testingEmergoResultsView(IFile fileSelected) {
		
		try {
			fileSelected.deleteMarkers(EmergoMarker.EMERGO_MARKER_ID, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		FeatureDependency fd = new FeatureDependency();
		fd.setLineNumber(32);
		fd.setFile(fileSelected);
		fd.setConfiguration("A");
		fd.setFeature("A");

		EmergoMarker.createMarker("Provides x to " + "metodo(napo)", fd);
	}

}