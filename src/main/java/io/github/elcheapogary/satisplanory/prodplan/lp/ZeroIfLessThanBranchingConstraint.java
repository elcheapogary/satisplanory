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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class ZeroIfLessThanBranchingConstraint
        extends BranchingConstraint
{
    private final Expression expression;
    private final BigFraction minimum;

    public ZeroIfLessThanBranchingConstraint(Expression expression, BigFraction minimum)
    {
        this.expression = expression;
        this.minimum = minimum;
    }

    @Override
    Collection<? extends Constraint> getConstraints(Tableau tableau)
    {
        BigFraction value = tableau.getValue(expression);

        if (value.signum() > 0 && value.compareTo(minimum) < 0){
            List<Constraint> constraints = new ArrayList<>(2);
            constraints.add(expression.eq(0));
            constraints.add(expression.gte(minimum));
            return constraints;
        }else{
            return Collections.emptyList();
        }
    }
}
