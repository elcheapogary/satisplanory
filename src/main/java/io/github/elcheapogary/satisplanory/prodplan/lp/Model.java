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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

public class Model
{
    private final List<DecisionVariable> decisionVariables = new ArrayList<>();
    private final Collection<BranchingConstraint> branchingConstraints = new LinkedList<>();
    private final Collection<Constraint> constraints = new LinkedList<>();

    public BinaryExpression addBinaryVariable(String name)
    {
        DecisionVariable decisionVariable = new DecisionVariable(decisionVariables.size(), name);
        decisionVariables.add(decisionVariable);
        BinaryExpression retv = new BinaryExpression(Collections.singletonMap(decisionVariable, BigFraction.one()), BigFraction.zero());
        constraints.add(retv.lte(1));
        branchingConstraints.add(new IntegerBranchingConstraint(retv));
        return retv;
    }

    public void addConstraint(Constraint constraint)
    {
        constraints.add(constraint);
    }

    public IntegerExpression addFreeIntegerVariable(String name)
    {
        return addIntegerConstraint(addFreeVariable(name));
    }

    public Expression addFreeVariable(String name)
    {
        return addVariable("+" + name).subtract(addVariable("-" + name));
    }

    public IntegerExpression addIntegerConstraint(Expression expression)
    {
        IntegerExpression retv = new IntegerExpression(expression.getCoefficients(), expression.getConstantValue());
        branchingConstraints.add(new IntegerBranchingConstraint(expression));
        return retv;
    }

    public IntegerExpression addIntegerVariable(String name)
    {
        return addIntegerConstraint(addVariable(name));
    }

    public Expression addVariable(String name)
    {
        DecisionVariable decisionVariable = new DecisionVariable(decisionVariables.size(), name);
        decisionVariables.add(decisionVariable);
        return new Expression(Collections.singletonMap(decisionVariable, BigFraction.one()), BigFraction.zero());
    }

    public void addZeroIfLessThanConstraint(Expression expression, BigFraction minimum)
    {
        Objects.requireNonNull(expression);
        Objects.requireNonNull(minimum);

        if (minimum.signum() <= 0){
            throw new IllegalArgumentException("minimum <= 0");
        }

        this.branchingConstraints.add(new ZeroIfLessThanBranchingConstraint(expression, minimum));
    }

    public void addZeroIfMoreThanConstraint(Expression expression, BigFraction maximum)
    {
        Objects.requireNonNull(expression);
        Objects.requireNonNull(maximum);

        if (maximum.signum() >= 0){
            throw new IllegalArgumentException("maximum >= 0");
        }

        this.branchingConstraints.add(new ZeroIfGreaterThanBranchingConstraint(expression, maximum));
    }

    public OptimizationResult maximize(Expression objectiveFunction)
            throws InfeasibleSolutionException, UnboundedSolutionException, InterruptedException
    {
        return maximize(Collections.singletonList(objectiveFunction));
    }

    public OptimizationResult maximize(Expression objectiveFunction, Consumer<String> logger)
            throws InfeasibleSolutionException, UnboundedSolutionException, InterruptedException
    {
        return maximize(Collections.singletonList(objectiveFunction), logger);
    }

    public OptimizationResult maximize(List<Expression> objectiveFunctions)
            throws UnboundedSolutionException, InterruptedException, InfeasibleSolutionException
    {
        return maximize(objectiveFunctions, null);
    }

    public OptimizationResult maximize(List<Expression> objectiveFunctions, Consumer<String> logger)
            throws UnboundedSolutionException, InterruptedException, InfeasibleSolutionException
    {
        if (logger != null){
            logger = new Logger(logger);

            for (DecisionVariable v : decisionVariables){
                logger.accept("x" + v.id + "=" + v.getName());
            }
            int cn = 1;
            for (Constraint c : constraints){
                logger.accept("c" + cn + ":c=" + c.getComparison() + ":" + c.getExpression().getConstantValue());
                for (var entry : c.getExpression().getCoefficients().entrySet()){
                    logger.accept("c" + cn + ":x" + entry.getKey().id + "=" + entry.getValue());
                }
                cn++;
            }
            int fn = 1;
            for (Expression e : objectiveFunctions){
                logger.accept("f" + fn + ":c=" + e.getConstantValue());
                for (var entry : e.getCoefficients().entrySet()){
                    logger.accept("f" + fn + ":x" + entry.getKey().id + "=" + entry.getValue());
                }
                fn++;
            }
        }

        Tableau tableau = new Tableau(logger, decisionVariables);

        for (Constraint c : constraints){
            tableau.addConstraint(c);
        }

        tableau.solveFeasibility();

        List<BigFraction> objectiveFunctionValues = new ArrayList<>(objectiveFunctions.size());

        for (Expression e : objectiveFunctions){
            Tableau branchConstrainedTableau = BranchingSolver.maximize(tableau, e, branchingConstraints);
            BigFraction objectiveValue = branchConstrainedTableau.getValue(e);
            tableau.addConstraint(e.eq(objectiveValue));
            tableau.solveFeasibility();
        }

        if (!objectiveFunctions.isEmpty()){
            Expression lastObjectiveFunction = objectiveFunctions.get(objectiveFunctions.size() - 1);

            Set<DecisionVariable> variablesToMinimize = new TreeSet<>(Variable.COMPARATOR);
            variablesToMinimize.addAll(decisionVariables);
            variablesToMinimize.removeAll(lastObjectiveFunction.getCoefficients().keySet());

            Map<DecisionVariable, BigFraction> coefficients = new TreeMap<>(Variable.COMPARATOR);

            for (DecisionVariable v : variablesToMinimize){
                coefficients.put(v, BigFraction.negativeOne());
            }

            tableau = BranchingSolver.maximize(tableau, new Expression(coefficients, BigFraction.zero()), branchingConstraints);
        }

        Map<DecisionVariable, BigFraction> decisionVariableValues = new TreeMap<>(Variable.COMPARATOR);

        for (DecisionVariable dv : decisionVariables){
            decisionVariableValues.put(dv, tableau.getValue(dv));
        }

        return new OptimizationResult(objectiveFunctionValues, decisionVariableValues);
    }

    private static class Logger
            implements Consumer<String>
    {
        private final Consumer<String> logger;

        public Logger(Consumer<String> logger)
        {
            this.logger = logger;
        }

        @Override
        public synchronized void accept(String s)
        {
            logger.accept(Thread.currentThread().getName() + " " + s);
        }
    }
}
