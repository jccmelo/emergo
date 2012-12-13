package br.ufpe.cin.emergo.graph.transform;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DirectedMultigraph;

import dk.brics.util.file.WildcardExpander;

import soot.Body;
import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.DefinitionStmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import br.ufpe.cin.emergo.analysis.SootManager;
import br.ufpe.cin.emergo.analysis.reachingdefs.LiftedReachingDefinitions;
import br.ufpe.cin.emergo.analysis.reachingdefs.SimpleReachingDefinitions;
import br.ufpe.cin.emergo.graph.DependencyNode;
import br.ufpe.cin.emergo.graph.IntermediateDependencyGraphBuilder;
import br.ufpe.cin.emergo.graph.ValueContainerEdge;
import br.ufpe.cin.emergo.instrument.EagerConfigTag;
import br.ufpe.cin.emergo.instrument.FeatureInstrumentor;
import br.ufpe.cin.emergo.instrument.FeatureTag;
import br.ufpe.cin.emergo.instrument.IConfigRep;
import br.ufpe.cin.emergo.preprocessor.ContextManager;
import br.ufpe.cin.emergo.preprocessor.Preprocessor;
import br.ufpe.cin.emergo.util.ASTNodeUnitBridge;
import br.ufpe.cin.emergo.util.EmergoConstants;
import br.ufpe.cin.emergo.util.MethodDeclarationSootMethodBridge;
import br.ufpe.cin.emergo.util.Pair;
import br.ufpe.cin.emergo.util.SelectionNodesVisitor;
import br.ufpe.cin.emergo.compiler.Compiler;
import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.core.EmergoException;
import br.ufpe.cin.emergo.core.SelectionPosition;

public class GraphTransformer {
	
	private static TreeSet<Integer> lineNumbers = new TreeSet<Integer>();

	/**
	 * Holds the dependency graph.
	 */
	private static DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> useDefGraph;
	
