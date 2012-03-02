package br.ufpe.cin.emergo.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.StringNameProvider;

import dk.au.cs.java.compiler.cfg.point.Point;
import dk.au.cs.java.compiler.node.Token;

/**
 * Contains some utility methods for debug.
 * 
 * @author Társis
 * 
 */
public class DebugUtil {

	private static String EMERGO_DIR = ".emergo";

	/**
	 * Defeats instantiation.
	 */
	private DebugUtil() {
	}

	/**
	 * Will try to create the a file named {@code fileName} in Emergo's default
	 * directory in $HOME/.emergo/ and write the String {@code str} to it.
	 * 
	 * Throws IllegalArgumentException if either {@code str} or {@code fileName}
	 * is null
	 * 
	 * @param str
	 *            the string to written
	 * @param fileName
	 *            the file in which the string will be written
	 * @return a File instance designated by {@code file} where the String
	 *         {@code str} was written
	 */
	public static File writeStringToFile(String str, String fileName) {
		return writeStringToFile(str, new File(System.getProperty("user.home")
				+ File.separator + EMERGO_DIR + File.separator + fileName));
	}

	/**
	 * Will try to create the a file named {@code file} and its parent
	 * directories if they do not exist and write the String {@code str} to it.
	 * 
	 * Throws IllegalArgumentException if either {@code str} or {@code file} is
	 * null
	 * 
	 * @param str
	 *            the string to be written
	 * @param file
	 *            the file in which the string will be written
	 * @return a File instance designated by {@code file} where the String
	 *         {@code was written}
	 */
	public static File writeStringToFile(String str, File file) {
		if (str == null) {
			throw new IllegalArgumentException("String cannot be null");
		}
		if (file == null) {
			throw new IllegalArgumentException("File cannot be null");
		}

		file.getParentFile().mkdirs();

		FileWriter writer;
		try {
			writer = new FileWriter(file);
			writer.write(str);
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return file;
	}

	/**
	 * Will try to create the a file named {@code fileName} in Emergo's default
	 * directory in $HOME/.emergo/ and write the String representation of
	 * {@code graph} to it.
	 * 
	 * XXX: Use a "Stringifier" for the edges/nodes
	 * 
	 * Throws IllegalArgumentException if either {@code graph} or
	 * {@code fileName} is null
	 * 
	 * @param graph
	 *            the graph to written
	 * @param fileName
	 *            the file in which the string will be written
	 * @return a File instance designated by {@code file} where the String
	 *         {@code was written}
	 */
	public static File exportToDotFile(DirectedGraph graph, String fileName) {
		if (graph == null) {
			throw new IllegalArgumentException("The graph cannot be null");
		}
		if (fileName == null) {
			throw new IllegalArgumentException("The file name cannot be null");
		}
		return exportToDotFile(graph, new File(System.getProperty("user.home")
				+ File.separator + EMERGO_DIR + File.separator + fileName));
	}

	/**
	 * Will try to write the string representation of graph by exporting it to
	 * {@code file}.
	 * 
	 * XXX: Use a "Stringifier" for the edges/nodes
	 * 
	 * Throws IllegalArgumentException if either {@code graph} or {@code file}
	 * is null
	 * 
	 * @param graph
	 *            the graph to be exported
	 * @param file
	 *            the file where graph is to be exported
	 * @return the file where the graph was written to
	 */
	public static File exportToDotFile(DirectedGraph graph, File file) {
		file.getParentFile().mkdirs();
		
		DOTExporter<?, ?> exporter = new DOTExporter<Object, Object>(
				new StringNameProvider<Object>() {

					@Override
					public String getVertexName(Object vertex) {
						if (vertex instanceof Point) {
							Point point = (Point) vertex;
							StringBuilder builder = new StringBuilder("\"");
							builder.append(vertex.toString());
							builder.append("\\n");
							builder.append(point.getVarSet());
							builder.append("\\n(");
							Token token = point.getToken();
							builder.append(token.getLine() + "," + token.getPos());
							builder.append(")\"");
							return builder.toString();
						}
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
			exporter.export(writer, graph);
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return file;
	}
}
