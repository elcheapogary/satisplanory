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
import java.util.Map;

public class IntegerExpression
        extends Expression
{
    IntegerExpression(Map<DecisionVariable, BigFraction> coefficients, BigFraction constantValue)
    {
        super(coefficients, constantValue);
    }

    private IntegerExpression(Expression e)
    {
        this(e.getCoefficients(), e.getConstantValue());
    }

    @Override
    public IntegerExpression add(long addend)
    {
        return new IntegerExpression(super.add(addend));
    }

    @Override
    public IntegerExpression add(BigInteger addend)
    {
        return new IntegerExpression(super.add(addend));
    }

    public IntegerExpression add(IntegerExpression addend)
    {
        return new IntegerExpression(super.add(addend));
    }

    @Override
    public IntegerExpression multiply(long multiplicand)
    {
        return new IntegerExpression(super.multiply(multiplicand));
    }

    @Override
    public IntegerExpression multiply(BigInteger multiplicand)
    {
        return new IntegerExpression(super.multiply(multiplicand));
    }

    @Override
    public IntegerExpression subtract(long subtrahend)
    {
        return new IntegerExpression(super.subtract(subtrahend));
    }

    @Override
    public IntegerExpression subtract(BigInteger subtrahend)
    {
        return new IntegerExpression(super.subtract(subtrahend));
    }

    public IntegerExpression subtract(IntegerExpression subtrahend)
    {
        return new IntegerExpression(super.subtract(subtrahend));
    }
}
