package br.ufpe.cin.emergo.views;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

public class MarkedLinesAction extends Action {

	private static final String ID = "br.ufpe.cin.emergo.MarkedLinesAction";
	private String text;
	private List<LineOfCode> linesOfCode;

	public void setLinesOfCode(List<LineOfCode> linesOfCode) {
		this.linesOfCode = linesOfCode;
	}

	@Override
	public void run() {
		for (LineOfCode lines : this.linesOfCode) {
			System.out.println(lines);
		}
		System.err.println("action was Done");

	}

}