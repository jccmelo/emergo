package br.ufpe.cin.emergo.core;

/**
 * Immutable class used to represent an user selection on a given file. Note that no verification is made, like if the
 * {@code startLine} or {@code offSet} is within the actual bounds of the file, or even if the file exists. Only if the
 * {@code filePath} is null is check when building;
 * 
 * Use SelectionPosition.builder()... to construct an instance.
 * 
 * TODO: describe if the numbers are 0-based and things like that.
 * 
 * @author Társis Tolêdo
 * 
 */
public final class SelectionPosition {
	private final int length;
	private final int offSet;
	private final int startLine;
	private final int startColumn;
	private final int endLine;
	private final int endColumn;
	private final String filePath;
	private final String stringRepresentation;

	/**
	 * Clients are encouraged to use the SelectionPosition.Builder class instead of this constructor.
	 * 
	 * @param l
	 * @param o
	 * @param sl
	 * @param sc
	 * @param el
	 * @param ec
	 * @param f
	 */
	public SelectionPosition(int l, int o, int sl, int sc, int el, int ec, String f) {
		this.length = l;
		this.offSet = o;
		this.startLine = sl;
		this.startColumn = sc;
		this.endLine = el;
		this.endColumn = ec;
		this.filePath = f;
		this.stringRepresentation = "[(" + sl + "," + sc + "),(" + el + "," + ec + ")," + l + "]";
	}

	/**
	 * Gets the starting line of the selection.
	 * 
	 * @return the starting line.
	 */
	public int getStartLine() {
		return startLine;
	}

	/**
	 * Gets the offset of the selection.
	 * 
	 * @return the off set.
	 */
	public int getOffSet() {
		return offSet;
	}

	/**
	 * Gets the filepath of the file where the selection occured.
	 * 
	 * @return the file path.
	 */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * Gets the length of the selection.
	 * 
	 * @return
	 */
	public int getLength() {
		return length;
	}

	/**
	 * Gets the end column of the selecion.
	 * 
	 * @return
	 */
	public int getEndColumn() {
		return endColumn;
	}

	/**
	 * Gets the end line of the selection.
	 * 
	 * @return
	 */
	public int getEndLine() {
		return endLine;
	}

	/**
	 * Gets the start column of the selection.
	 * 
	 * @return
	 */
	public int getStartColumn() {
		return startColumn;
	}

	/**
	 * Returns a String representation for this object.
	 */
	public String toString() {
		return stringRepresentation;
	}

	/**
	 * Creates and instance of a builder. The client may chain set the properties on the Builder and then call
	 * {@link Builder#build()}.
	 * 
	 * @return
	 */
	public static Builder builder() {
		return new SelectionPosition.Builder();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + endColumn;
		result = prime * result + endLine;
		result = prime * result + ((filePath == null) ? 0 : filePath.hashCode());
		result = prime * result + length;
		result = prime * result + offSet;
		result = prime * result + startColumn;
		result = prime * result + startLine;
		result = prime * result + ((stringRepresentation == null) ? 0 : stringRepresentation.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SelectionPosition))
			return false;
		SelectionPosition other = (SelectionPosition) obj;
		if (endColumn != other.endColumn)
			return false;
		if (endLine != other.endLine)
			return false;
		if (filePath == null) {
			if (other.filePath != null)
				return false;
		} else if (!filePath.equals(other.filePath))
			return false;
		if (length != other.length)
			return false;
		if (offSet != other.offSet)
			return false;
		if (startColumn != other.startColumn)
			return false;
		if (startLine != other.startLine)
			return false;
		if (stringRepresentation == null) {
			if (other.stringRepresentation != null)
				return false;
		} else if (!stringRepresentation.equals(other.stringRepresentation))
			return false;
		return true;
	}

	/**
	 * A simple builder for the enclosing class. Forces the client to explicitly and verbosely sets the arguments.
	 * 
	 * @author Társis
	 * 
	 */
	public static final class Builder {
		private int l;
		private int o;
		private int sl;
		private int el;
		private int sc;
		private int ec;
		private String f;

		public Builder length(int l) {
			this.l = l;
			return this;
		}

		public Builder offSet(int o) {
			this.o = o;
			return this;
		}

		public Builder startLine(int sl) {
			this.sl = sl;
			return this;
		}

		public Builder startColumn(int sc) {
			this.sc = sc;
			return this;
		}

		public Builder endLine(int el) {
			this.el = el;
			return this;
		}

		public Builder endColumn(int ec) {
			this.ec = ec;
			return this;
		}

		public Builder filePath(String f) {
			this.f = f;
			return this;
		}

		public SelectionPosition build() {
			return new SelectionPosition(l, o, sl, sc, el, ec, f);
		}
	}
}
