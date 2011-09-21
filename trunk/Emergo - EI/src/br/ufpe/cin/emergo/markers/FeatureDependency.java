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

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((configuration == null) ? 0 : configuration.hashCode());
		result = prime * result + ((feature == null) ? 0 : feature.hashCode());
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		result = prime * result + lineNumber;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FeatureDependency other = (FeatureDependency) obj;
		if (configuration == null) {
			if (other.configuration != null)
				return false;
		} else if (!configuration.equals(other.configuration))
			return false;
		if (feature == null) {
			if (other.feature != null)
				return false;
		} else if (!feature.equals(other.feature))
			return false;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		if (lineNumber != other.lineNumber)
			return false;
		return true;
	}
	
	
	

}