package br.ufpe.cin.emergo.views;

import org.apache.commons.collections.comparators.ComparableComparator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.ITextSelection;

public class LineOfCode extends ComparableComparator{
	private String file;
	private String selection;
	private int line;
	
	private ITextSelection textSelection;
	private IFile fileSelection;
	
	public LineOfCode(ITextSelection textSelection,IFile fileSelection){
		this.textSelection = textSelection;
		this.fileSelection = fileSelection;
		this.file = fileSelection.getName();
		this.selection = textSelection.getText();
		this.line = textSelection.getStartLine();
	}
	public void setFile(String file) {
		this.file = file;
	}
	public String getFile() {
		return file;
	}
	public void setSelection(String selection) {
		this.selection = selection;
	}
	public String getSelection() {
		return selection;
	}
	public void setLine(int line) {
		this.line = line;
	}
	public int getLine() {
		return line;
	}
	public String toString(){
		return "Line: "+line+" ("+selection+")";
	}
	public boolean equals(LineOfCode other){
		return (this.getLine()==other.getLine() && this.getFile().equals(other.getFile()) && this.getSelection().equals(other.getSelection()));
	}
	public IFile getTextSelectionFile() {
		return this.fileSelection;
	}
	public ITextSelection getTextSelection() {
		return this.textSelection;
	}
}
