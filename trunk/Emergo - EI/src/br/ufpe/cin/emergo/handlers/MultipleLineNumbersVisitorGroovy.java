package br.ufpe.cin.emergo.handlers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.stmt.Statement;

public class MultipleLineNumbersVisitorGroovy extends CodeVisitorSupport {
	private Statement stmt;
	private Collection<Integer> lines;
	private Set<ASTNode> nodes = new HashSet<ASTNode>();
	
	public Set<ASTNode> getNodes(){
		return nodes;
	}

	public MultipleLineNumbersVisitorGroovy(Collection<Integer> lines, Statement compilationUnit) {
		super();
		this.lines = lines;
		this.stmt = compilationUnit;
	}

	public boolean visit(ASTNode node) {
		if (lines.contains(stmt.getLineNumber())) {
			nodes.add(node);
		}
		return true;
	}
}

