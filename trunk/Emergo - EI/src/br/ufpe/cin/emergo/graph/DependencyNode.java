package br.ufpe.cin.emergo.graph;

import br.ufpe.cin.emergo.core.SelectionPosition;

public interface DependencyNode {
	public SelectionPosition getPosition();

	public boolean isInSelection();
}
