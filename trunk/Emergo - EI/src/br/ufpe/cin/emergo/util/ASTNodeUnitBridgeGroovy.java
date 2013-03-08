package br.ufpe.cin.emergo.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.stmt.Statement;

import br.ufpe.cin.emergo.handlers.MultipleLineNumbersVisitorGroovy;

import soot.Body;
import soot.Unit;
import soot.tagkit.LineNumberTag;
import soot.util.Chain;

/**
 * The Class StmtUnitsBridgeGroovy is a utility class used to map between Statements
 * and Units.
 */
public class ASTNodeUnitBridgeGroovy {

	/**
	 * This is a utility class with only static methods. There's no need for a
	 * constructor.
	 */
	private ASTNodeUnitBridgeGroovy() {
	}

	/**
	 * Gets the ASTNodes from a unit. This transition takes into consideration
	 * the position of unit in the source code. The SourceLnPosTag must be
	 * present in this unit or an {@link IllegalArgumentException} will be
	 * thrown.
	 * 
	 * @param unit
	 *            the unit
	 * @param compilationUnit
	 *            the compilation unit
	 * @return the aST nodes from unit
	 */
	public static Collection<ASTNode> getASTNodesFromUnit(Unit unit, Statement stmt) {
		ASTNodesAtRangeFinderGroovy ASTNodeVisitor;
		try {
			ASTNodeVisitor = new ASTNodesAtRangeFinderGroovy(unit, stmt);
		} catch (IllegalArgumentException ex) {
			// TODO: silently ignoring an error is a bad idea.
			return Collections.emptyList();
		}
		stmt.visit(ASTNodeVisitor);
		return ASTNodeVisitor.getNodes();
	}

	/**
	 * Gets the ASTNodes from units. This transition ONLY takes into
	 * consideration the starting line of the Units. If more precision is
	 * necessary, see
	 * {@link ASTNodeUnitBridge#getASTNodesFromUnit(Unit, CompilationUnit)}.
	 * 
	 * @param units
	 *            the units
	 * @param compilationUnit
	 *            the compilation unit
	 * @return the aST nodes from units
	 * 
	 * @see
	 */
	public static Collection<ASTNode> getASTNodesFromUnits(Collection<Unit> units, Statement stmt) {
		return ASTNodeUnitBridgeGroovy.getASTNodesFromLines(ASTNodeUnitBridgeGroovy.getLinesFromUnits(units), stmt);
	}

	/**
	 * Gets the ASTNodes from lines.
	 * 
	 * @param lines
	 *            the lines
	 * @param compilationUnit
	 *            the compilation unit
	 * @return the aST nodes from lines
	 */
	public static Collection<ASTNode> getASTNodesFromLines(Collection<Integer> lines, Statement stmt) {
		MultipleLineNumbersVisitorGroovy linesVisitor = new MultipleLineNumbersVisitorGroovy(lines, stmt);
		stmt.visit(linesVisitor);
		return linesVisitor.getNodes();
	}

	/**
	 * Gets the lines from the AST nodes.
	 * 
	 * @param nodes
	 *            the nodes
	 * @param compilationUnit
	 *            the compilation unit
	 * @return the lines from ast nodes
	 */
	public static Collection<Integer> getLinesFromASTNodes(Collection<ASTNode> nodes, Statement stmt) {
		Set<Integer> lineSet = new HashSet<Integer>();
		for (ASTNode node : nodes) {
			lineSet.add(node.getLineNumber());
		}
		return lineSet;
	}

	/**
	 * Gets the line from unit.
	 * 
	 * @param unit
	 *            the unit
	 * @return the line from unit
	 */
	public static Integer getLineFromUnit(Unit unit) {
		if (unit.hasTag("LineNumberTag")) {
			LineNumberTag lineTag = (LineNumberTag) unit.getTag("LineNumberTag");
			return lineTag.getLineNumber();
		}
		return -1;
	}

	/**
	 * Gets the lines from units.
	 * 
	 * @param units
	 *            the units
	 * @return the lines from units
	 */
	public static Collection<Integer> getLinesFromUnits(Collection<Unit> units) {
		Set<Integer> lines = new HashSet<Integer>(units.size());
		Iterator<Unit> iterator = units.iterator();
		while (iterator.hasNext()) {
			Unit unit = iterator.next();
			if (unit.hasTag("LineNumberTag")) {
				LineNumberTag lineTag = (LineNumberTag) unit.getTag("LineNumberTag");
				lines.add(lineTag.getLineNumber());
			}
		}
		return lines;
	}

	/**
	 * Gets the units from lines.
	 * 
	 * @param lines
	 *            the lines
	 * @param body
	 *            the body
	 * @return the units from lines
	 */
	public static Collection<Unit> getUnitsFromLines(Collection<Integer> lines, Body body) {
		Set<Unit> unitSet = new HashSet<Unit>();
		for (Integer line : lines) {
			Chain<Unit> units = body.getUnits();
			for (Unit unit : units) {
				if (unit.hasTag("LineNumberTag")) {
					LineNumberTag lineTag = (LineNumberTag) unit.getTag("LineNumberTag");
					if (lineTag != null) {
						if (lineTag.getLineNumber() == line.intValue()) {
							unitSet.add(unit);
						}
					}
				}
			}
		}
		return unitSet;
	}
}
