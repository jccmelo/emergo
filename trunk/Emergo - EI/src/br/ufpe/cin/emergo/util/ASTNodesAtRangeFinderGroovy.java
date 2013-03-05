package br.ufpe.cin.emergo.util;

import java.util.Collection;
import java.util.HashSet;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.stmt.Statement;

import soot.Unit;
import soot.tagkit.LineNumberTag;

/**
 * The Class ASTNodesAtRangeFinderGroovy maps ASTNodes to Units using their positions
 * (line and column) in the source code as a parameter.
 */
public class ASTNodesAtRangeFinderGroovy extends CodeVisitorSupport {

	/** The compilation unit. */
	private Statement statement;

	/** The found nodes. */
	private Collection<ASTNode> foundNodes = new HashSet<ASTNode>();

	/** The starting line. */
	private int startLine;

	/** The starting position. */
	private int startPos;

	/** The ending line. */
	private int endLine;

	/** The ending position. */
	private int endPos;

	/**
	 * Instantiates a new ASTNodes at range finder.
	 * 
	 * @param startLine
	 *            the starting line
	 * @param startPos
	 *            the starting posision
	 * @param endLine
	 *            the ending line
	 * @param endPos
	 *            the ending position
	 * @param compilationUnit
	 *            the compilation unit in which the nodes will be visited
	 */
	public ASTNodesAtRangeFinderGroovy(int startLine, int startPos, int endLine, int endPos, Statement stmt) {
		this.statement = stmt;
		this.startLine = startLine;
		this.startPos = startPos;
		this.endLine = endLine;
		this.endPos = endPos;
	}

	/**
	 * Instantiates a new ASTNodes at range finder. The Unit MUST have the
	 * SourceLnPosTag tag attached to it, or an {@link IllegalArgumentException}
	 * will be thrown.
	 * 
	 * @param unit
	 *            the unit
	 * @param compilationUnit
	 *            the compilation unit
	 */
	public ASTNodesAtRangeFinderGroovy(Unit unit, Statement stmt) {
		if (unit.hasTag("LineNumberTag")) {
			LineNumberTag lineTag = (LineNumberTag) unit.getTag("LineNumberTag");
			this.startLine = lineTag.getLineNumber();
			this.startPos = lineTag.getLineNumber();
			this.statement = stmt;
		} else {
			throw new IllegalArgumentException("No LineNumberTag found in this unit.");
		}
	}

	public void visit(ASTNode node) {
		if (statement.getLineNumber() == startLine && statement.getColumnNumber() + 1 >= startPos && 
				statement.getLastLineNumber() == endLine && statement.getLastColumnNumber()-1 <= endPos) {
			this.foundNodes.add(node);
		}
	}

	/**
	 * Gets the nodes found. It will be empty before visiting.
	 * 
	 * @return the nodes
	 */
	public Collection<ASTNode> getNodes() {
		return this.foundNodes;
	}
}
