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
 * @author Társis
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

	private SelectionPosition(int l, int o, int sl, int sc, int el, int ec, String f) {
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
			if (f == null) {
				throw new IllegalArgumentException("The filepath is null");
			}
			return new SelectionPosition(l, o, sl, sc, el, ec, f);
		}
	}
}