	public static DirectedGraph<DependencyNode, ValueContainerEdge<ConfigSet>> generateDependencyGraph(
			final SelectionPosition selectionPosition,
			Map<Object, Object> options) throws EmergoException, ExecutionException {
		
		useDefGraph = null;

		// The file in which the selection resides.
		File selectionFile;
		selectionFile = new File(selectionPosition.getFilePath());
		if (!selectionFile.exists()) {
			throw new EmergoException("File " + selectionPosition.getFilePath()
					+ " not found.");
		}

		String rootpath = (String) options.get("rootpath");
		File ifdefSpecFile = new File(rootpath + File.separator + EmergoConstants.FEATURE_MODEL_FILE_NAME);
		if (!ifdefSpecFile.exists()) {
			throw new RuntimeException(
					"The " + EmergoConstants.FEATURE_MODEL_FILE_NAME + " of the project was not found at " + rootpath);
		}

		// Holds a the list of Files to be parsed by the compiler.
		List<File> javaFiles = new ArrayList<File>();
		
		// Holds a the list of jar Files to be parsed by the compiler.
		List<File> jarFiles = new ArrayList<File>();

		/*
		 * Builds the classpath in the format needed by Johnni Winther's
		 * compiler. Paths should be separated by a whitespace and may contain
		 * wildcards like ** and *.
		 * 
		 * For example:
		 * 
		 * ./src/**|/*.java ./tst/my/folder/*.java
		 * 
		 * (Ignore the '|' above)
		 */
		@SuppressWarnings("unchecked")
		List<File> sources = (List<File>) options.get("classpath");

		for (File file : sources) {
			if (file.isDirectory()) {
				String filepath = file.getPath() + File.separator + "**"
						+ File.separator + "*.groovy"; //java
				List<File> expandWildcards = WildcardExpander
						.expandWildcards(filepath);
				javaFiles.addAll(expandWildcards);
			} else if (file.isFile() && file.exists()) {
				jarFiles.add(file);
			}
		}

		String classpath = generateClassPath(jarFiles); 
		
		
		//==================
	
        /*
         * this visitor will compute the ASTNodes that were selected by the
         * user
         */
		ITextSelection textSelection = new TextSelection(selectionPosition.getOffSet(), selectionPosition.getLength());
        SelectionNodesVisitor selectionNodesVisitor = new SelectionNodesVisitor(textSelection);

        /*
         * Now we need to create a compilation unit for the file, and then
         * parse it to generate an AST in which we will perform our
         * analyses.
         *
         * TODO: is there a different way of doing this? Maybe eclipse has a
         * copy of the compilation unit in memory already?
         */
        IFile textSelectionFile = (IFile) new File(selectionPosition.getFilePath());
        CompilationUnit jdtCompilationUnit = GraphTransformer.getCompilationUnit(textSelectionFile);

        jdtCompilationUnit.accept(selectionNodesVisitor);
        Set<ASTNode> selectionNodes = selectionNodesVisitor.getNodes();

        for (ASTNode astNode : selectionNodes) {
        	GraphTransformer.lineNumbers.add(jdtCompilationUnit.getLineNumber(astNode.getStartPosition()));
        }
        System.out.println("Selection" + selectionNodes);
		
        /*
         * Initialize and configure Soot's options and find out which method
         * contains the selection
         */
        String correspondentClasspath = MethodDeclarationSootMethodBridge.getCorrespondentClasspath(textSelectionFile);
        SootManager.configure(correspondentClasspath);
        MethodDeclaration methodDeclaration = MethodDeclarationSootMethodBridge.getParentMethod(selectionNodes.iterator().next());
        String declaringMethodClass = methodDeclaration.resolveBinding().getDeclaringClass().getQualifiedName();
        MethodDeclarationSootMethodBridge mdsm = new MethodDeclarationSootMethodBridge(methodDeclaration);
        SootMethod sootMethod = SootManager.getMethodBySignature(declaringMethodClass, mdsm.getSootMethodSubSignature());
        Body body = sootMethod.retrieveActiveBody();
        
        
        /*
         * Maps ASTNodes to Units based on the line no.
         */
        Collection<Unit> unitsInSelection = ASTNodeUnitBridge.getUnitsFromLines(ASTNodeUnitBridge.getLinesFromASTNodes(selectionNodes, jdtCompilationUnit),
                body);
        if (unitsInSelection.isEmpty()) {
        	throw new EmergoException("the selection doesn't map to any Soot Unit");
        }
        
        /*
		 * Instruments in-memory Jimple code. 
		 */
		new FeatureInstrumentor().transform(body);

		/*
		 * Builds the CFG and runs the analysis.
		 */
		EagerConfigTag configTag = (EagerConfigTag) body
				.getTag(EagerConfigTag.TAG_NAME);

		if (configTag == null) {
			throw new IllegalStateException(
					"No EagerConfigTag found for body of method "
							+ body.getMethod());
		}

		Set<IConfigRep> configReps = configTag.getConfigReps();
		if (configReps.size() < 1) {
			throw new EmergoException("Set of configReps is less than 1.");
		}

		UnitGraph bodyGraph = new BriefUnitGraph(body);
		
		LiftedReachingDefinitions reachingDefinitions = new LiftedReachingDefinitions(bodyGraph, configReps);
		
		DependencyGraphBuilder graphBuilder = new DependencyGraphBuilder();
		useDefGraph = graphBuilder.generateDependencyGraph(bodyGraph, reachingDefinitions);
		
//		createProvidesGraph(unitsInSelection, reachingDefinitions, body);
		
		//==================
		
		
//		// Information about the user selection.
//		/*
//		 * XXX the +1 heres indicate that there is some coupling, for this class
//		 * knows how the information is passed from the client
//		 */
//		final int selectionStartLine = selectionPosition.getStartLine() + 1;
//		final int selecionEndLine = selectionPosition.getEndLine() + 1;
//		
//		final Set<String> pointsInUserSelection = manager.getFeaturesByLine(selectionStartLine);
//		
//		/*
//		 * Finds out in which method the user selection happened based on the
//		 * information inside the SelectionPosition instance.
//		 */
//		final AMethodDecl[] methodBox = new AMethodDecl[1];
//		final String filePath = selectionPosition.getFilePath();
//
//		AMethodDecl methodDecl = methodBox[0];
//		if (methodDecl == null) {
//			throw new IllegalArgumentException(
//					"Could not find enclosing method for the selection");
//		}
//		Method method = methodDecl.getMethod();
//
//	
//
//		/*
//		 * Now that there is enough information about the selection and the CFGs
//		 * have been generated, create the intermediate dependency graph.
//		 */
//		boolean interprocedural = (Boolean) options.get("interprocedural");
//		if (interprocedural) {
//			int depth = (Integer) options.get("interprocedural-depth");
//			int inline = (Integer) options.get("interprocedural-inline");
//			useDefGraph = IntermediateDependencyGraphBuilder
//					.buildInterproceduralGraph(cfg, depth, inline, selectionPosition);
//		} else {
//			useDefGraph = IntermediateDependencyGraphBuilder
//					.buildIntraproceduralGraph(cfg, pointsInUserSelection, methodDecl);
//		}

		return useDefGraph;
	}
	
