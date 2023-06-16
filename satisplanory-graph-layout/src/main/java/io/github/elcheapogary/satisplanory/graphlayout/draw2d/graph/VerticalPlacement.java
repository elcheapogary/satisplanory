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

import io.github.elcheapogary.satisplanory.graphlayout.draw2d.geometry.Insets;

/**
 * Assigns the Y and Height values to the nodes in the graph. All nodes in the
 * same row are given the same height.
 * 
 * @author Randy Hudson
 * @since 2.1.2
 */
class VerticalPlacement extends GraphVisitor {

	void visit(DirectedGraph g) {
		Insets pad;
		int currentY = g.getMargin().top;
		int row, rowHeight;
		g.rankLocations = new int[g.ranks.size() + 1];
		for (row = 0; row < g.ranks.size(); row++) {
			g.rankLocations[row] = currentY;
			Rank rank = g.ranks.getRank(row);
			rowHeight = 0;
			rank.topPadding = rank.bottomPadding = 0;
			for (int n = 0; n < rank.size(); n++) {
				Node node = rank.getNode(n);
				pad = g.getPadding(node);
				rowHeight = Math.max(node.height, rowHeight);
				rank.topPadding = Math.max(pad.top, rank.topPadding);
				rank.bottomPadding = Math.max(pad.bottom, rank.bottomPadding);
			}
			currentY += rank.topPadding;
			rank.setDimensions(currentY, rowHeight);
			currentY += rank.height + rank.bottomPadding;
		}
		g.rankLocations[row] = currentY;
		g.size.height = currentY;
	}

}
