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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Model
{
    private final List<Variable> variables = new ArrayList<>();
    private final Collection<Constraint> constraints = new ArrayList<>();
    private final List<Expression> integerExpressions = new ArrayList<>();

    public Model addConstraint(Constraint constraint)
    {
        this.constraints.add(Objects.requireNonNull(constraint));
        return this;
    }

    public FractionExpression addFractionVariable()
    {
        Variable v = new Variable(variables.size());
        variables.add(v);
        return v;
    }

    public IntegerExpression addIntegerVariable()
    {
        return integer(addFractionVariable());
    }

    Collection<Constraint> getConstraints()
    {
        return Collections.unmodifiableCollection(constraints);
    }

    int getNumberOfConstraints()
    {
        return constraints.size();
    }

    Collection<? extends Variable> getVariables()
    {
        return Collections.unmodifiableList(variables);
    }

    public IntegerExpression integer(Expression expression)
    {
        integerExpressions.add(expression);
        return new IntegerExpression(expression.getConstantValue(), expression.getVariableValues());
    }

    public OptimizationResult maximize(Expression objectiveFunction)
            throws InfeasibleSolutionException, UnboundedSolutionException, InterruptedException
    {
        return MILPSolver.maximize(this, objectiveFunction, integerExpressions);
    }

    public OptimizationResult minimize(Expression objectiveFunction)
            throws InfeasibleSolutionException, UnboundedSolutionException, InterruptedException
    {
        return MILPSolver.minimize(this, objectiveFunction, integerExpressions);
    }
}
