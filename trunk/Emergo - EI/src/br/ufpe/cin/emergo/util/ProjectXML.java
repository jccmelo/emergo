package br.ufpe.cin.emergo.util;

import java.util.List;

public class ProjectXML {
	
	private String name;
	private String classpath;
	private String fileExtension;
	private List<PositionXML> pos;
	
	public ProjectXML(String name, String classpath, String fileExtension,
			List<PositionXML> pos) {
		super();
		this.name = name;
		this.classpath = classpath;
		this.fileExtension = fileExtension;
		this.pos = pos;
	}

	public String getName() {
		return name;
	}

	public String getClasspath() {
		return classpath;
	}

	public String getFileExtension() {
		return fileExtension;
	}

	public List<PositionXML> getPos() {
		return pos;
	}

}
