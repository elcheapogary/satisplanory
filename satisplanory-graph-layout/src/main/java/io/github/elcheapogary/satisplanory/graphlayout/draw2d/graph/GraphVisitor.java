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
 * Performs some action on a Graph.
 * 
 * @author Randy Hudson
 * @since 2.1.2
 */
abstract class GraphVisitor {

	/**
	 * Act on the given directed graph.
	 * 
	 * @param g the graph
	 */
	void visit(DirectedGraph g) {
	}

	/**
	 * Called in reverse order of visit.
	 * 
	 * @since 3.1
	 * @param g the graph to act upon
	 */
	void revisit(DirectedGraph g) {
	}

}
