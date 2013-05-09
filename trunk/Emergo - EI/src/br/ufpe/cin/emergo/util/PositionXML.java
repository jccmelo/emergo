package br.ufpe.cin.emergo.util;

import java.util.Random;

public class PositionXML {
	
	private int length;
	private int offSet;
	private int startLine;
	private int startColumn;
	private int endLine;
	private int endColumn;
	private String className;
	private String methodName;
	
	public PositionXML(int startLine, int endLine, int startColumn, 
			int endColumn, String className, String methodName) {
		super();
		this.startLine = startLine;
		this.startColumn = startColumn;
		this.endLine = endLine;
		this.endColumn = endColumn;
		this.className = className;
		this.methodName = methodName;
		this.length = startColumn+endColumn;
		this.offSet = this.length + 3 * new Random().nextInt(100);
				
	}

	public int getLength() {
		return length;
	}

	public int getOffSet() {
		return offSet;
	}

	public int getStartLine() {
		return startLine;
	}

	public int getStartColumn() {
		return startColumn;
	}

	public int getEndLine() {
		return endLine;
	}

	public int getEndColumn() {
		return endColumn;
	}

	public String getClassName() {
		return className;
	}

	public String getMethodName() {
		return methodName;
	}
	
	

}
