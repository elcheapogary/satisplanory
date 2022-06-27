/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.prodplan.lp;

import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.util.Collections;
import java.util.Map;

class Variable
        extends FractionExpression
{
    final int index;

    Variable(int index)
    {
        super(BigFraction.zero());
        this.index = index;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Variable variable = (Variable)o;
        return index == variable.index;
    }

    int getIndex()
    {
        return index;
    }

    @Override
    Map<? extends Variable, ? extends BigFraction> getVariableValues()
    {
        return Collections.singletonMap(this, BigFraction.one());
    }

    @Override
    public int hashCode()
    {
        return index;
    }

    @Override
    public String toString()
    {
        return "Variable{" +
                "index=" + index +
                '}';
    }
}
