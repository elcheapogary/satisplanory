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
 * @deprecated virtual nodes of an edge should be cast to Node.
 * @author Randy Hudson
 * @since 2.1.2
 */
public class VirtualNode extends Node {

	/**
	 * The next node.
	 */
	public Node next;

	/**
	 * The previous node.
	 */
	public Node prev;

	/**
	 * Constructs a virtual node.
	 * 
	 * @deprecated This class is for internal use only.
	 * @param e the edge
	 * @param i the row
	 */
	public VirtualNode(Edge e, int i) {
		super(e);
		incoming.add(e);
		outgoing.add(e);
		width = e.width;
		height = 0;
		rank = i;
		setPadding(new Insets(0, e.padding, 0, e.padding));
	}

	/**
	 * Constructor.
	 * 
	 * @param o      object
	 * @param parent subgraph
	 */
	public VirtualNode(Object o, Subgraph parent) {
		super(o, parent);
	}

	/**
	 * Returns the index of {@link #prev}.
	 * 
	 * @return median
	 */
	public double medianIncoming() {
		return prev.index;
	}

	/**
	 * Returns the index of {@link #next}.
	 * 
	 * @return outgoing
	 */
	public double medianOutgoing() {
		return next.index;
	}

	/**
	 * For internal use only. Returns the original edge weight multiplied by the
	 * omega value for the this node and the node on the previous rank.
	 * 
	 * @return the weighted weight, or omega
	 */
	public int omega() {
		Edge e = (Edge) data;
		if (e.source.rank + 1 < rank && rank < e.target.rank)
			return 8 * e.weight;
		return 2 * e.weight;
	}

	/**
	 * @see Object#toString()
	 */
	public String toString() {
		if (data instanceof Edge)
			return "VN[" + (((Edge) data).vNodes.indexOf(this) + 1) //$NON-NLS-1$
					+ "](" + data + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		return super.toString();
	}

}
