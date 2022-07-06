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

public class Constraint
{
    private final Expression expression;
    private final Comparison comparison;

    Constraint(Expression expression, Comparison comparison)
    {
        this.expression = expression;
        this.comparison = comparison;
    }

    Comparison getComparison()
    {
        return comparison;
    }

    Expression getExpression()
    {
        return expression;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        Expression tmpExpression = expression;
        BigFraction constant = expression.getConstantValue();

        if (constant.signum() != 0){
            tmpExpression = tmpExpression.subtract(constant);
            constant = constant.negate();
        }

        tmpExpression.appendToStringBuilder(sb);

        sb.append(" ");

        sb.append(switch (comparison){
            case EQ -> "==";
            case GTE -> ">=";
            case LTE -> "<=";
        });

        sb.append(" ");
        sb.append(constant.toString());

        return sb.toString();
    }

    enum Comparison
    {
        LTE, GTE, EQ;
    }
}
