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
 * For INTERNAL use only.
 * 
 * @author hudsonr
 * @since 2.1.2
 */
class SubgraphBoundary extends Node {

	/**
	 * constant indicating TOP.
	 */
	public static final int TOP = 0;

	/**
	 * constant indicating LEFT.
	 */
	public static final int LEFT = 1;

	/**
	 * constant indicating BOTTOM.
	 */
	public static final int BOTTOM = 2;

	/**
	 * constant indicating RIGHT.
	 */
	public static final int RIGHT = 3;

	/**
	 * Constructs a new boundary.
	 * 
	 * @param s    the subgraph
	 * @param p    the padding
	 * @param side which side
	 */
	public SubgraphBoundary(Subgraph s, Insets p, int side) {
		super(null, s);
		this.width = s.width;
		this.height = s.height;
		this.padding = new Insets();
		switch (side) {
		case LEFT:
			width = s.insets.left;
			y = s.y;
			padding.left = p.left;
			padding.right = s.innerPadding.left;
			padding.top = padding.bottom = 0;
			setParent(s.getParent());
			data = "left(" + s + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			break;
		case RIGHT:
			width = s.insets.right;
			y = s.y;
			padding.right = p.right;
			padding.left = s.innerPadding.right;
			padding.top = padding.bottom = 0;
			setParent(s.getParent());
			data = "right(" + s + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			break;
		case TOP:
			height = s.insets.top;
			// $TODO width of head/tail should be 0
			width = 5;
			padding.top = p.top;
			padding.bottom = s.innerPadding.top;
			padding.left = padding.right = 0;
			data = "top(" + s + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			break;
		case BOTTOM:
			height = s.insets.bottom;
			// $TODO width of head/tail should be 0
			width = 5;
			padding.top = s.innerPadding.bottom;
			padding.bottom = p.bottom;
			padding.left = padding.right = 0;
			data = "bottom(" + s + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			break;
		}
	}

}
