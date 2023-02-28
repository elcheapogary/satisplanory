/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.prodplan.lp;

import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OptimizationResult
{
    private final List<BigFraction> objectiveValues;
    private final Map<DecisionVariable, BigFraction> variableValues;

    OptimizationResult(List<BigFraction> objectiveValues, Map<DecisionVariable, BigFraction> variableValues)
    {
        this.objectiveValues = Collections.unmodifiableList(objectiveValues);
        this.variableValues = variableValues;
    }

    public boolean getBooleanValue(BinaryExpression expression)
    {
        return getIntegerValue(expression).signum() > 0;
    }

    public BigFraction getFractionValue(Expression expression)
    {
        try (var stream = expression.getCoefficients().entrySet().parallelStream()) {
            return stream.map(entry -> variableValues.get(entry.getKey()).multiply(entry.getValue()))
                    .reduce(expression.getConstantValue(), BigFraction::add);
        }
    }

    public BigInteger getIntegerValue(IntegerExpression expression)
    {
        return getFractionValue(expression).toBigIntegerExact();
    }

    public List<BigFraction> getObjectiveValues()
    {
        return objectiveValues;
    }
}
