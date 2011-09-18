package br.ufpe.cin.emergo.util;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import dk.au.cs.java.compiler.cfg.ControlFlowGraph;
import dk.au.cs.java.compiler.cfg.edge.Edge;
import dk.au.cs.java.compiler.cfg.point.Point;

public class CFGIterator implements Iterable<Point> {

	private LinkedHashSet<Point> points;

	public Iterator<Point> iterator() {
		return points.iterator();
	}

	public CFGIterator(ControlFlowGraph cfg) {
		Point entryPoint = cfg.getEntryPoint();
		LinkedHashSet<Point> visitedPoints = new LinkedHashSet<Point>();
		LinkedList<Point> pendingPoints = new LinkedList<Point>();
		pendingPoints.add(entryPoint);
		visitedPoints.add(entryPoint);

		while (!pendingPoints.isEmpty()) {
			Point poppedPoint = pendingPoints.removeFirst();
			Set<? extends Edge> outgoingEdges = poppedPoint.getOutgoingEdges();
			for (Edge edge : outgoingEdges) {
				Point target = edge.getTarget();
				if (visitedPoints.add(target)) {
					pendingPoints.add(target);
				}
			}
		}

		this.points = visitedPoints;
	}
}
