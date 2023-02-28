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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

public class Graph<N, E>
{
    private final Collection<Node<N, E>> nodes = new LinkedList<>();

    /**
     * Creates a new node in this graph.
     *
     * @param name The name of the new node. Node names do not need to be unique in the graph.
     * @param data The data stored in the node.
     * @return A new node in this graph.
     */
    public Node<N, E> createNode(String name, N data)
    {
        Node<N, E> node = new Node<>(this, name, data);

        nodes.add(node);

        return node;
    }

    public Collection<? extends Node<N, E>> getNodes()
    {
        return Collections.unmodifiableCollection(nodes);
    }
}
