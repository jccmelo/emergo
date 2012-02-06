package br.ufpe.cin.emergo.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;

public class MarkerGrouping {

	private String name;
	private List<IMarker> children;

	public MarkerGrouping() {
		this.children = new ArrayList<IMarker>();
	}

	public MarkerGrouping(List<IMarker> children) {
		this.children = children;
	}

	public MarkerGrouping(String name) {
		this.name = name;
		this.children = new ArrayList<IMarker>();
	}

	public void addChildren(IMarker marker) {
		this.children.add(marker);
	}

	public void removeChildren(IMarker marker) {
		this.children.remove(marker);
	}

	public void removeChildren(int index) {
		this.children.remove(index);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setChildren(List<IMarker> children) {
		this.children = children;
	}

	public List<IMarker> getChildren() {
		return children;
	}

	public String toString() {
		return this.name;
	}

	@Override
	public boolean equals(Object other) {
		// All markergroupings have only one name
		if (other instanceof MarkerGrouping) {
			return this.getName().equals(((MarkerGrouping) other).getName());
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 41;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

}