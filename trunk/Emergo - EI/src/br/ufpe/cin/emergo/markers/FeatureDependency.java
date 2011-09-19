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

	public FeatureDependency setFile(IFile file) {
		this.file = file;
		return this;
	}

	public FeatureDependency setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
		return this;
	}

	public FeatureDependency setConfiguration(String configuration) {
		this.configuration = configuration;
		return this;
	}

	public FeatureDependency setFeature(String feature) {
		this.feature = feature;
		return this;
	}

}