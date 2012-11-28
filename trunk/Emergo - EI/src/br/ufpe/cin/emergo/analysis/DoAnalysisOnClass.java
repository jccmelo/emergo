package br.ufpe.cin.emergo.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import br.ufpe.cin.emergo.analysis.reachingdefs.LazyLiftedReachingDefinitions;
import br.ufpe.cin.emergo.analysis.reachingdefs.LiftedReachingDefinitions;
import br.ufpe.cin.emergo.analysis.reachingdefs.SimpleReachingDefinitions;
import br.ufpe.cin.emergo.analysis.reachingdefs.UnliftedReachingDefinitions;
import br.ufpe.cin.emergo.instrument.EagerConfigTag;
import br.ufpe.cin.emergo.instrument.FeatureInstrumentor;
import br.ufpe.cin.emergo.instrument.IConfigRep;
import br.ufpe.cin.emergo.instrument.ILazyConfigRep;
import br.ufpe.cin.emergo.instrument.LazyConfigTag;
import br.ufpe.cin.emergo.preprocessor.ContextManager;
import br.ufpe.cin.emergo.preprocessor.Preprocessor;
import br.ufpe.cin.emergo.compiler.Compiler;

public class DoAnalysisOnClass {

	final static Logger logger = Logger.getLogger(DoAnalysisOnClass.class
			.getName());

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		/*
		 * First of all, (1) the preprocessing is performed.
		 */
		ContextManager manager = ContextManager.getContext();
		manager.setSrcfile("Out.groovy"); // input class
		manager.setDestfile("Testclass.groovy"); // output class

		Preprocessor pp = new Preprocessor();

		String defs = "";// "A , SOMA";
		pp.setDefs(defs);

		// TODO create the graphic interface
		pp.execute();
		logger.info("Preprocessing done successfully!");

		/*
		 * (2) Compiles the output class
		 */
		try {
			new Compiler(manager.getDestfile()).doCompilation();
		} catch (IOException e) {
			System.out.println("Unexpected error while compiling");
			e.printStackTrace();
		}
		logger.info("Compiling done successfully!");
		
		/*
		 * (3) Loads and configures the output class with the Soot Framework
		 */
		// Set up the class we’re working with
		SootManager.configure("../Preprocessor4SPL/bin/");
		SootClass c = SootManager.loadAndSupport(Compiler.retrieveClassName(manager.getDestfile()));

		// Retrieve the method and its body
		SootMethod m = c.getMethodByName("main");
		Body b = m.retrieveActiveBody();

		logger.info("Configuration done successfully!");

		/*
		 * (4) Instruments bytecode of the output class
		 */
		new FeatureInstrumentor().transform(b);

		logger.info("Instrumentation done successfully!");

		/*
		 * (5) Builds the CFG, runs the analyses and interprets the results
		 */

		// --------------------------------------------------------------------
		// Analysis: SimpleReachingDefinitions
		// --------------------------------------------------------------------
		BriefUnitGraph briefUnitGraph = new BriefUnitGraph(b);

		SimpleReachingDefinitions reachingDefinitions = new SimpleReachingDefinitions(
				briefUnitGraph);

		runAnalysis(reachingDefinitions, briefUnitGraph, "SimpleReachingDefinitions");

		// --------------------------------------------------------------------
		// Analysis: UnliftedReachingDefinitions
		// --------------------------------------------------------------------

		EagerConfigTag configTag = (EagerConfigTag) b
				.getTag(EagerConfigTag.TAG_NAME);

		if (configTag == null) {
			throw new IllegalStateException(
					"No EagerConfigTag found for body of method "
							+ b.getMethod());
		}

		Set<IConfigRep> configReps = configTag.getConfigReps();
		if (configReps.size() < 1) {
			return;
		}

		UnitGraph bodyGraph = new BriefUnitGraph(b);

		for (IConfigRep config : configReps) {
			UnliftedReachingDefinitions unliftedReachingDefinitions = new UnliftedReachingDefinitions(
					bodyGraph, config);
			
			runAnalysis(unliftedReachingDefinitions, bodyGraph, "UnliftedReachingDefinitions-"+config);
		}
		
		// --------------------------------------------------------------------
		// Analysis: LiftedReachingDefinitions
		// --------------------------------------------------------------------
		
		LiftedReachingDefinitions liftedReachingDefinitions = new LiftedReachingDefinitions(bodyGraph, configReps);
		
		runAnalysis(liftedReachingDefinitions, bodyGraph, "LiftedReachingDefinitions");
		
		// --------------------------------------------------------------------
		// Analysis: LazyLiftedReachingDefinitions
		// --------------------------------------------------------------------
		
		LazyConfigTag configLazyTag = (LazyConfigTag) b.getTag(LazyConfigTag.TAG_NAME);
		if (configLazyTag == null) {
			throw new IllegalStateException("No LazyConfigTag found for body of method " + b.getMethod());
		}
		
		ILazyConfigRep lazyConfig = configLazyTag.getLazyConfig();
		if (lazyConfig.size() < 1) {
			return;
		}
		
		UnitGraph bodyLazyGraph = new BriefUnitGraph(b);
		
		LazyLiftedReachingDefinitions lazyLiftedReachingDefinitions = new LazyLiftedReachingDefinitions(bodyLazyGraph, lazyConfig);
		
		runAnalysis(lazyLiftedReachingDefinitions, bodyLazyGraph, "LazyLiftedReachingDefinitions");
		
		// --------------------------------------------------------------------
		// Compares the results: LatticeEquivalenceTester
		// --------------------------------------------------------------------
		
//		new LatticeEquivalenceTester().transform(b);
		
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

		String path = settingDirectory() + fileName + ".txt";

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(path));
			
			// Iterate over the results
			Iterator i = graph.iterator();
			
			while (i.hasNext()) {
				Unit u = (Unit) i.next();
//				FlowSet in = analysis.getFlowBefore(u);
				FlowSet out = analysis.getFlowAfter(u);

				// Do something clever with the results

				System.out.println(u + " | " + out);
				writer.write("Unit: " + u + " => " + out);
				writer.newLine();
			}

			writer.flush();
			writer.close();
			
			logger.info(fileName+" executed successfully!");

		} catch (IOException e) {
			logger.info("Unexpected error while executing the analysis "+fileName);
			e.printStackTrace();
		}
	}
	
	private static String settingDirectory(){
		String className = Compiler.retrieveClassName(ContextManager.getContext().getDestfile());
//		String path = "../Preprocessor4SPL/results/"+ className +"/";
		String path = "../Preprocessor4SPL/results/"+ className + new Date().getDate() +"/";

		File dir = new File(path);
		dir.mkdirs();
		
//		if (!dir.exists()) {
//			path = "../Preprocessor4SPL/results/"+ className + new Date().getDate() +"/";
//			
//			dir = new File(path);
//			dir.mkdirs();
//		}
		
		return path;
	}
}
