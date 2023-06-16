/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.lp;

import java.util.Comparator;

abstract class Variable
{
    static final Comparator<Variable> COMPARATOR = Comparator.comparingInt(o -> o.id);
    final int id;

    public Variable(int id)
    {
        this.id = id;
    }

    public abstract String getDebugName();

    public int getId()
    {
        return id;
    }
}
