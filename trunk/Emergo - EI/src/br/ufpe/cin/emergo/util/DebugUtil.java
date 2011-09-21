package br.ufpe.cin.emergo.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.StringNameProvider;

import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.graph.DependencyNodeWrapper;
import br.ufpe.cin.emergo.graph.ValueContainerEdge;
import dk.au.cs.java.compiler.cfg.point.Point;

/**
 * Contains some utility methods for debug.
 * 
 * @author Társis
 * 
 */
public class DebugUtil {

	/**
	 * Defeats instantiation.
	 */
	private DebugUtil() {
	}

	public static File exportDot(DirectedGraph filteredDependencyGraph, File file) {
		if (file == null) {
			file = new File(System.getProperty("user.home") + File.separator + "unnamed.dot");
		}

		DOTExporter<?, ?> exporter = new DOTExporter<Object, Object>(new StringNameProvider<Object>() {

			@Override
			public String getVertexName(Object vertex) {
				return "\"" + vertex.toString() + "\"";
			}

		}, null, new EdgeNameProvider<Object>() {

			@Override
			public String getEdgeName(Object edge) {
				return edge.toString();
			}

		});

		FileWriter writer;
		try {
			writer = new FileWriter(file);
			exporter.export(writer, filteredDependencyGraph);
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return file;
	}
}
