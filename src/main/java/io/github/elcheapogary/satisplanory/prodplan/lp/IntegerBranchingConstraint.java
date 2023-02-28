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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class IntegerBranchingConstraint
        extends BranchingConstraint
{
    private final Expression expression;

    public IntegerBranchingConstraint(Expression expression)
    {
        this.expression = expression;
    }

    @Override
    Collection<? extends Constraint> getConstraints(Tableau tableau)
    {
        BigFraction value = tableau.getValue(expression);

        if (value.isInteger()){
            return Collections.emptyList();
        }

        BigInteger lower = value.toBigInteger();

        List<Constraint> constraints = new ArrayList<>(2);

        constraints.add(expression.lte(lower));
        constraints.add(expression.gte(lower.add(BigInteger.ONE)));

        return constraints;
    }
}
