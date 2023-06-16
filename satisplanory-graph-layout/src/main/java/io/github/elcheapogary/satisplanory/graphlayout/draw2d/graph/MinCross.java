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
 * Sweeps up and down the ranks rearranging them so as to reduce edge crossings.
 * 
 * @author Randy Hudson
 * @since 2.1.2
 */
class MinCross extends GraphVisitor {

	static final int MAX = 45;

	private DirectedGraph g;
	private RankSorter sorter = new RankSorter();

	public MinCross() {
	}

	/**
	 * @since 3.1
	 */
	public MinCross(RankSorter sorter) {
		setRankSorter(sorter);
	}

	public void setRankSorter(RankSorter sorter) {
		this.sorter = sorter;
	}

	void solve() {
		Rank rank;
		for (int loop = 0; loop < MAX; loop++) {
			for (int row = 1; row < g.ranks.size(); row++) {
				rank = g.ranks.getRank(row);
				sorter.sortRankIncoming(g, rank, row, (double) loop / MAX);
			}
			if (loop == MAX - 1)
				continue;
			for (int row = g.ranks.size() - 2; row >= 0; row--) {
				rank = g.ranks.getRank(row);
				sorter.sortRankOutgoing(g, rank, row, (double) loop / MAX);
			}
		}
	}

	/**
	 * @see GraphVisitor#visit(DirectedGraph)
	 */
	public void visit(DirectedGraph g) {
		sorter.init(g);
		this.g = g;
		solve();
		sorter.optimize(g);
	}

}
