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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

public class IntegerExpression
        extends Expression
{
    private final Map<? extends Variable, ? extends BigFraction> variableValues;

    IntegerExpression(BigFraction constantValue, Map<? extends Variable, ? extends BigFraction> variableValues)
    {
        super(constantValue);
        this.variableValues = variableValues;
    }

    public BigInteger getValue(OptimizationResult result)
    {
        return result.getValue(this).toBigIntegerExact();
    }

    @Override
    Map<? extends Variable, ? extends BigFraction> getVariableValues()
    {
        return variableValues;
    }
}
