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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

class Variable
        extends FractionExpression
{
    final int index;

    Variable(int index)
    {
        super(BigDecimal.ZERO);
        this.index = index;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Variable variable = (Variable) o;
        return index == variable.index;
    }

    int getIndex()
    {
        return index;
    }

    @Override
    Map<? extends Variable, ? extends BigDecimal> getVariableValues()
    {
        return Collections.singletonMap(this, BigDecimal.ONE);
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
