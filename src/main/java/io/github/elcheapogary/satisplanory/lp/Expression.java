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

import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Expression
{
    private final Map<DecisionVariable, BigFraction> coefficients;
    private final BigFraction constantValue;

    Expression(Map<DecisionVariable, BigFraction> coefficients, BigFraction constantValue)
    {
        this.coefficients = coefficients;
        this.constantValue = constantValue;
    }

    public static BinaryExpression zero()
    {
        return BinaryExpression.ZERO;
    }

    public Expression add(Expression addend)
    {
        Set<DecisionVariable> variables = new TreeSet<>(Variable.COMPARATOR);

        variables.addAll(coefficients.keySet());
        variables.addAll(addend.coefficients.keySet());

        Map<DecisionVariable, BigFraction> newCoefficients = new TreeMap<>(Variable.COMPARATOR);

        for (DecisionVariable v : variables){
            BigFraction coefficient = Objects.requireNonNullElse(coefficients.get(v), BigFraction.zero())
                    .add(Objects.requireNonNullElse(addend.coefficients.get(v), BigFraction.zero()));

            if (coefficient.signum() != 0){
                newCoefficients.put(v, coefficient);
            }
        }

        return new Expression(newCoefficients, constantValue.add(addend.constantValue));
    }

    public Expression add(BigFraction addend)
    {
        return new Expression(coefficients, constantValue.add(addend));
    }

    public Expression add(BigDecimal addend)
    {
        return add(BigFraction.valueOf(addend));
    }

    public Expression add(BigInteger addend)
    {
        return add(BigFraction.valueOf(addend));
    }

    public Expression add(long addend)
    {
        return add(BigFraction.valueOf(addend));
    }

    public Expression add(double addend)
    {
        return add(BigDecimal.valueOf(addend));
    }

    void appendToStringBuilder(StringBuilder sb)
    {
        boolean first = true;

        for (var entry : coefficients.entrySet()){
            DecisionVariable v = entry.getKey();
            BigFraction c = entry.getValue();

            if (c.signum() == 0){
                continue;
            }

            if (first){
                first = false;
                if (c.equals(BigFraction.negativeOne())){
                    sb.append("-");
                }else if (!c.equals(BigFraction.one())){
                    sb.append(c);
                }
            }else{
                if (c.signum() < 0){
                    sb.append(" - ");
                    if (!c.equals(BigFraction.negativeOne())){
                        sb.append(c.abs());
                    }
                }else{
                    sb.append(" + ");
                    if (!c.equals(BigFraction.one())){
                        sb.append(c);
                    }
                }
            }

            sb.append("${");
            sb.append(v.getName());
            sb.append("}");
        }

        if (constantValue.signum() != 0){
            if (first){
                sb.append(constantValue);
            }else if (constantValue.signum() < 0){
                sb.append(" - ");
                sb.append(constantValue.abs());
            }else{
                sb.append(" + ");
                sb.append(constantValue);
            }
        }
    }

    public Expression divide(BigFraction divisor)
    {
        if (divisor.signum() == 0){
            throw new ArithmeticException();
        }else if (divisor.compareTo(BigFraction.one()) == 0){
            return new Expression(coefficients, constantValue);
        }

        Map<DecisionVariable, BigFraction> newCoefficients = new TreeMap<>(Variable.COMPARATOR);

        for (var entry : coefficients.entrySet()){
            newCoefficients.put(entry.getKey(), entry.getValue().divide(divisor));
        }

        return new Expression(newCoefficients, constantValue.divide(divisor));
    }

    public Expression divide(BigDecimal divisor)
    {
        return divide(BigFraction.valueOf(divisor));
    }

    public Expression divide(BigInteger divisor)
    {
        return divide(BigFraction.valueOf(divisor));
    }

    public Expression divide(long divisor)
    {
        return divide(BigFraction.valueOf(divisor));
    }

    public Expression divide(double divisor)
    {
        return divide(BigDecimal.valueOf(divisor));
    }

    public Constraint eq(Expression other)
    {
        return new Constraint(subtract(other), Constraint.Comparison.EQ);
    }

    public Constraint eq(BigFraction value)
    {
        return new Constraint(subtract(value), Constraint.Comparison.EQ);
    }

    public Constraint eq(BigDecimal value)
    {
        return new Constraint(subtract(value), Constraint.Comparison.EQ);
    }

    public Constraint eq(BigInteger value)
    {
        return new Constraint(subtract(value), Constraint.Comparison.EQ);
    }

    public Constraint eq(long value)
    {
        return new Constraint(subtract(value), Constraint.Comparison.EQ);
    }

    public Constraint eq(double value)
    {
        return new Constraint(subtract(value), Constraint.Comparison.EQ);
    }

    Map<DecisionVariable, BigFraction> getCoefficients()
    {
        return coefficients;
    }

    BigFraction getConstantValue()
    {
        return constantValue;
    }

    public Constraint gte(Expression other)
    {
        return new Constraint(subtract(other), Constraint.Comparison.GTE);
    }

    public Constraint gte(BigFraction value)
    {
        return new Constraint(subtract(value), Constraint.Comparison.GTE);
    }

    public Constraint gte(BigDecimal value)
    {
        return new Constraint(subtract(value), Constraint.Comparison.GTE);
    }

    public Constraint gte(BigInteger value)
    {
        return new Constraint(subtract(value), Constraint.Comparison.GTE);
    }

    public Constraint gte(long value)
    {
        return new Constraint(subtract(value), Constraint.Comparison.GTE);
    }

    public Constraint gte(double value)
    {
        return new Constraint(subtract(value), Constraint.Comparison.GTE);
    }

    public Constraint lte(Expression other)
    {
        return new Constraint(subtract(other), Constraint.Comparison.LTE);
    }

    public Constraint lte(BigFraction value)
    {
        return new Constraint(subtract(value), Constraint.Comparison.LTE);
    }

    public Constraint lte(BigDecimal value)
    {
        return new Constraint(subtract(value), Constraint.Comparison.LTE);
    }

    public Constraint lte(BigInteger value)
    {
        return new Constraint(subtract(value), Constraint.Comparison.LTE);
    }

    public Constraint lte(long value)
    {
        return new Constraint(subtract(value), Constraint.Comparison.LTE);
    }

    public Constraint lte(double value)
    {
        return new Constraint(subtract(value), Constraint.Comparison.LTE);
    }

    public Expression multiply(BigFraction multiplicand)
    {
        if (multiplicand.signum() == 0){
            return zero();
        }else if (multiplicand.compareTo(BigFraction.one()) == 0){
            return new Expression(coefficients, constantValue);
        }

        Map<DecisionVariable, BigFraction> newCoefficients = new TreeMap<>(Variable.COMPARATOR);

        for (var entry : coefficients.entrySet()){
            newCoefficients.put(entry.getKey(), entry.getValue().multiply(multiplicand));
        }

        return new Expression(newCoefficients, constantValue.multiply(multiplicand));
    }

    public Expression multiply(BigDecimal multiplicand)
    {
        return multiply(BigFraction.valueOf(multiplicand));
    }

    public Expression multiply(BigInteger multiplicand)
    {
        return multiply(BigFraction.valueOf(multiplicand));
    }

    public Expression multiply(long multiplicand)
    {
        return multiply(BigFraction.valueOf(multiplicand));
    }

    public Expression multiply(double multiplicand)
    {
        return multiply(BigDecimal.valueOf(multiplicand));
    }

    public Expression negate()
    {
        Map<DecisionVariable, BigFraction> newCoefficients = new TreeMap<>(Variable.COMPARATOR);

        for (var entry : coefficients.entrySet()){
            newCoefficients.put(entry.getKey(), entry.getValue().negate());
        }

        return new Expression(newCoefficients, constantValue.negate());
    }

    public Expression subtract(Expression subtrahend)
    {
        return add(subtrahend.negate());
    }

    public Expression subtract(BigFraction subtrahend)
    {
        return new Expression(coefficients, constantValue.subtract(subtrahend));
    }

    public Expression subtract(BigDecimal subtrahend)
    {
        return subtract(BigFraction.valueOf(subtrahend));
    }

    public Expression subtract(BigInteger subtrahend)
    {
        return subtract(BigFraction.valueOf(subtrahend));
    }

    public Expression subtract(long subtrahend)
    {
        return subtract(BigFraction.valueOf(subtrahend));
    }

    public Expression subtract(double subtrahend)
    {
        return subtract(BigDecimal.valueOf(subtrahend));
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        appendToStringBuilder(sb);

        return sb.toString();
    }
}
