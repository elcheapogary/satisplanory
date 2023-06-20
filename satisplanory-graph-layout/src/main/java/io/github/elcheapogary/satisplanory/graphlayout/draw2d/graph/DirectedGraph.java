/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.github.elcheapogary.satisplanory.graphlayout.draw2d.graph;

import io.github.elcheapogary.satisplanory.graphlayout.draw2d.PositionConstants;
import io.github.elcheapogary.satisplanory.graphlayout.draw2d.geometry.Dimension;
import io.github.elcheapogary.satisplanory.graphlayout.draw2d.geometry.Insets;

/**
 * A graph consisting of nodes and directed edges. A DirectedGraph serves as the
 * input to a graph layout algorithm. The algorithm will place the graph's nodes
 * and edges according to certain goals, such as short, non-crossing edges, and
 * readability.
 *
 * @author hudsonr
 * @since 2.1.2
 */
public class DirectedGraph
{

    /**
     * All the edges in the graph.
     */
    public EdgeList edges = new EdgeList();
    /**
     * All the nodes in the graph.
     */
    public NodeList nodes = new NodeList();
    /**
     * The list of rows which makeup the final graph layout.
     */
    RankList ranks = new RankList();
    Node forestRoot;
    Insets margin = new Insets();
    int[] rankLocations;
    int[][] cellLocations;
    int tensorStrength;
    int tensorSize;
    Dimension size = new Dimension();
    private int direction = PositionConstants.SOUTH;
    /**
     * The default padding to be used for nodes which don't specify any padding.
     * Padding is the amount of empty space to be left around a node. The default
     * value is undefined.
     */
    private Insets defaultPadding = new Insets(16);

    /**
     * Returns the default padding for nodes.
     *
     * @return the default padding
     * @since 3.2
     */
    public Insets getDefaultPadding()
    {
        return defaultPadding;
    }

    /**
     * Sets the default padding for all nodes in the graph. Padding is the empty
     * space left around the <em>outside</em> of each node. The default padding is
     * used for all nodes which do not specify a specific amount of padding (i.e.,
     * their padding is <code>null</code>).
     *
     * @param insets the padding
     */
    public void setDefaultPadding(Insets insets)
    {
        defaultPadding = insets;
    }

    /**
     * Returns the direction in which the graph will be layed out.
     *
     * @return the layout direction
     * @since 3.2
     */
    public int getDirection()
    {
        return direction;
    }

    /**
     * Sets the layout direction for the graph. Edges will be layed out in the
     * specified direction (unless the graph contains cycles). Supported values are:
     * <UL>
     * <LI>{@link PositionConstants#EAST}
     * <LI>{@link PositionConstants#SOUTH}
     * </UL>
     * <p>
     * The default direction is south.
     *
     * @param direction the layout direction
     * @since 3.2
     */
    public void setDirection(int direction)
    {
        this.direction = direction;
    }

    public Dimension getLayoutSize()
    {
        return size;
    }

    /**
     * Sets the outer margin for the entire graph. The margin is the space in which
     * nodes should not be placed.
     *
     * @return the graph's margin
     * @since 3.2
     */
    public Insets getMargin()
    {
        return margin;
    }

    /**
     * Sets the graphs margin.
     *
     * @param insets the graph's margin
     * @since 3.2
     */
    public void setMargin(Insets insets)
    {
        this.margin = insets;
    }

    public Node getNode(int rank, int index)
    {
        if (ranks.size() <= rank)
            return null;
        Rank r = ranks.getRank(rank);
        if (r.size() <= index)
            return null;
        return r.getNode(index);
    }

    /**
     * Returns the effective padding for the given node. If the node has a specified
     * padding, it will be used, otherwise, the graph's defaultPadding is returned.
     * The returned value must not be modified.
     *
     * @param node the node
     * @return the effective padding for that node
     */
    public Insets getPadding(Node node)
    {
        Insets pad = node.getPadding();
        if (pad == null)
            return defaultPadding;
        return pad;
    }

    /**
     * Removes the given edge from the graph.
     *
     * @param edge the edge to be removed
     */
    public void removeEdge(Edge edge)
    {
        edges.remove(edge);
        edge.source.outgoing.remove(edge);
        edge.target.incoming.remove(edge);
        if (edge.vNodes != null)
            for (int j = 0; j < edge.vNodes.size(); j++)
                removeNode(edge.vNodes.getNode(j));
    }

    /**
     * Removes the given node from the graph. Does not remove the node's edges.
     *
     * @param node the node to remove
     */
    public void removeNode(Node node)
    {
        nodes.remove(node);
        if (ranks != null)
            ranks.getRank(node.rank).remove(node);
    }
}