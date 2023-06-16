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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LpTest
{
    @Test
    public void testIntegerVariables1()
            throws InfeasibleSolutionException, UnboundedSolutionException, InterruptedException
    {
        Model model = new Model();

        Expression a = model.addVariable("a");

        IntegerExpression b = model.addIntegerVariable("b");

        model.addConstraint(a.eq(b.multiply(2)));

        BinaryExpression c = model.addBinaryVariable("c");

        Expression d = model.addVariable("d");

        model.addConstraint(d.eq(c.multiply(3.56)));

        model.addConstraint(a.add(d).lte(15));

        OptimizationResult result = model.maximize(b.add(d));

        assertEquals(5, result.getIntegerValue(b).intValue());
    }
}
