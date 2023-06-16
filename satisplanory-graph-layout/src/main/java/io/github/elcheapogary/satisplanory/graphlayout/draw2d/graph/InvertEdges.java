/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package io.github.elcheapogary.satisplanory.graphlayout.draw2d.graph;

/**
 * Inverts any edges which are marked as backwards or "feedback" edges.
 * 
 * @author Daniel Lee
 * @since 2.1.2
 */
class InvertEdges extends GraphVisitor {

	/**
	 * 
	 * @see GraphVisitor#visit(DirectedGraph)
	 */
	public void visit(DirectedGraph g) {
		for (int i = 0; i < g.edges.size(); i++) {
			Edge e = g.edges.getEdge(i);
			if (e.isFeedback)
				e.invert();
		}
	}

}
