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
 * calculates the height and y-coordinates for nodes and subgraphs in a compound
 * directed graph.
 * 
 * @author Randy Hudson
 * @since 2.1.2
 */
class CompoundVerticalPlacement extends VerticalPlacement {

	/**
	 * @see GraphVisitor#visit(DirectedGraph) Extended to set subgraph values.
	 */
	void visit(DirectedGraph dg) {
		CompoundDirectedGraph g = (CompoundDirectedGraph) dg;
		super.visit(g);
		for (int i = 0; i < g.subgraphs.size(); i++) {
			Subgraph s = (Subgraph) g.subgraphs.get(i);
			s.y = s.head.y;
			s.height = s.tail.height + s.tail.y - s.y;
		}
	}

}
