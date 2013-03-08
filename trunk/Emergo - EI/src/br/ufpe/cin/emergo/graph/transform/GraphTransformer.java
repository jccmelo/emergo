package br.ufpe.cin.emergo.graph.transform;

import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.ITextSelection;

import br.ufpe.cin.emergo.handlers.ClassNodeOperationGroovy;

public class GraphTransformer {
	
	public static TreeSet<Integer> lineNumbers = new TreeSet<Integer>();
	
	
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
	
	public static MethodNode getGroovyCompilationUnit(IFile textSelectionFile, Map<Object, Object> options) {
		
		String scriptLocation = (String) options.get("selectionFile"); //.groovy
		
		//Configure
		CompilerConfiguration conf = new CompilerConfiguration();
		conf.setTargetDirectory((String) options.get("correspondentClasspath")); // until bin folder
		
		ArrayList<String> p = (ArrayList<String>) options.get("classpath");
		Object cp = p.get(0);
		
		conf.setClasspath(cp.toString());
		
		//Compile
		GroovyClassLoader gcl = new GroovyClassLoader();
		org.codehaus.groovy.control.CompilationUnit cu = new org.codehaus.groovy.control.CompilationUnit(gcl);
		cu.setConfiguration(conf);
		cu.configure(conf);
		cu.addPhaseOperation(new ClassNodeOperationGroovy((ITextSelection) options.get("textSelection")), Phases.SEMANTIC_ANALYSIS);
		SourceUnit sourceUnit = cu.addSource(new File(scriptLocation)); //add more sources if it is needed
		sourceUnit.configure(conf);
		cu.setClassLoader(gcl);
		cu.compile();
		
		ClassNode classNode = cu.getFirstClassNode();
		
		List<MethodNode> methods = classNode.getDeclaredMethods((String)options.get("methodName")); //method name
		MethodNode methodNode = methods.get(0);
		
		return methodNode;
		
    }
	
	/**
     * This returns a string for the IFile that is used to generate the keys
     * for classNode maps and moduleNode maps.
     * @param file
     * @return
     */
    public static String getSourceFileKey(final IFile file) {
        return file.getRawLocation().toOSString();
    }
    
//	// TODO: extract *some* methods from this one.
//    private Map<Pair<Unit, IFeatureRep>, Set<Unit>> createProvidesGraph(Collection<Unit> unitsInSelection, LiftedReachingDefinitions reachingDefinitions,
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
//                Set<IFeatureRep> featuresThatUseDefinition = new HashSet<IFeatureRep>();
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
//                            featuresThatUseDefinition.add(nextUnitTag.getFeatureRep());
//                        }
//                    }
//
//                    EagerMapLiftedFlowSet liftedFlowAfter = reachingDefinitions.getFlowAfter(nextUnit);
//                    FlowSet[] lattices = liftedFlowAfter.getLattice(); //getLattices();
//                    
//
//                    // and for every configuration...
//                    for (int latticeIndex = 0; latticeIndex < lattices.length; latticeIndex++) {
//                        FlowSet flowSet = lattices[latticeIndex];
//                        IFeatureRep currConfiguration = bodyFeatureTag.getFeatureRep();//getConfigurationForId(latticeIndex);
//
//                        // if the unit belongs to the current configuration...
//                        if (nextUnitTag.belongsToConfiguration(currConfiguration.)) {
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
//                                        Pair<Unit, IFeatureRep> currentPair = new Pair<Unit, IFeatureRep>(definition, currConfiguration);
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
