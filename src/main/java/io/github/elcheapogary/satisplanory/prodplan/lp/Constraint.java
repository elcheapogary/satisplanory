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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class Constraint
{
    private final Map<? extends Variable, ? extends BigDecimal> variableValues;
    private final BigDecimal min;
    private final BigDecimal max;

    Constraint(Map<? extends Variable, ? extends BigDecimal> variableValues, BigDecimal min, BigDecimal max)
    {
        this.variableValues = Collections.unmodifiableMap(variableValues);
        this.min = min;
        this.max = max;
    }

    BigDecimal getMax()
    {
        return max;
    }

    BigDecimal getMin()
    {
        return min;
    }

    BigDecimal getVariableMultiplier(Variable variable)
    {
        return variableValues.get(variable);
    }

    Collection<? extends Variable> getVariables()
    {
        return variableValues.keySet();
    }
}
