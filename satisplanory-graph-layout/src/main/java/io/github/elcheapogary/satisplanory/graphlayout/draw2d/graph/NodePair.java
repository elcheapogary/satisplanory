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
 * @author hudsonr
 * @since 2.1
 */
class NodePair {

	public Node n1;
	public Node n2;

	public NodePair() {
	}

	public NodePair(Node n1, Node n2) {
		this.n1 = n1;
		this.n2 = n2;
	}

	public boolean equals(Object obj) {
		if (obj instanceof NodePair) {
			NodePair np = (NodePair) obj;
			return np.n1 == n1 && np.n2 == n2;
		}
		return false;
	}

	public int hashCode() {
		return n1.hashCode() ^ n2.hashCode();
	}

	/**
	 * @see Object#toString()
	 */
	public String toString() {
		return "[" + n1 + ", " + n2 + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
