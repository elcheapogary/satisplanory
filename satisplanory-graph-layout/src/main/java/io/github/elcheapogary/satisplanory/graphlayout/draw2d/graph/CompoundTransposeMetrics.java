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

import io.github.elcheapogary.satisplanory.graphlayout.draw2d.PositionConstants;

/**
 * Performs transposing of subgraphics in a compound directed graph.
 * 
 * @since 3.7.1
 */
class CompoundTransposeMetrics extends TransposeMetrics {

	public void visit(DirectedGraph g) {
		if (g.getDirection() == PositionConstants.SOUTH)
			return;
		super.visit(g);
		int temp;
		CompoundDirectedGraph cg = (CompoundDirectedGraph) g;
		for (int i = 0; i < cg.subgraphs.size(); i++) {
			Node node = cg.subgraphs.getNode(i);
			temp = node.width;
			node.width = node.height;
			node.height = temp;
			if (node.getPadding() != null)
				node.setPadding(t.t(node.getPadding()));
		}
	}

	public void revisit(DirectedGraph g) {
		if (g.getDirection() == PositionConstants.SOUTH)
			return;
		super.revisit(g);
		int temp;
		CompoundDirectedGraph cg = (CompoundDirectedGraph) g;
		for (int i = 0; i < cg.subgraphs.size(); i++) {
			Node node = (Node) cg.subgraphs.get(i);
			temp = node.width;
			node.width = node.height;
			node.height = temp;
			temp = node.y;
			node.y = node.x;
			node.x = temp;
			if (node.getPadding() != null)
				node.setPadding(t.t(node.getPadding()));
		}
	}

}