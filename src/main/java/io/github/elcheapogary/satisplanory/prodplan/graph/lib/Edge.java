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

public class Edge<N, E>
{
    private final Node<N, E> source;
    private final Node<N, E> destination;
    private final E data;

    Edge(Node<N, E> source, Node<N, E> destination, E data)
    {
        this.source = source;
        this.destination = destination;
        this.data = data;
    }

    public E getData()
    {
        return data;
    }

    public Node<N, E> getDestination()
    {
        return destination;
    }

    public Node<N, E> getSource()
    {
        return source;
    }
}
