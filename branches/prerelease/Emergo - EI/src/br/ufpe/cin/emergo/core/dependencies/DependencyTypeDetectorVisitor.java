package br.ufpe.cin.emergo.core.dependencies;

import java.util.ArrayList;

import br.ufpe.cin.emergo.core.SelectionPosition;
import dk.au.cs.java.compiler.analysis.DepthFirstAdapter;
import dk.au.cs.java.compiler.ifdef.IfDefVarSet;
import dk.au.cs.java.compiler.node.ACompilationUnit;
import dk.au.cs.java.compiler.node.AFieldAccessPrimary;
import dk.au.cs.java.compiler.node.AFieldDecl;
import dk.au.cs.java.compiler.node.AIfdefDecl;
import dk.au.cs.java.compiler.node.AMethodDecl;
import dk.au.cs.java.compiler.node.AMethodInvocationPrimary;
import dk.au.cs.java.compiler.node.AProgram;
import dk.au.cs.java.compiler.node.Token;
import dk.au.cs.java.compiler.type.members.Field;
import dk.au.cs.java.compiler.type.members.Method;

public class DependencyTypeDetectorVisitor extends DepthFirstAdapter {

	private String filePath;
	private SelectionPosition position;

	private ArrayList<DepthFirstAdapter> visitors = new ArrayList<DepthFirstAdapter>();

	public DependencyTypeDetectorVisitor(String filePath,
			SelectionPosition position) {
		this.filePath = filePath;
		this.position = position;
	}
	
	public void runVisitors(AProgram rootNode) {
		for (DepthFirstAdapter visitor : visitors) {
			rootNode.apply(visitor);
		}
	}

	public ArrayList<DepthFirstAdapter> getVisitors() {
		return visitors;
	}

	@Override
	public void caseAIfdefDecl(AIfdefDecl node) {
		System.out.println("caseAIfdefDecl " + node.toString());

		super.caseAIfdefDecl(node);
	}

	@Override
	public void caseACompilationUnit(ACompilationUnit cUnit) {
		String file = cUnit.getFile().getPath();
		if (file.equals(filePath)) {
			cUnit.apply(new DepthFirstAdapter() {

				@Override
				public void caseAFieldDecl(AFieldDecl node) {
					if (selected(node.getToken())) {
						// System.out.println("SELECTED FIELD DECL " +
						// node.getName());
						Field field = node.getField();
						IfDefVarSet varSet = IfDefGetVarSet.getVarSet(node);
						addField(field, varSet);
					}
					super.caseAFieldDecl(node);
				}

				@Override
				public void caseAFieldAccessPrimary(AFieldAccessPrimary node) {
					if (selected(node.getToken())) {
						// System.out.println("SELECTED FIELD ACCESS " +
						// node.getName());
						Field field = node.getField();
						IfDefVarSet varSet = IfDefGetVarSet.getVarSet(node);
						addField(field, varSet);
					}
					super.caseAFieldAccessPrimary(node);
				}

				@Override
				public void caseAMethodDecl(AMethodDecl node) {
					if (selected(node.getToken())) {
						Method method = node.getMethod();
						IfDefVarSet varSet = IfDefGetVarSet.getVarSet(node);
						addMethod(method, varSet);
					}
					super.caseAMethodDecl(node);
				}

				@Override
				public void caseAMethodInvocationPrimary(
						AMethodInvocationPrimary node) {
					if (selected(node.getToken())) {
						Method method = node.getMethod();
						IfDefVarSet varSet = IfDefGetVarSet.getVarSet(node);
						addMethod(method, varSet);
					}
					super.caseAMethodInvocationPrimary(node);
				}

			});

		}
	}

	private boolean selected(Token token) {
		int line = token.getLine();
		int startLine = position.getStartLine() + 1;
		int endLine = position.getEndLine() + 1;

		if (line >= startLine && line <= endLine) {
			int pos = token.getPos();
			int startColumn = position.getStartColumn();
			int endColumn = position.getEndColumn();

			// System.out.println(String.format("SELECTED %d, %d, %d, %d, %d, %d",
			// line, pos, startLine, endLine, startColumn, endColumn));
			if (line == startLine && pos < startColumn) {
				return false;
			}

			if (line == endLine && pos > endColumn) {
				return false;
			}
			return true;
		}
		return false;
	}

	private void addField(Field field, IfDefVarSet varSet) {
		if (field == null) {
			System.err.println("Field == null");
			return;
		}

		if (varSet == null) {
			System.err.println("VarSet == null");
			return;
		}

		visitors.add(new FieldDependencyVisitor(field, varSet));
	}

	private void addMethod(Method method, IfDefVarSet varSet) {
		if (method == null) {
			System.err.println("Method == null");
			return;
		}

		if (varSet == null) {
			System.err.println("VarSet == null");
			return;
		}

		visitors.add(new MethodDependencyVisitor(method, varSet));
	}

}
