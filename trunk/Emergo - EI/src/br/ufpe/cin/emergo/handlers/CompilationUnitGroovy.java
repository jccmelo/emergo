package br.ufpe.cin.emergo.handlers;

import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.MethodNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.ITextSelection;

import br.ufpe.cin.emergo.graph.transform.GraphTransformer;
import br.ufpe.cin.emergo.util.SelectionNodesGroovyVisitor;

public class CompilationUnitGroovy extends CommandCompilationUnit {

	@Override
	public void markNodesFromSelection(IFile textSelectionFile,
			ITextSelection textSelection, Map<Object, Object> options) {
		
		SelectionNodesGroovyVisitor selectionNodesVisitor = new SelectionNodesGroovyVisitor(textSelection);
		
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
