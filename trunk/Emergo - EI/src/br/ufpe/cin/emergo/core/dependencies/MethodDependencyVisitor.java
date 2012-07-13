package br.ufpe.cin.emergo.core.dependencies;

import dk.au.cs.java.compiler.ifdef.IfDefVarSet;
import dk.au.cs.java.compiler.node.ACompilationUnit;
import dk.au.cs.java.compiler.node.AMethodDecl;
import dk.au.cs.java.compiler.node.AMethodInvocationPrimary;
import dk.au.cs.java.compiler.node.AProgram;
import dk.au.cs.java.compiler.type.members.Method;

public class MethodDependencyVisitor extends DependencyVisitor {
	
	private Method method;

	protected MethodDependencyVisitor(Method method, IfDefVarSet varSet) {
		super(varSet);
		this.method = method;
	}
	
	
	@Override
	public void caseAProgram(AProgram node) {
		System.out.println("SELECTED METHOD: " + method.getFullNameDescriptor() + " | VARSET: " + varSet);
		super.caseAProgram(node);
	}
	
	@Override
	public void caseAMethodDecl(AMethodDecl node) {
		if (dependency(node.getMethod()) && checkVarSet(node)) {
			Dependency dependency = new Dependency();
			dependency.description = "DEPENDENCY DECL METHOD";
			dependency.file = node.getAncestor(ACompilationUnit.class).getFile().getAbsolutePath();
			dependency.line = node.getToken().getLine();
			dependency.varSet = IfDefGetVarSet.getVarSet(node);
			dependencies.add(dependency);
			
			System.out.println("\t" + dependency.description + 
					"| FILE: " + dependency.file +
					" | LINE: " + dependency.line + 
					" | VARSET: " + dependency.varSet);
		}
		super.caseAMethodDecl(node);
	}
	
	@Override
	public void caseAMethodInvocationPrimary(AMethodInvocationPrimary node) {
		if (dependency(node.getMethod()) && checkVarSet(node)) {
			Dependency dependency = new Dependency();
			dependency.description = "DEPENDENCY ACCESS METHOD";
			dependency.file = node.getAncestor(ACompilationUnit.class).getFile().getAbsolutePath();
			dependency.line = node.getToken().getLine();
			dependency.varSet = IfDefGetVarSet.getVarSet(node);
			dependencies.add(dependency);
			
			System.out.println("\t" + dependency.description + 
					"| FILE: " + dependency.file +
					" | LINE: " + dependency.line + 
					" | VARSET: " + dependency.varSet);
		}
		super.caseAMethodInvocationPrimary(node);
	}
	
	private boolean dependency(Method m) {
		if (method != null && m != null) {
			String methodDescriptor = method.getFullNameDescriptor();
			String acessDescriptor = m.getFullNameDescriptor();
			
			if (methodDescriptor.equals(acessDescriptor)) {
				return true;
			}
		}
		
		return false;
	}

}
