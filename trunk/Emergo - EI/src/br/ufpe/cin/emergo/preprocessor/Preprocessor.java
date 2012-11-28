package br.ufpe.cin.emergo.preprocessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.tools.ant.BuildException;

public class Preprocessor {

	private String defs; // defs are the features (e.g. DEBUG, LOGGING, etc)
	private String incdir;

	public void execute() throws BuildException {
		ContextManager context = ContextManager.getContext();

		if (context.getSrcfile() == null || context.getDestfile() == null) {
			throw new BuildException(
					"Some parameter missed. Make sure that definition list, input and output files are provided.");
		}

		try {
			preprocess();
		} catch (IOException e) {
			throw new BuildException("IO error while preprocessing", e);
		}
	}

	private void preprocess() throws IOException {
		ContextManager context = ContextManager.getContext();

		BufferedReader br = null; // for reading from file
		BufferedWriter bw = null; // for writing on file

		try {

			br = new BufferedReader(new FileReader(context.getSrcfile()));
			bw = new BufferedWriter(new FileWriter(context.getDestfile()));

			/**
			 * Gets the defined features, split them by "," and, finally, remove
			 * all duplicate white spaces
			 */		
			Set set = new HashSet(Arrays.asList(defs.replaceAll("\\s+", "")
					.split(",")));

			// Compiles the regex then sets the pattern
			Pattern pattern = Pattern.compile(Tag.regex,
					Pattern.CASE_INSENSITIVE);

			String line;
			int lineNumber = 0; // for counting the line number
			int currentLevel = 0; // for controling the tags (e.g. ifdefs and
									// endifs)
			int removeLevel = -1; // if -1 can write, otherwise cannot
			boolean skip = false; // this flag serves to control code within a
									// certain feature

			// reading line-by-line from input file
			while ((line = br.readLine()) != null) {
				lineNumber++;
				String infoLine = String.valueOf(lineNumber);

				/**
				 * Creates a matcher that will match the given input against
				 * this pattern.
				 */
				Matcher matcher = pattern.matcher(line);

				/**
				 * Matches the defined pattern with the current line
				 */
				if (matcher.matches()) {
					bw.write(line);
					bw.newLine(); // for keeping the real line number

					/**
					 * MatchResult is unaffected by subsequent operations
					 */
					MatchResult result = matcher.toMatchResult();
					String dir = result.group(1).toLowerCase(); // preprocessor
																// directives
					String param = result.group(2); // feature's name

					if (Tag.IFDEF.equals(dir)) {
						//adds ifdef X on the stack
						context.addDirective(dir + " "
								+ param.replaceAll("\\s", ""));

						// verifies if the feature was defined
						if (defs.replaceAll("\\s+", "").length() > 0) {
							skip = !set.contains(param);
						} else {
							skip = false;
						}
						

						if (removeLevel == -1 && skip) {
							removeLevel = currentLevel;
						}
						currentLevel++;
						continue;
					} else if (Tag.IFNDEF.equals(dir)) {

						context.addDirective(dir + " " + param);
						
						if (defs.replaceAll("\\s+", "").length() > 0) {
							skip = set.contains(param);
						} else {
							skip = false;
						}
						
						if (removeLevel == -1 && skip) {
							removeLevel = currentLevel;
						}
						currentLevel++;
						continue;
					} else if (Tag.ELSE.equals(dir)) {
						currentLevel--;
						if (currentLevel == removeLevel) {
							removeLevel = -1;
						}
						if (removeLevel == -1 && !skip) {
							removeLevel = currentLevel;
						}
						currentLevel++;
						continue;
					} else if (Tag.ENDIF.equals(dir)) {

						if (context.stackIsEmpty()) {
							System.out
									.println("#endif encountered without corresponding #ifdef or #ifndef");
							return;
						}

						context.removeTopDirective();

						currentLevel--;
						if (currentLevel == removeLevel) {
							removeLevel = -1;
						}
						continue;
					} else if (Tag.INCLUDE.equals(dir)) {
						include(param, bw);
						continue;
					}
				} else {
					
					/**
					 * verifies if the current line 
					 *    does not have text (code)
					 */
					if(!line.trim().isEmpty()){
						/**
						 * Add information on mapping between 
						 * feature expression and line number.
						 */
						addInfoOnMapping(context, lineNumber);
					}
				}
				if (removeLevel == -1 || currentLevel < removeLevel) {
					bw.write(line);
					bw.newLine();
				}
			}
		} finally {
			if (br != null) {
				br.close();
			}
			if (bw != null) {

				Map2Xml xml = new Map2Xml();
				try {
					/* writing in a metadata file about this class */
					String nameClass = context.getDestfile().split("\\.")[0];
					
					FileWriter fw = new FileWriter(new File(nameClass+".xml"));

					xml.toXml(context.getMapFeatures(), fw);
				} catch (XMLStreamException e) {
					System.out.println("Error on xml generation");
					e.printStackTrace();
				}

				bw.close();
			}
		}

	}
	
	private void addInfoOnMapping(ContextManager context, Integer infoLine){
		// verifica pelo contexto se o a linha atual pertence alguma
		// feature se sim, add no map. Caso contrario, faz nada.
		Stack<String> auxStack = new Stack<String>();

		// copy stack
		for (int i = 0; i < context.stackSize(); i++) {
			auxStack.add(context.stackDirectives.get(i));
		}

		while (!auxStack.isEmpty()) {
			// gets feature's name
			String feature = auxStack.peek().split(" ")[1];

			if (auxStack.peek().contains(Tag.IFNDEF)) {
				feature = "~" + feature;
			}
			
			// add info about line number of a certain feature
			context.addFeatureInfo(feature, infoLine);
			context.addInfo(infoLine, feature);
			
			auxStack.pop();
		}
		
		auxStack.clear();
	}

	private void include(String file, BufferedWriter bw) throws IOException {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(incdir, file)));
			String line;
			while ((line = br.readLine()) != null) {
				bw.write(line);
				bw.newLine();
			}
		} finally {
			if (br != null) {
				br.close();
			}
		}
	}

	public String getDefs() {
		return defs;
	}

	public String getIncdir() {
		return incdir;
	}

	public void setDefs(String defs) {
		this.defs = defs;
	}

	public void setIncdir(String incdir) {
		this.incdir = incdir;
	}

	public static void main(String[] args) {

		// sets the IO files
		ContextManager manager = ContextManager.getContext();
		manager.setSrcfile("Out.groovy");
		manager.setDestfile("Testclass2.groovy");

		Preprocessor pp = new Preprocessor();

		String defs = "";//"A , SOMA";
		pp.setDefs(defs);
		
		// TODO create the graphic interface
		pp.execute();
	}
}