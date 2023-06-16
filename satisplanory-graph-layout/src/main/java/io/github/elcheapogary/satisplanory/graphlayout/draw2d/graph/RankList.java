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

import java.util.ArrayList;

/**
 * For internal use only. A list of lists.
 * 
 * @author hudsonr
 * @since 2.1.2
 */
public final class RankList {

	ArrayList ranks = new ArrayList();

	/**
	 * Returns the specified rank.
	 * 
	 * @param rank the row
	 * @return the rank
	 */
	public Rank getRank(int rank) {
		while (ranks.size() <= rank)
			ranks.add(new Rank());
		return (Rank) ranks.get(rank);
	}

	/**
	 * Returns the total number or ranks.
	 * 
	 * @return the total number of ranks
	 */
	public int size() {
		return ranks.size();
	}

}
