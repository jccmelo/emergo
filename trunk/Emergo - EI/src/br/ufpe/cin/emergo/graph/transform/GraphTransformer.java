package br.ufpe.cin.emergo.graph.transform;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import br.ufpe.cin.emergo.analysis.SootManager;
import br.ufpe.cin.emergo.analysis.reachingdefs.SimpleReachingDefinitions;
import br.ufpe.cin.emergo.instrument.FeatureInstrumentor;
import br.ufpe.cin.emergo.preprocessor.ContextManager;
import br.ufpe.cin.emergo.preprocessor.Preprocessor;
import br.ufpe.cin.emergo.compiler.Compiler;

public class GraphTransformer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		/*
		 * First of all, (1) the preprocessing is performed.
		 */
		ContextManager manager = ContextManager.getContext();
		manager.setSrcfile("SimpleRDTest.groovy"); // input class
		manager.setDestfile("SimpleRD.groovy"); // output class

		Preprocessor pp = new Preprocessor();

		String defs = "";// "A , SOMA";
		pp.setDefs(defs);

		// TODO create the graphic interface
		pp.execute();

		/*
		 * (2) Compiles the output class
		 */
		try {
			new Compiler(manager.getDestfile()).doCompilation();
		} catch (IOException e) {
			System.out.println("Unexpected error while compiling");
			e.printStackTrace();
		}
		
		/*
		 * (3) Loads and configures the output class with the Soot Framework
		 */
		// Set up the class we’re working with
		SootManager.configure("../Preprocessor4SPL/bin/");
		SootClass c = SootManager.loadAndSupport(Compiler.retrieveClassName(manager.getDestfile()));

		// Retrieve the method and its body
		SootMethod m = c.getMethodByName("main");
		Body b = m.retrieveActiveBody();

		/*
		 * (4) Instruments bytecode of the output class
		 */
		new FeatureInstrumentor().transform(b);

		/*
		 * (5) Builds the CFG, runs the analyses and interprets the results
		 */

		// --------------------------------------------------------------------
		// Analysis: SimpleReachingDefinitions
		// --------------------------------------------------------------------
		BriefUnitGraph briefUnitGraph = new BriefUnitGraph(b);

		SimpleReachingDefinitions reachingDefinitions = new SimpleReachingDefinitions(
				briefUnitGraph);

		runAnalysis(reachingDefinitions, briefUnitGraph, "SimpleRDAnalysis");
		
		/*
		 * (6) Transforms the graph
		 */
		new DependencyGraphBuilder();
		
	}
	
	/**
	 * This method is responsible by the execution of the analyses
	 * and it generates one .txt report for each analysis.
	 * 
	 * @param analysis
	 * @param graph
	 * @param fileName
	 */
	private static void runAnalysis(
			ForwardFlowAnalysis<Unit, ? extends FlowSet> analysis,
			UnitGraph graph, String fileName) {

		// Iterate over the results
		Iterator i = graph.iterator();


		String path = "/Users/paolaaccioly/" + fileName + ".txt";
		BufferedWriter writer = null;

		try {
			writer = new BufferedWriter(new FileWriter(path));

			while (i.hasNext()) {
				Unit u = (Unit) i.next();
				FlowSet in = analysis.getFlowBefore(u);
				FlowSet out = analysis.getFlowAfter(u);

				// Do something clever with the results
				//List ins = in.toList();
				List outs = out.toList();

				for (int j = 0; j < outs.size(); j++) {
					writer.write(u.getTags() + " | " + outs.get(j).toString());
					writer.newLine();
				}
			}

			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
