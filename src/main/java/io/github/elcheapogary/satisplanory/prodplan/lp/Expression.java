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
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public abstract class Expression
{
    private final BigDecimal constantValue;

    Expression(BigDecimal constantValue)
    {
        this.constantValue = constantValue;
    }

    public static FractionExpression constant(BigDecimal value)
    {
        return new ConcreteFractionExpression(value, Collections.emptyMap());
    }

    public static FractionExpression constant(long value)
    {
        return constant(BigDecimal.valueOf(value));
    }

    public static FractionExpression constant(double value)
    {
        return constant(BigDecimal.valueOf(value));
    }

    public static FractionExpression zero()
    {
        return FractionExpression.ZERO;
    }

    public FractionExpression add(BigDecimal constantValue)
    {
        return new ConcreteFractionExpression(this.constantValue.add(constantValue), getVariableValues());
    }

    public FractionExpression add(long constantValue)
    {
        return add(BigDecimal.valueOf(constantValue));
    }

    public FractionExpression add(double constantValue)
    {
        return add(BigDecimal.valueOf(constantValue));
    }

    public FractionExpression add(Expression expression)
    {
        return add(1, expression);
    }

    public FractionExpression add(BigDecimal multiplier, Expression expression)
    {
        Map<Variable, BigDecimal> m = new TreeMap<>(Comparator.comparing(Variable::getIndex));

        m.putAll(getVariableValues());

        for (var entry : expression.getVariableValues().entrySet()){
            m.compute(entry.getKey(), (variable2, existingMultiplier) ->
                    Objects.requireNonNullElse(existingMultiplier, BigDecimal.ZERO)
                            .add(multiplier.multiply(entry.getValue()))
            );
        }

        return new ConcreteFractionExpression(constantValue.add(multiplier.multiply(expression.constantValue)), m);
    }

    public FractionExpression add(long multiplier, Expression expression)
    {
        return add(BigDecimal.valueOf(multiplier), expression);
    }

    public FractionExpression add(double multiplier, Expression expression)
    {
        return add(BigDecimal.valueOf(multiplier), expression);
    }

    public Constraint eq(BigDecimal amount)
    {
        return new Constraint(getVariableValues(), amount.subtract(constantValue), amount.subtract(constantValue));
    }

    public Constraint eq(long amount)
    {
        return eq(BigDecimal.valueOf(amount));
    }

    public Constraint eq(double amount)
    {
        return eq(BigDecimal.valueOf(amount));
    }

    public Constraint eq(Expression expression)
    {
        return this.subtract(expression).eq(0);
    }

    BigDecimal getConstantValue()
    {
        return constantValue;
    }

    abstract Map<? extends Variable, ? extends BigDecimal> getVariableValues();

    public Constraint gte(Expression expression)
    {
        return this.subtract(expression).gte(0);
    }

    public Constraint gte(BigDecimal amount)
    {
        return new Constraint(getVariableValues(), amount.subtract(constantValue), null);
    }

    public Constraint gte(long amount)
    {
        return gte(BigDecimal.valueOf(amount));
    }

    public Constraint gte(double amount)
    {
        return gte(BigDecimal.valueOf(amount));
    }

    public Constraint lte(BigDecimal amount)
    {
        return new Constraint(getVariableValues(), null, amount.subtract(constantValue));
    }

    public Constraint lte(long amount)
    {
        return lte(BigDecimal.valueOf(amount));
    }

    public Constraint lte(double amount)
    {
        return lte(BigDecimal.valueOf(amount));
    }

    public Constraint lte(Expression expression)
    {
        return this.subtract(expression).lte(0);
    }

    public FractionExpression multiply(long value)
    {
        return multiply(BigDecimal.valueOf(value));
    }

    public FractionExpression multiply(double value)
    {
        return multiply(BigDecimal.valueOf(value));
    }

    public FractionExpression multiply(BigDecimal value)
    {
        Map<Variable, BigDecimal> m = new TreeMap<>(Comparator.comparing(Variable::getIndex));

        for (var entry : getVariableValues().entrySet()){
            m.put(entry.getKey(), entry.getValue().multiply(value));
        }

        return new ConcreteFractionExpression(constantValue.multiply(value), m);
    }

    public FractionExpression negate()
    {
        Map<Variable, BigDecimal> m = new TreeMap<>(Comparator.comparing(Variable::getIndex));

        for (var entry : getVariableValues().entrySet()){
            m.put(entry.getKey(), entry.getValue().negate());
        }

        return new ConcreteFractionExpression(constantValue.negate(), m);
    }

    public Constraint nonNegative()
    {
        return gte(0);
    }

    public FractionExpression subtract(Expression expression)
    {
        return subtract(1, expression);
    }

    public FractionExpression subtract(BigDecimal multiplier, Expression expression)
    {
        return add(multiplier.negate(), expression);
    }

    public FractionExpression subtract(long multiplier, Expression expression)
    {
        return subtract(BigDecimal.valueOf(multiplier), expression);
    }

    public FractionExpression subtract(double multiplier, Expression expression)
    {
        return subtract(BigDecimal.valueOf(multiplier), expression);
    }

    public FractionExpression subtract(BigDecimal constantValue)
    {
        return add(constantValue.negate());
    }

    public FractionExpression subtract(long constantValue)
    {
        return subtract(BigDecimal.valueOf(constantValue));
    }

    public FractionExpression subtract(double constantValue)
    {
        return subtract(BigDecimal.valueOf(constantValue));
    }

    @Override
    public String toString()
    {
        return "Expression{" +
                "constantValue=" + constantValue + ',' +
                "variableValues=" + getVariableValues() +
                '}';
    }

    private static class ConcreteFractionExpression
            extends FractionExpression
    {
        private final Map<? extends Variable, ? extends BigDecimal> variableValues;

        public ConcreteFractionExpression(BigDecimal constantValue, Map<? extends Variable, ? extends BigDecimal> variableValues)
        {
            super(constantValue);
            this.variableValues = variableValues;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConcreteFractionExpression that = (ConcreteFractionExpression) o;
            return variableValues.equals(that.variableValues);
        }

        @Override
        Map<? extends Variable, ? extends BigDecimal> getVariableValues()
        {
            return variableValues;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(variableValues);
        }
    }
}
