package br.ufpe.cin.emergo.handlers;

import static dk.au.cs.java.compiler.Util.showPhaseProgress;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dk.au.cs.java.compiler.ErrorType;
import dk.au.cs.java.compiler.Errors;
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

public class JWCompilerDependencyFinder {

	public JWCompilerDependencyFinder(final SelectionPosition selectionPosition, Map<Object, Object> options) throws FileNotFoundException {
		Main.resetCompiler();

		File selectionFile;
		selectionFile = new File(selectionPosition.getFilePath());
		if (!selectionFile.exists()) {
			throw new FileNotFoundException("File " + selectionPosition.getFilePath() + " not found.");
		}

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

		AProgram node = parseProgram(files);

		/*
		 * XXX: Surprisingly, this is necessary so that compiler phases does not throw NPEs.
		 */
		Main.program = node;

		// XXX I don't know what this is for...
		node.setOptionalInvariant(true);

		/*
		 * Apply compiler phases to the root node.
		 * 
		 * TODO: Check if some of these can be removed to speed thing up.
		 */
		node.apply(new Weeding());
		node.apply(new WeedingCheck());
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
		node.apply(new TypeChecking());
		node.apply(new PromotionInference());
		node.apply(new CFGGenerator());
		node.apply(new XACTDesugaring());
		node.apply(new Resources());

		/*
		 * Helpful information about the method at issue.
		 */
		String entryPoint = (String) options.get("type");
		String methodName = (String) options.get("method");
		String methodDescriptor = (String) options.get("methodDescriptor");

		/*
		 * Information about the user selection.
		 */
		final int selectionStartLine = selectionPosition.getStartLine() + 1;

		TypeDeclaration typeDeclaration = ClassEnvironment.lookupCanonicalName(new InternalLookup(entryPoint, false));
		final Method method = typeDeclaration.getSelfType().getMethod(methodName, methodDescriptor);
		ControlFlowGraph cfg = method.getControlFlowGraph();

		final List<Point> pointsInUserSelection = new ArrayList<Point>();

		/*
		 * XXX this selection boundary check is *very* broken. Use the information on the SelectionPosition to get a
		 * more precise set
		 */
		cfg.apply(new PointVisitor() {
			@Override
			protected Object defaultPoint(Point point, Object question) {
				Token token = point.getToken();
				int line = token.getLine();
				int pos = token.getPos();
				if (line == selectionStartLine) {
					pointsInUserSelection.add(point);
				}
				return super.defaultPoint(point, question);
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

		// Generate the intermediate depency graph.
		IntermediateDependencyGraphBuilder.build(node, cfg, pointsInUserSelection, methodDeclBox[0]);

	}

	// XXX this method was copied from dk.au...compiler.Main. Go through it again.
	private static AProgram parseProgram(List<File> sourceFiles) {
		final List<ACompilationUnit> sources = new /* CopyOnWrite */ArrayList<ACompilationUnit>();

		// LinkedList<Thread> threads = new LinkedList<Thread>();
		for (final File file : sourceFiles) {
			/*
			 * Thread thread = new Thread() { public void run() {
			 */
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
		/*
		 * }; threads.add(thread); thread.start(); } for (Thread thread : threads) { try { thread.join(); } catch
		 * (InterruptedException e) { } }
		 */
		AProgram node = new AProgram(new TIdentifier("AProgram", 0, 0), sources);
		node.setOptionalInvariant(true); // enables the runtime tree
											// invariant
		return node;
	}
};
