/*
 * Copyright (c) 2022 elcheapogary
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
import java.util.Map;
import java.util.TreeMap;

public class Graph<N, E>
{
    private final Map<String, Node<N, E>> nodeMap = new TreeMap<>();

    public Node<N, E> createNode(String name, N data)
    {
        if (nodeMap.containsKey(name)){
            throw new IllegalArgumentException("Duplicate node name: " + name);
        }

        Node<N, E> node = new Node<>(this, name, data);

        nodeMap.put(name, node);

        return node;
    }

    public Node<N, E> getNode(String name)
    {
        return nodeMap.get(name);
    }

    public Collection<? extends Node<N, E>> getNodes()
    {
        return Collections.unmodifiableCollection(nodeMap.values());
    }
}
