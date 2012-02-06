package br.ufpe.cin.emergo.core.dependencies;

import dk.au.cs.java.compiler.analysis.DepthFirstAdapter;
import dk.au.cs.java.compiler.ifdef.IfDefVarSet;
import dk.au.cs.java.compiler.node.AIfdefDecl;
import dk.au.cs.java.compiler.node.AIfdefStm;
import dk.au.cs.java.compiler.node.AProgram;
import dk.au.cs.java.compiler.node.IIfdefConfigurable;
import dk.au.cs.java.compiler.node.Node;

public class IfDefGetVarSet extends DepthFirstAdapter {
	
	private IfDefVarSet varSet;
	
	public static IfDefVarSet getVarSet(Node node) {
		AIfdefDecl ifDefDecl = node.getAncestor(AIfdefDecl.class);
		AIfdefStm ifDefStm = node.getAncestor(AIfdefStm.class);
		if (ifDefDecl != null || ifDefStm != null) {
			//System.out.println("\tIFDEF " + ifDefDecl + " | " + ifDefStm);
			
			IfDefGetVarSet getVarSet = new IfDefGetVarSet();
			
			if (ifDefDecl != null) {
				ifDefDecl.apply(getVarSet);
			}
			if (ifDefStm != null) {
				ifDefStm.apply(getVarSet);
			}
			
			//System.out.println("\tVARSET " + getVarSet.varSet);
			
			return getVarSet.varSet;
		} else {
			AProgram program = node.getAncestor(AProgram.class);
			IfDefGetVarSet getVarSet = new IfDefGetVarSet();
			program.apply(getVarSet);
			return getVarSet.varSet;
			
		}
	}
	
	@Override
	public void defaultIn(Node node) {
		if (varSet == null) {
			if (!(node instanceof AIfdefDecl) && !(node instanceof AIfdefStm)) {
				if (node instanceof IIfdefConfigurable) {
					IIfdefConfigurable ifdefConfigurable = (IIfdefConfigurable) node;
					varSet = ifdefConfigurable.getVarSet();
				}
			}
			
			super.defaultIn(node);
		}
	}

}
