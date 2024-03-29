/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.prodplan.graph.lib;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Node<N, E>
{
    private final Graph<N, E> graph;
    private final String name;
    private final Map<Node<N, E>, Edge<N, E>> outgoingEdges = new HashMap<>();
    private final Map<Node<N, E>, Edge<N, E>> incomingEdges = new HashMap<>();
    private final N data;

    Node(Graph<N, E> graph, String name, N data)
    {
        this.graph = graph;
        this.name = name;
        this.data = data;
    }

    public N getData()
    {
        return data;
    }

    public Edge<N, E> getEdgeTo(Node<N, E> target)
    {
        return outgoingEdges.get(target);
    }

    public Map<Node<N, E>, Edge<N, E>> getIncomingEdges()
    {
        return Collections.unmodifiableMap(incomingEdges);
    }

    /**
     * Returns the name of this node. The name is not necessarily unique in the graph.
     *
     * @return The name of this node.
     */
    public String getName()
    {
        return name;
    }

    public Map<Node<N, E>, Edge<N, E>> getOutgoingEdges()
    {
        return Collections.unmodifiableMap(outgoingEdges);
    }

    public Edge<N, E> link(Node<N, E> targetNode, E edgeData)
    {
        if (targetNode.graph != this.graph){
            throw new IllegalArgumentException("Node is for different graph");
        }

        if (outgoingEdges.containsKey(targetNode)){
            throw new IllegalArgumentException("Duplicate edge from node [" + getName() + "] to node [" + targetNode.getName() + "]");
        }

        Edge<N, E> edge = new Edge<>(this, targetNode, edgeData);

        outgoingEdges.put(targetNode, edge);
        targetNode.incomingEdges.put(this, edge);

        return edge;
    }
}
