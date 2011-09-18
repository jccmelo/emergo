package br.ufpe.cin.emergo.core;

import static dk.au.cs.java.compiler.Util.showPhaseProgress;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;

import br.ufpe.cin.emergo.graph.IntermediateDependencyGraphBuilder;
import br.ufpe.cin.emergo.graph.ValueContainerEdge;

import dk.au.cs.java.compiler.ErrorType;
import dk.au.cs.java.compiler.Errors;
import dk.au.cs.java.compiler.Flags;
import dk.au.cs.java.compiler.Main;
import dk.au.cs.java.compiler.cfg.ControlFlowGraph;
import dk.au.cs.java.compiler.cfg.analysis.PointVisitor;
import dk.au.cs.java.compiler.cfg.gen.CFGGenerator;
import dk.au.cs.java.compiler.cfg.point.Point;
import dk.au.cs.java.compiler.check.DisambiguationCheck;
import dk.au.cs.java.compiler.check.EnvironmentsCheck;
import dk.au.cs.java.compiler.check.HierarchyCheck;
import dk.au.cs.java.compiler.check.TypeLinkingCheck;
import dk.au.cs.java.compiler.check.WeedingCheck;
import dk.au.cs.java.compiler.ifdef.IfDefBDDAssigner;
import dk.au.cs.java.compiler.ifdef.IfDefUtil;
import dk.au.cs.java.compiler.ifdef.IfDefVarSet;
import dk.au.cs.java.compiler.ifdef.SharedSimultaneousAnalysis;
import dk.au.cs.java.compiler.lexer.Lexer;
import dk.au.cs.java.compiler.lexer.LexerException;
import dk.au.cs.java.compiler.node.ACompilationUnit;
import dk.au.cs.java.compiler.node.AMethodDecl;
import dk.au.cs.java.compiler.node.AProgram;
import dk.au.cs.java.compiler.node.Start;
import dk.au.cs.java.compiler.node.TIdentifier;
import dk.au.cs.java.compiler.node.Token;
import dk.au.cs.java.compiler.parser.Parser;
import dk.au.cs.java.compiler.parser.ParserException;
import dk.au.cs.java.compiler.phases.AbstractPhase;
import dk.au.cs.java.compiler.phases.ConstantFolding;
import dk.au.cs.java.compiler.phases.Disambiguation;
import dk.au.cs.java.compiler.phases.Environments;
import dk.au.cs.java.compiler.phases.Hierarchy;
import dk.au.cs.java.compiler.phases.Reachability;
import dk.au.cs.java.compiler.phases.Resources;
import dk.au.cs.java.compiler.phases.TargetResolver;
import dk.au.cs.java.compiler.phases.TypeChecking;
import dk.au.cs.java.compiler.phases.TypeLinking;
import dk.au.cs.java.compiler.phases.Weeding;
import dk.au.cs.java.compiler.phases.XACTDesugaring;
import dk.au.cs.java.compiler.phases.promotion.PromotionInference;
import dk.au.cs.java.compiler.type.environment.ClassEnvironment;
import dk.au.cs.java.compiler.type.environment.InternalLookup;
import dk.au.cs.java.compiler.type.environment.TypeDeclaration;
import dk.au.cs.java.compiler.type.members.Method;
import dk.brics.util.file.WildcardExpander;

/**
 * This class uses the Experimental Java Compiler, by Johnni Winther, as the flow analysis framework.
 * 
 * @author Társis
 * 
 */
public class JWCompilerDependencyFinder {

	/**
	 * Holds the dependency graph.
	 */
	private DirectedGraph<Object, ValueContainerEdge> useDefWeb;

