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

    enum Comparison
    {
        LTE, GTE, EQ;
    }
}
