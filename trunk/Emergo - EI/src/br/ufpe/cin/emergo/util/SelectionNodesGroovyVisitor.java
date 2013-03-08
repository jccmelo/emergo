package br.ufpe.cin.emergo.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.builder.AstBuilderTransformation;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.SourceUnit;
import org.eclipse.jface.text.ITextSelection;

public class SelectionNodesGroovyVisitor extends CodeVisitorSupport {

	/** The text selection. */
	private ITextSelection textSelection;

	/** The nodes will be added to this Set as the visitor visits the nodes */
	private Set<ASTNode> nodes = new HashSet<ASTNode>();

	public SelectionNodesGroovyVisitor() {
	}

	/**
	 * Instantiates a new selection nodes visitor.
	 * 
	 * @param textSelection
	 *            the text selection
	 */
	public SelectionNodesGroovyVisitor(ITextSelection textSelection) {
		this.textSelection = textSelection;
	}

	/**
	 * Gets the nodes.
	 * 
	 * @return the nodes
	 */
	public Set<ASTNode> getNodes() {
		return nodes;
	}

	public void visitStatement(Statement stmt) {
		// super.visitBlockStatement(stmt);
		if (stmt instanceof BlockStatement) {
			super.visitBlockStatement((BlockStatement) stmt);
			List<Statement> listStmt = ((BlockStatement) stmt).getStatements();

			for (Iterator it = listStmt.iterator(); it.hasNext();) {
				Statement currentStmt = (Statement) it.next();
				
				if (currentStmt.getLineNumber() == textSelection.getStartLine() + 1) {
					System.out.println("Node added => "+currentStmt.getText());
					nodes.add(currentStmt);
				}

			}
		}
	}

	public Statement getStatement(Statement stmt, int i) {
		if (stmt == null)
			return null;
		Statement first = stmt;
		while (first instanceof BlockStatement) {
			List<Statement> list = ((BlockStatement) first).getStatements();
			if (list.isEmpty()) {
				first = null;
			} else {
				if (list.size() > i)
					first = list.get(i);
				else
					first = null;
			}
		}
		return first;
	}

	public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {

		AstBuilderTransformation t = new AstBuilderTransformation();
		t.visit(nodes, sourceUnit);
	}

	/**
	 * Populates the {@link #nodes} Set with the ASTNodes. Use
	 * {@link #getNodes()} to retrieve the nodes after accepting this visitor to
	 * an ASTNode
	 * 
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#preVisit(org.eclipse.jdt.core.dom.ASTNode)
	 */
	// public void preVisit(ASTNode node) {
	// super.preVisit(node);
	// if (node.getStartPosition() >= textSelection.getOffset() &&
	// (node.getStartPosition() + node.getLength()) <= textSelection.getOffset()
	// + textSelection.getLength()) {
	// nodes.add(node);
	// }
	// }
}
