package br.ufpe.cin.emergo.core.dependencies;

import dk.au.cs.java.compiler.ifdef.IfDefVarSet;
import dk.au.cs.java.compiler.node.ACompilationUnit;
import dk.au.cs.java.compiler.node.AFieldAccessPrimary;
import dk.au.cs.java.compiler.node.AFieldDecl;
import dk.au.cs.java.compiler.node.AProgram;
import dk.au.cs.java.compiler.type.members.Field;

public class FieldDependencyVisitor extends DependencyVisitor {
	
	private Field field;
	
	public FieldDependencyVisitor(Field field, IfDefVarSet varSet) {
		super(varSet);
		this.field = field;
	}
	
	@Override
	public void caseAProgram(AProgram node) {
		System.out.println("SELECTED FIELD: " + field.getFullNameDescriptor() + " | VARSET: " + varSet);
		super.caseAProgram(node);
	}
	
		
	@Override
	public void caseAFieldDecl(AFieldDecl node) {
		//System.out.println("caseAFieldDecl " + node.getName());
		if (dependency(node.getField()) && checkVarSet(node)) {
			System.out.println("\tDEPENDENCY DECL FIELD | FILE: " + node.getAncestor(ACompilationUnit.class).getFileName() +
					" | LINE: " + node.getToken().getLine() + 
					" | VARSET: " + IfDefGetVarSet.getVarSet(node));
		}
		
		super.caseAFieldDecl(node);
	}
	
	@Override
	public void caseAFieldAccessPrimary(AFieldAccessPrimary node) {
		//System.out.println("caseAFieldAccessPrimary " + node.toString());
		
		if (dependency(node.getField()) && checkVarSet(node)) {
			System.out.println("\tDEPENDENCY ACCESS FIELD | FILE: " + node.getAncestor(ACompilationUnit.class).getFileName() +
					" | LINE: " + node.getToken().getLine() + 
					" | VARSET: " + IfDefGetVarSet.getVarSet(node));
		}
		
		super.caseAFieldAccessPrimary(node);
	}
	
	
	private boolean dependency(Field f) {
		if (field != null && f != null) {
			String fieldDescriptor = field.getFullNameDescriptor();
			String acessDescriptor = f.getFullNameDescriptor();
			
			if (fieldDescriptor.equals(acessDescriptor)) {
				return true;
			}
		}
		
		return false;
	}
	
	
	
}


