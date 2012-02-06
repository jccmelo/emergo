package br.ufpe.cin.emergo.core.dependencies;

import dk.au.cs.java.compiler.analysis.DepthFirstAdapter;
import dk.au.cs.java.compiler.ifdef.IfDefVarSet;
import dk.au.cs.java.compiler.node.Node;

public class DependencyVisitor extends DepthFirstAdapter {
	
	protected IfDefVarSet varSet;
	
	protected DependencyVisitor(IfDefVarSet varSet) {
		this.varSet = varSet;
	}
	
	protected boolean checkVarSet(Node node) {
		return !sameVarSet(varSet, IfDefGetVarSet.getVarSet(node));
	}
	
	protected boolean sameVarSet(IfDefVarSet varSet1, IfDefVarSet varSet2) {
		
		IfDefVarSet andVarSet = varSet1.and(varSet2);
		boolean result = varSet1.equals(andVarSet);
		//System.out.println("sameVarSet " + varSet1 + " AND " + varSet2 + " = " + result);
		return result;
	}

}
