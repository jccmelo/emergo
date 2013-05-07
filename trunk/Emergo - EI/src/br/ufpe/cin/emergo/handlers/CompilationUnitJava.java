package br.ufpe.cin.emergo.handlers;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jface.text.ITextSelection;

import br.ufpe.cin.emergo.graph.transform.GraphTransformer;
import br.ufpe.cin.emergo.util.SelectionNodesVisitor;

public class CompilationUnitJava extends CommandCompilationUnit {

	@Override
	public void markNodesFromSelection(IFile textSelectionFile,
			ITextSelection textSelection, Map<Object, Object> options) {
		
		SelectionNodesVisitor selectionNodesVisitor = new SelectionNodesVisitor(textSelection);
		jdtCompilationUnit = GraphTransformer.getCompilationUnit(textSelectionFile);

        jdtCompilationUnit.accept(selectionNodesVisitor);
        selectionNodes = selectionNodesVisitor.getNodes();

        for (ASTNode astNode : selectionNodes) {
        	GraphTransformer.lineNumbers.add(jdtCompilationUnit.getLineNumber(astNode.getStartPosition()));
        }
        
        options.put("selectionNodes", selectionNodes);
	}

}
