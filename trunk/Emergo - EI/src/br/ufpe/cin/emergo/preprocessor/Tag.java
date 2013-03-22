package br.ufpe.cin.emergo.preprocessor;

public final class Tag {

	public static final String comment = "//";
	
	public static final String IFDEF = "#ifdef";
	
	public static final String IFNDEF = "#ifndef";
	
	public static final String ELSE = "#else";
	
	public static final String ENDIF = "#endif";
	
	public static final String INCLUDE = "#include";
	
	
	public static final String regex = "^\\s*"+comment+"(" + IFDEF + "|" + IFNDEF
			+ "|" + ELSE + "|" + ENDIF + "|" + INCLUDE
			+ ")\\s*(.*)\\s*$";
	
	public static final String ifdefRegex = "^\\s*"+comment+"(" + IFDEF + "|" + IFNDEF
			+ "|" + ELSE + ")\\s*(.*)\\s*$";
}
