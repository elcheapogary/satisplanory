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

public class OptimizationResult
{
    private final BigFraction objectiveFunctionValue;
    private final BigFraction[] variableValues;

    OptimizationResult(BigFraction objectiveFunctionValue, BigFraction[] variableValues)
    {
        this.objectiveFunctionValue = objectiveFunctionValue;
        this.variableValues = variableValues;
    }

    public BigFraction getObjectiveFunctionValue()
    {
        return objectiveFunctionValue;
    }

    BigFraction getValue(Expression expression)
    {
        BigFraction result = BigFraction.valueOf(expression.getConstantValue());

        for (var entry : expression.getVariableValues().entrySet()){
            Variable v = entry.getKey();
            BigDecimal m = entry.getValue();

            result = result.add(variableValues[v.index].multiply(BigFraction.valueOf(m)));
        }

        return result;
    }

    OptimizationResult negateObjectiveFunctionValue()
    {
        return new OptimizationResult(objectiveFunctionValue.negate(), variableValues);
    }
}
