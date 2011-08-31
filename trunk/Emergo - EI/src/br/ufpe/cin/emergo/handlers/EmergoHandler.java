package br.ufpe.cin.emergo.handlers;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

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
		
		
		return null;
	}


}
