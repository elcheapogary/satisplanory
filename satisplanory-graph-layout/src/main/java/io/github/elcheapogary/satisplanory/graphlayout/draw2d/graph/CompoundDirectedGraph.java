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
 * A <code>DirectedGraph</code> whose Nodes may be compound {@link Subgraph}s,
 * which may contain other nodes. Any node in the graph may be parented by one
 * subgraph. Since subgraphs are nodes, the source or target end of an
 * {@link Edge} may be a subgraph. For additional restrictions, refer to the
 * JavaDoc for the layout algorithm being used.
 * <P>
 * A CompoundDirectedGraph is passed to a graph layout, which will position all
 * of the nodes, subgraphs, and edges in that graph. This class serves as the
 * data structure for a layout algorithm.
 * 
 * @author Randy Hudson
 * @since 2.1.2
 */
public class CompoundDirectedGraph extends DirectedGraph {

	/**
	 * For internal use only.
	 */
	public NodeList subgraphs = new NodeList();

	/**
	 * For internal use only.
	 */
	public EdgeList containment = new EdgeList();

}
