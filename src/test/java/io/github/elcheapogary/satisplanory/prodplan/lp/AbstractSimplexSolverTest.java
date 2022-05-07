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

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class AbstractSimplexSolverTest
{
    protected PrintWriter createLogger()
            throws IOException
    {
        return null;
    }

    protected abstract SimplexSolver createSimplexSolver(PrintWriter logger);

    @Test
    public void smallTest()
            throws UnboundedSolutionException, InfeasibleSolutionException, InterruptedException, IOException
    {
        Model model = new Model();

        FractionExpression x1 = model.addFractionVariable();
        model.addConstraint(x1.gte(-50));

        FractionExpression x3 = model.addFractionVariable();
        model.addConstraint(x3.nonNegative());

        FractionExpression x4 = model.addFractionVariable();
        model.addConstraint(x4.nonNegative());

        FractionExpression x5 = model.addFractionVariable();
        model.addConstraint(x5.nonNegative());

        FractionExpression x6 = model.addFractionVariable();
        model.addConstraint(x6.nonNegative());

        FractionExpression x7 = model.addFractionVariable();
        model.addConstraint(x7.nonNegative());

        model.addConstraint(FractionExpression.zero()
                .subtract(x1)
                .subtract(30, x5)
                .subtract(15, x6)
                .eq(0)
        );

        model.addConstraint(FractionExpression.zero()
                .subtract(x3)
                .add(20, x5)
                .eq(0)
        );

        model.addConstraint(FractionExpression.zero()
                .subtract(x4)
                .add(15, x6)
                .eq(0)
        );

        model.addConstraint(x3.gte(x7));

        model.addConstraint(x4.gte(x7));

        OptimizationResult result = solve(model, x3.add(x4).add(1000, x7));

        assertEquals(0, BigDecimal.valueOf(20).compareTo(result.getValue(x3).toBigDecimal(4, RoundingMode.HALF_UP)));
        assertEquals(0, BigDecimal.valueOf(20).compareTo(result.getValue(x4).toBigDecimal(4, RoundingMode.HALF_UP)));
    }

    private OptimizationResult solve(Model model, FractionExpression objectiveFunction)
            throws UnboundedSolutionException, InfeasibleSolutionException, InterruptedException, IOException
    {
        PrintWriter logger = createLogger();
        try {
            return createSimplexSolver(logger).maximize(model, objectiveFunction);
        }finally{
            if (logger != null){
                logger.close();
            }
        }
    }

    @Test
    public void testNotFeasible()
    {
        Model model = new Model();

        FractionExpression x = model.addFractionVariable();
        model.addConstraint(x.nonNegative());

        FractionExpression y = model.addFractionVariable();
        model.addConstraint(y.nonNegative());

        model.addConstraint(x.lte(5));
        model.addConstraint(y.lte(5));

        model.addConstraint(x.add(y).gte(100));

        assertThrows(InfeasibleSolutionException.class, () -> solve(model, x.add(y)));
    }
}
