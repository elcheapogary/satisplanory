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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class Constraint
{
    private final Map<? extends Variable, ? extends BigFraction> variableValues;
    private final BigFraction min;
    private final BigFraction max;

    Constraint(Map<? extends Variable, ? extends BigFraction> variableValues, BigFraction min, BigFraction max)
    {
        this.variableValues = Collections.unmodifiableMap(variableValues);
        this.min = min;
        this.max = max;
    }

    BigFraction getMax()
    {
        return max;
    }

    BigFraction getMin()
    {
        return min;
    }

    BigFraction getVariableMultiplier(Variable variable)
    {
        return variableValues.get(variable);
    }

    Collection<? extends Variable> getVariables()
    {
        return variableValues.keySet();
    }
}