	/**
	 * 
	 * 
	 * @param selectionPosition
	 * @param options
	 * @throws FileNotFoundException
	 */
	public JWCompilerDependencyFinder(final SelectionPosition selectionPosition, Map<Object, Object> options) throws FileNotFoundException {
		// Tesets compiler status.
		Main.resetCompiler();

		// The file in which the selection resides.
		File selectionFile;
		selectionFile = new File(selectionPosition.getFilePath());
		if (!selectionFile.exists()) {
			throw new FileNotFoundException("File " + selectionPosition.getFilePath() + " not found.");
		}

		String rootpath = (String) options.get("rootpath");
		File ifdefSpecFile = new File(rootpath + File.separator + "ifdef.txt");
		if (!ifdefSpecFile.exists()) {
			throw new RuntimeException("The ifdef.txt of the project was not found at " + rootpath);
		}

		EnumSet<Flags> flags = (EnumSet<Flags>) Main.FLAGS;
		flags.add(Flags.IFDEF);
		SharedSimultaneousAnalysis.useSharedSetStrategy(true);
		IfDefUtil.parseIfDefSpecification(ifdefSpecFile);

		// Holds a the list of Files to be parsed by the compiler.
		List<File> files = new ArrayList<File>();

		/*
		 * Builds the classpath in the format needed on Johnni Winther's compiler. Paths should be separated by a
		 * whitespace and may contain wildcards like ** and *.
		 * 
		 * For example:
		 * 
		 * ./src/**|/*.java ./tst/my/folder/*.java
		 * 
		 * (Ignore the '|' above)
		 */
		@SuppressWarnings("unchecked")
		List<File> classpath = (List<File>) options.get("classpath");

		for (File file : classpath) {
			if (file.isDirectory()) {
				String filepath = file.getPath() + File.separator + "**" + File.separator + "*.java";
				List<File> expandWildcards = WildcardExpander.expandWildcards(filepath);
				files.addAll(expandWildcards);

			} else if (file.isFile() && file.exists()) {
				// XXX treat .jar files.
			}
		}

		// XXX find out classpath
		ClassEnvironment.init("", true);

		// The root point of the parsed program.
		AProgram node = parseProgram(files);

		// XXX: Surprisingly, this is necessary to prevent the compiler from throwing NPEs.
		Main.program = node;

		// XXX I don't know what this is for yet...
		node.setOptionalInvariant(true);

		/*
		 * Apply compiler phases to the root node.
		 * 
		 * TODO: Check if some of these can be removed to speed things up.
		 */
		node.apply(new Weeding());
		node.apply(new WeedingCheck());
		node.apply(new IfDefBDDAssigner());
		node.apply(new Environments());
		node.apply(new EnvironmentsCheck());
		node.apply(new TypeLinking());
		node.apply(new TypeLinkingCheck());
		node.apply(new Hierarchy());
		node.apply(new HierarchyCheck());
		node.apply(new Disambiguation());
		node.apply(new DisambiguationCheck());
		node.apply(new TargetResolver());
		node.apply(new Reachability());
		// Comment line below to disable constant folding optimizations.
		// node.apply(new ConstantFolding());
		node.apply(new TypeChecking());
		node.apply(new CFGGenerator());
		node.apply(new PromotionInference());
		node.apply(new XACTDesugaring());
		node.apply(new Resources());
		Errors.check();

		/*
		 * Helpful information about the method at issue is provided by the client.
		 * 
		 * type: the class name method:
		 * 
		 * the method within the class name
		 * 
		 * methodDescriptor: a bytecode descriptor of the method
		 */
		String entryPoint = (String) options.get("type");
		String methodName = (String) options.get("method");
		String methodDescriptor = (String) options.get("methodDescriptor");

		// Information about the user selection.
		/*
		 * XXX the +1 heres indicate that there is some coupling, for this class knows how the information is passed
		 * from the client
		 */
		final int selectionStartLine = selectionPosition.getStartLine() + 1;

		TypeDeclaration typeDeclaration = ClassEnvironment.lookupCanonicalName(new InternalLookup(entryPoint, false));
		final Method method = typeDeclaration.getSelfType().getMethod(methodName, methodDescriptor);
		ControlFlowGraph cfg = method.getControlFlowGraph();

		final List<Point> pointsInUserSelection = new ArrayList<Point>();

		/*
		 * XXX this selection boundary check is *very* broken. Use the information on the SelectionPosition to get a
		 * more precise set.
		 */
		cfg.apply(new PointVisitor() {
			@Override
			protected Object defaultPoint(Point point, Object question) {
				Token token = point.getToken();
				int line = token.getLine();
				if (line == selectionStartLine) {
					pointsInUserSelection.add(point);
				}
				return null;
			}
		});

		/*
		 * XXX use this to find out if Johnni fixed the bug that cause all nodes to marked as true instead of having a
		 * feature expression.
		 */
		cfg.apply(new PointVisitor() {
			@Override
			protected Object defaultPoint(Point point, Object question) {
				IfDefVarSet varSet = point.getVarSet();
				System.out.println(varSet);
				return null;
			}
		});

		/*
		 * XXX this is a *very* expensive way of finding out the AMethodDecl from a Method. Look for a faster/easier
		 * way.
		 */
		final AMethodDecl[] methodDeclBox = new AMethodDecl[1];
		node.apply(new AbstractPhase() {
			Method m = method.getDeclaringMember();

			@Override
			public void inAMethodDecl(AMethodDecl node) {
				if (node.getMethod().equals(m)) {
					methodDeclBox[0] = node;
				}
			}
		});

		/*
		 * Now that there is enough information about the selection and the CFGs have been generated, create the
		 * intermediate depency graph.
		 */
		DirectedGraph<Object, ValueContainerEdge> useDefWeb = IntermediateDependencyGraphBuilder.build(node, cfg, pointsInUserSelection, methodDeclBox[0]);
		this.useDefWeb = useDefWeb;
	}

	// XXX this method was copied from dk...compiler.Main. Go through it again.
	private static AProgram parseProgram(List<File> sourceFiles) {
		final List<ACompilationUnit> sources = new /* CopyOnWrite */ArrayList<ACompilationUnit>();

		for (final File file : sourceFiles) {
			try {
				showPhaseProgress();
				FileInputStream fis = new FileInputStream(file);
				Parser parser = new Parser(new Lexer(fis));
				Start startsym = parser.parse();
				fis.close();
				ACompilationUnit cu = startsym.getCompilationUnit();
				cu.setToken(new TIdentifier(file.getPath(), 0, 0));
				cu.setFile(file);
				sources.add(cu);
			} catch (FileNotFoundException e) {
				Errors.errorMessage(ErrorType.FILE_OPEN_ERROR, "File " + file.getPath() + " not found");
				Errors.check(); // no use in parsing of not all files can be found
			} catch (LexerException e) {
				Errors.error(ErrorType.LEXER_EXCEPTION, file, e.getLine(), e.getPos(), "Syntax error at " + file.getPath() + " " + e.getMessage(), false);
			} catch (ParserException e) {
				Errors.error(ErrorType.PARSER_EXCEPTION, file, e.getToken().getLine(), e.getToken().getPos(), "Syntax error at " + file.getPath() + " " + e.getMessage(), false);
			} catch (IOException e) {
				Errors.errorMessage(ErrorType.IO_ERROR, "Error reading file " + file.getPath() + ": " + e.getMessage());
				Errors.check(); // no use in parsing of not all files can be read
			}
		}
		AProgram node = new AProgram(new TIdentifier("AProgram", 0, 0), sources);

		// enables the runtime tree invariant
		node.setOptionalInvariant(true);

		return node;
	}

	/**
	 * Gets the dependency graph.
	 * 
	 * @return a graph representing the dependencies found
	 */
	public DirectedGraph<Object, ValueContainerEdge> getGraph() {
		return this.useDefWeb;
	}
};
