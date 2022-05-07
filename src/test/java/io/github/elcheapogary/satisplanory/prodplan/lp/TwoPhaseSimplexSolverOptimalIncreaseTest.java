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

import java.io.PrintWriter;

public class TwoPhaseSimplexSolverOptimalIncreaseTest
        extends AbstractSimplexSolverTest
{
    @Override
    protected SimplexSolver createSimplexSolver(PrintWriter logger)
    {
        return new TwoPhaseSimplexSolver.Builder()
                .setLogger(logger)
                .setPivotSelectionRule(TwoPhaseSimplexSolver.PivotSelectionRule.OPTIMAL_INCREASE)
                .build();
    }
}