	private static String generateClassPath(Collection<File> jarFiles) {
		StringBuilder classpath = new StringBuilder();
		for (File file : jarFiles) {
			classpath.append(file.getAbsolutePath() + File.pathSeparator);
		}
		String result = classpath.toString();
		return result.substring(0, result.length() - 1);
	}
	
	public static CompilationUnit getCompilationUnit(IFile textSelectionFile) {
        ICompilationUnit compilationUnit = JavaCore.createCompilationUnitFrom(textSelectionFile);
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(compilationUnit);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
        CompilationUnit jdtCompilationUnit = (CompilationUnit) parser.createAST(null);
        return jdtCompilationUnit;
    }

//	// TODO: extract *some* methods from this one.
//    private Map<Pair<Unit, Set<String>>, Set<Unit>> createProvidesGraph(Collection<Unit> unitsInSelection, LiftedReachingDefinitions reachingDefinitions,
//            Body body) {
//        Map<Pair<Unit, Set<String>>, Set<Unit>> unitConfigurationMap = new HashMap<Pair<Unit, Set<String>>, Set<Unit>>();
//        FeatureTag bodyFeatureTag = (FeatureTag) body.getTag("FeatureTag");
//
//        this.useDefGraph = new DirectedMultigraph<DependencyNode, ValueContainerEdge<ConfigSet>>((Class<? extends ValueContainerEdge<ConfigSet>>) ValueContainerEdge.class);
//
//        // for every unit in the selection...
//        for (Unit unitFromSelection : unitsInSelection) {
//            if (unitFromSelection instanceof DefinitionStmt) {
//                /*
//                 * exclude definitions when it's temp on the leftOp.
//                 */
//                DefinitionStmt definition = (DefinitionStmt) unitFromSelection;
//                Local leftOp = (Local) definition.getLeftOp();
//                if (leftOp.getName().contains("$")) {
//                    continue;
//                }
//
//                Set<String> featuresThatUseDefinition = new HashSet<String>();
//
//                // for every unit in the body...
//                Iterator<Unit> iterator = body.getUnits().snapshotIterator();
//                while (iterator.hasNext()) {
//                    Unit nextUnit = iterator.next();
//                    FeatureTag nextUnitTag = (FeatureTag) nextUnit.getTag("FeatureTag");
//
//                    List useAndDefBoxes = nextUnit.getUseAndDefBoxes();
//                    for (Object object : useAndDefBoxes) {
//                        ValueBox vbox = (ValueBox) object;
//                        if (vbox.getValue().equivTo(leftOp)) {
//                            featuresThatUseDefinition.addAll(nextUnitTag);
//                        }
//                    }
//
//                    LiftedFlowSet<Collection<Set<Object>>> liftedFlowAfter = reachingDefinitions.getFlowAfter(nextUnit);
//                    FlowSet[] lattices = liftedFlowAfter.getLattices();
//
//                    // and for every configuration...
//                    for (int latticeIndex = 0; latticeIndex < lattices.length; latticeIndex++) {
//                        FlowSet flowSet = lattices[latticeIndex];
//                        Set<String> currConfiguration = bodyFeatureTag.getConfigurationForId(latticeIndex);
//
//                        // if the unit belongs to the current configuration...
//                        if (nextUnitTag.belongsToConfiguration(currConfiguration)) {
//
//                            // if the definition reaches this unit...
//                            if (flowSet.contains(definition)) {
//                                List<ValueBox> useBoxes = nextUnit.getUseBoxes();
//                                for (ValueBox vbox : useBoxes) {
//                                    /*
//                                     * and the definition is used, add to the
//                                     * map (graph)...
//                                     */
//                                    if (vbox.getValue().equivTo(leftOp)) {
//                                        Pair<Unit, Set<String>> currentPair = new Pair<Unit, Set<String>>(definition, currConfiguration);
//                                        Set<Unit> unitConfigurationReachesSet = unitConfigurationMap.get(currentPair);
//
//                                        if (!useDefGraph.containsVertex(definition)) {
//                                        	useDefGraph.addVertex(definition);
//                                        }
//                                        if (!useDefGraph.containsVertex(nextUnit)) {
//                                        	useDefGraph.addVertex(nextUnit);
//                                        }
//
//                                        Set<ValueContainerEdge<Set<String>>> allEdges = useDefGraph.getAllEdges(definition, nextUnit);
//                                        if (allEdges.size() >= 1) {
//                                            int diffCounter = 0;
//                                            Iterator<ValueContainerEdge<Set<String>>> edgesIterator = allEdges.iterator();
//                                            Set<ValueContainerEdge<Set<String>>> edgeRemovalSchedule = new HashSet<ValueContainerEdge<Set<String>>>();
//                                            while (edgesIterator.hasNext()) {
//                                                ValueContainerEdge<Set<String>> valueContainerEdge = (ValueContainerEdge<Set<String>>) edgesIterator.next();
//                                                Set<String> valueConfiguration = valueContainerEdge.getValue();
//                                                Integer idForConfiguration = 0;// bodyFeatureTag.getConfigurationForId(valueConfiguration);
//                                                FlowSet flowSetFromOtherReached = lattices[idForConfiguration];
//                                                if (flowSetFromOtherReached.equals(flowSet)) {
//                                                    /*
//                                                     * Se a configurao que
//                                                     * estiver "querendo" entrar
//                                                     * for menor, ento ela
//                                                     * expulsar os maiores.
//                                                     */
//                                                    if (valueConfiguration.size() > currConfiguration.size()
//                                                            && featuresThatUseDefinition.containsAll(currConfiguration)) {
//                                                        edgeRemovalSchedule.add(valueContainerEdge);
//                                                        ValueContainerEdge<Set<String>> addEdge = useDefGraph.addEdge(definition, nextUnit);
//                                                        addEdge.setValue(currConfiguration);
//                                                        continue;
//                                                    }
//                                                } else {
//                                                    diffCounter++;
//                                                }
//                                            }
//                                            if (diffCounter == allEdges.size() && featuresThatUseDefinition.containsAll(currConfiguration)) {
//                                                ValueContainerEdge<Set<String>> addEdge = useDefGraph.addEdge(definition, nextUnit);
//                                                addEdge.setValue(currConfiguration);
//                                            }
//                                            useDefGraph.removeAllEdges(edgeRemovalSchedule);
//                                        } else {
//                                            ValueContainerEdge<Set<String>> addEdge = useDefGraph.addEdge(definition, nextUnit);
//                                            addEdge.setValue(currConfiguration);
//                                        }
//
//                                        if (unitConfigurationReachesSet == null) {
//                                            unitConfigurationReachesSet = new HashSet<Unit>();
//                                            unitConfigurationReachesSet.add(nextUnit);
//                                            unitConfigurationMap.put(currentPair, unitConfigurationReachesSet);
//                                        } else {
//                                            unitConfigurationReachesSet.add(nextUnit);
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//                System.out.println("features that use the definition at issue: " + featuresThatUseDefinition);
//            }
//        }
//
//        return unitConfigurationMap;
//    }
}
