package br.ufpe.cin.emergo.core.dependencies;

import dk.au.cs.java.compiler.analysis.DepthFirstAdapter;
import dk.au.cs.java.compiler.node.AClassTypeDecl;
import dk.au.cs.java.compiler.node.ACompilationUnit;
import dk.au.cs.java.compiler.node.AConstructorDecl;
import dk.au.cs.java.compiler.node.AFieldAccessPrimary;
import dk.au.cs.java.compiler.node.AFieldDecl;
import dk.au.cs.java.compiler.node.ALocalDecl;
import dk.au.cs.java.compiler.node.AMethodDecl;
import dk.au.cs.java.compiler.node.AMethodInvocationPrimary;
import dk.au.cs.java.compiler.node.AProgram;
import dk.au.cs.java.compiler.node.Node;

public class ASTPrinterVisitor extends DepthFirstAdapter {
	
	int tabs;
	String toPrint; 
	
	@Override
	public void defaultNode(Node node) {
		//System.out.print("\r\n" + getTabs(tabs) + node.getClass().getSimpleName());
		//tabs++;
		
		super.defaultNode(node);
	}
	
	
	
	@Override
	public void defaultIn(Node node) {
		System.out.println();
		System.out.print(getTabs(tabs) + node.getClass().getSimpleName());
		if (toPrint != null) {
			System.out.print(" - " + toPrint);
			toPrint = null;
		}
		tabs++;
		super.defaultIn(node);
	}
	
	
	@Override
	public void defaultOut(Node node) {
		//System.out.println();
		tabs--;
		super.defaultOut(node);
	}
	
	public String getTabs(int num) {
		String tabs = "";
		for (int i = 0; i < num; i++) {
			tabs += "  ";
		}
		return tabs;
	}
	
	@Override
	public void outAProgram(AProgram node) {
		System.out.println();
		super.outAProgram(node);
	}
	
	@Override
	public void inACompilationUnit(ACompilationUnit node) {
		toPrint = node.getFileName();
		super.inACompilationUnit(node);
	}
	
	@Override
	public void inAClassTypeDecl(AClassTypeDecl node) {
		toPrint = node.getName().toString();
		super.inAClassTypeDecl(node);
	}
	
	@Override
	public void inAFieldDecl(AFieldDecl node) {
		toPrint = node.getName().toString();
		super.inAFieldDecl(node);
	}
	
	@Override
	public void inAMethodDecl(AMethodDecl node) {
		toPrint = node.getName().toString();
		super.inAMethodDecl(node);
	}
	
	@Override
	public void inAConstructorDecl(AConstructorDecl node) {
		toPrint = node.getName().toString();
		super.inAConstructorDecl(node);
	}
	
	@Override
	public void inALocalDecl(ALocalDecl node) {
		toPrint = node.getName().toString();
		super.inALocalDecl(node);
	}
	
	@Override
	public void inAFieldAccessPrimary(AFieldAccessPrimary node) {
		toPrint = node.getName().toString();
		super.inAFieldAccessPrimary(node);
	}
	
	@Override
	public void inAMethodInvocationPrimary(AMethodInvocationPrimary node) {
		toPrint = node.getName().toString();
		super.inAMethodInvocationPrimary(node);
	}

}
