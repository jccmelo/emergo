package br.ufpe.cin.emergo.handlers;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit.PrimaryClassNodeOperation;
import org.codehaus.groovy.control.SourceUnit;
import org.eclipse.jface.text.ITextSelection;

import br.ufpe.cin.emergo.util.SelectionNodesGroovyVisitor;

public class ClassNodeOperationGroovy extends PrimaryClassNodeOperation {

	/** The text selection. */
	private ITextSelection textSelection;
	
	public ClassNodeOperationGroovy(ITextSelection textSelection) {
		this.textSelection = textSelection;
	}

	@Override
	public void call(SourceUnit source, GeneratorContext context, ClassNode node)
			throws CompilationFailedException {
		
		ModuleNode ast = source.getAST();
		ast.getStatementBlock().visit(new SelectionNodesGroovyVisitor(textSelection));
	}

}
