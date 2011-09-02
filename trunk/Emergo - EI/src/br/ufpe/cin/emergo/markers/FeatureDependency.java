package br.ufpe.cin.emergo.markers;

import org.eclipse.core.resources.IFile;

public class FeatureDependency {

	private IFile file;
	private int lineNumber;
	private String configuration;
	private String feature;

	public IFile getFile() {
		return file;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public String getConfiguration() {
		return configuration;
	}

	public String getFeature() {
		return feature;
	}

	public void setFile(IFile file) {
		this.file = file;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}

	public void setFeature(String feature) {
		this.feature = feature;
	}

}