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

import java.util.Collection;
import java.util.Collections;

interface SimplexSolver
{
    default OptimizationResult maximize(Model model, Expression objectiveFunction)
            throws InfeasibleSolutionException, UnboundedSolutionException, InterruptedException
    {
        return maximize(model, objectiveFunction, Collections.emptyList());
    }

    OptimizationResult maximize(Model model, Expression objectiveFunction, Collection<? extends Constraint> extraConstraints)
            throws InfeasibleSolutionException, UnboundedSolutionException, InterruptedException;

    default OptimizationResult minimize(Model model, Expression objectiveFunction)
            throws UnboundedSolutionException, InfeasibleSolutionException, InterruptedException
    {
        return minimize(model, objectiveFunction, Collections.emptyList());
    }

    default OptimizationResult minimize(Model model, Expression objectiveFunction, Collection<? extends Constraint> extraConstraints)
            throws UnboundedSolutionException, InfeasibleSolutionException, InterruptedException
    {
        return maximize(model, objectiveFunction.negate(), extraConstraints).negateObjectiveFunctionValue();
    }
}
