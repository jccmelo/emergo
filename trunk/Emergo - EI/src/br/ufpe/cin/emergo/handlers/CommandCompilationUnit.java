package br.ufpe.cin.emergo.handlers;

import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.stmt.Statement;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.ITextSelection;

public abstract class CommandCompilationUnit {

	public static Set<ASTNode> selectionNodes;
	public static CompilationUnit jdtCompilationUnit;
	public static Statement stmt;

	public abstract void markNodesFromSelection(IFile textSelectionFile,
			ITextSelection textSelection, Map<Object, Object> options);
}
