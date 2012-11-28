package br.ufpe.cin.emergo.compiler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Logger;
 
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
 
/**
 * This class is responsible for compiling the groovy classes 
 * using dynamic compilation API.
 */
public class Compiler {
	private String classCompleteName; //path+name+extension
	private String className; // just classname
    final static Logger logger = Logger.getLogger(Compiler.class.getName()) ;
    
    public Compiler(String classCompleteName){
    	this.classCompleteName = classCompleteName;
    	this.className = retrieveClassName(classCompleteName);
    }

	/**
     * Does the required object initialization and compilation.
     * @throws IOException 
     */
    public void doCompilation() throws IOException{
    	
    	/*Getting source code to be compiled dynamically */
    	String sourceCode = getSourceCode();
    	
        /*Creating dynamic java source code file object*/
//        SimpleJavaFileObject fileObject = new DynamicJavaSourceCodeObject("com.accordess.ca.DynamicCompilationHelloWorld", sourceCode.toString()) ;
    	SimpleJavaFileObject fileObject = new DynamicJavaSourceCodeObject(className, sourceCode) ;
        JavaFileObject javaFileObjects[] = new JavaFileObject[]{fileObject};
 
        /*Instantiating the java compiler*/
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
 
        /**
         * Retrieving the standard file manager from compiler object, which is used to provide
         * basic building block for customizing how a compiler reads and writes to files.
         *
         * The same file manager can be reopened for another compiler task.
         * Thus we reduce the overhead of scanning through file system and jar files each time
         */
        StandardJavaFileManager stdFileManager = compiler.getStandardFileManager(null, Locale.getDefault(), null);
 
        /* Prepare a list of compilation units (java source code file objects) to input to compilation task*/
        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(javaFileObjects);
 
        /*Prepare any compilation options to be used during compilation*/
        //In this example, we are asking the compiler to place the output files under bin folder.
        String[] compileOptions = new String[]{"-d", "bin"} ;
        Iterable<String> compilationOptionss = Arrays.asList(compileOptions);
 
        /*Create a diagnostic controller, which holds the compilation problems*/
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
 
        /*Create a compilation task from compiler by passing in the required input objects prepared above*/
        CompilationTask compilerTask = compiler.getTask(null, stdFileManager, diagnostics, compilationOptionss, null, compilationUnits) ;
 
        //Perform the compilation by calling the call method on compilerTask object.
        boolean status = compilerTask.call();
 
        if (!status){//If compilation error occurs
            /*Iterate through each compilation problem and print it*/
            for (Diagnostic diagnostic : diagnostics.getDiagnostics()){
                System.out.format("Error on line %d in %s \n", diagnostic.getLineNumber(), diagnostic);
                throw new IOException("\nError while compiling");
            }
        }
        try {
            stdFileManager.close() ;//Close the file manager
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private String getSourceCode() throws IOException {
    	BufferedReader br = new BufferedReader(new FileReader(classCompleteName));
    	
    	String currentLine;
    	StringBuffer sourceCode = new StringBuffer();
    	/*Getting source code to be compiled dynamically */
    	while ((currentLine = br.readLine()) != null) {
    		sourceCode.append(currentLine);
    		sourceCode.append("\n");
    	}
    	br.close();
    	
    	return sourceCode.toString();
	}

	/**
     * This method retrieves the classname through
     * attribute classCompleteName.
     * @return the classname
     */
    public static String retrieveClassName(String completeName) {
    	//Getting all parts of the path+classname+extension
    	String[] parts = completeName.split("/");
    	//Getting the classname without extension as well as path
    	String className = parts[parts.length-1].split("\\.")[0];
    	
    	return className;
	}
 
    public static void main(String args[]){
        try {
			new Compiler("Out.groovy").doCompilation();
			logger.info("Bytecode generated successfully.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
 
}
 
/**
 * Creates a dynamic source code file object
 *
 * This is an example of how we can prepare a dynamic java source code for compilation.
 * This class reads the java code from a string and prepares a JavaFileObject
 *
 */
class DynamicJavaSourceCodeObject extends SimpleJavaFileObject{
    private String qualifiedName;
    private String sourceCode;
 
    /**
     * Converts the name to an URI, as that is the format expected by JavaFileObject
     *
     *
     * @param fully qualified name given to the class file
     * @param code the source code string
     */
    protected DynamicJavaSourceCodeObject(String name, String code) {
        super(URI.create("string:///" +name.replaceAll("\\.", "/") + Kind.SOURCE.extension), Kind.SOURCE);
        this.qualifiedName = name ;
        this.sourceCode = code ;
    }
 
    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors)
            throws IOException {
        return sourceCode ;
    }
 
    public String getQualifiedName() {
        return qualifiedName;
    }
 
    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }
 
    public String getSourceCode() {
        return sourceCode;
    }
 
    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }
}
