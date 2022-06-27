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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class MILPSolver
{
    private MILPSolver()
    {
    }

    private static OptimizationResult doSimplex(Model model, Expression objectiveFunction, Collection<? extends Constraint> extraConstraints, boolean maximize)
            throws InfeasibleSolutionException, UnboundedSolutionException, InterruptedException
    {
        if (maximize){
            return new TwoPhaseSimplexSolver.Builder().build()
                    .maximize(model, objectiveFunction, extraConstraints);
        }else{
            return new TwoPhaseSimplexSolver.Builder().build()
                    .minimize(model, objectiveFunction, extraConstraints);
        }
    }

    private static Map<Expression, BigFraction> getInvalidIntegerExpressions(OptimizationResult result, Collection<? extends Expression> integerExpressions)
    {
        Map<Expression, BigFraction> invalidIntegerExpressions = new HashMap<>((integerExpressions.size() * 3) / 2);
        for (Expression expression : integerExpressions){
            BigFraction value = result.getValue(expression);
            if (!value.isInteger()){
                invalidIntegerExpressions.put(expression, value);
            }
        }
        return invalidIntegerExpressions;
    }

    public static OptimizationResult maximize(Model model, Expression objectiveFunction, Collection<? extends Expression> integerExpressions)
            throws UnboundedSolutionException, InfeasibleSolutionException, InterruptedException
    {
        return optimize(model, objectiveFunction, integerExpressions, true);
    }

    /*
     * This method may not work, or be terribly slow
     */
    public static OptimizationResult minimize(Model model, Expression objectiveFunction, Collection<? extends Expression> integerExpressions)
            throws UnboundedSolutionException, InfeasibleSolutionException, InterruptedException
    {
        return optimize(model, objectiveFunction, integerExpressions, false);
    }

    private static OptimizationResult optimize(Model model, Expression objectiveFunction, Collection<? extends Expression> integerExpressions, boolean maximize)
            throws UnboundedSolutionException, InfeasibleSolutionException, InterruptedException
    {
        OptimizationResult result = doSimplex(model, objectiveFunction, Collections.emptyList(), maximize);

        Map<Expression, BigFraction> invalidIntegerExpressions = getInvalidIntegerExpressions(result, integerExpressions);

        if (invalidIntegerExpressions.isEmpty()){
            return result;
        }

        List<Collection<Constraint>> queue = new LinkedList<>();

        pushToQueue(objectiveFunction, invalidIntegerExpressions, Collections.emptyList(), queue, maximize);

        OptimizationResult bestValidResult = null;
        BigFraction bestValidResultValue = null;

        while (!queue.isEmpty()){
            if (Thread.interrupted()){
                throw new InterruptedException();
            }

            Collection<Constraint> extraConstraints = queue.remove(0);

            try {
                result = doSimplex(model, objectiveFunction, extraConstraints, maximize);
            }catch (InfeasibleSolutionException ignore){
                continue;
            }

            BigFraction objectiveValue = result.getObjectiveFunctionValue();

            if (bestValidResultValue != null && objectiveValue.compareTo(bestValidResultValue) < 0 == maximize){
                continue;
            }

            invalidIntegerExpressions = getInvalidIntegerExpressions(result, integerExpressions);

            if (invalidIntegerExpressions.isEmpty()){
                bestValidResult = result;
                bestValidResultValue = objectiveValue;
            }else{
                pushToQueue(objectiveFunction, invalidIntegerExpressions, extraConstraints, queue, maximize);
            }
        }

        if (bestValidResult == null){
            throw new InfeasibleSolutionException();
        }

        return bestValidResult;
    }

    private static void pushToQueue(Expression objectiveFunction, Map<Expression, BigFraction> invalidIntegerExpressions, Collection<? extends Constraint> currentConstraints, List<Collection<Constraint>> queue, boolean maximize)
    {
        class ExpressionData
        {
            public final Expression expression;
            public final BigDecimal integerValue;
            public final BigFraction cost;

            public ExpressionData(Expression expression, BigDecimal integerValue, BigFraction cost)
            {
                this.expression = expression;
                this.integerValue = integerValue;
                this.cost = cost;
            }
        }

        List<ExpressionData> expressionData = new ArrayList<>(invalidIntegerExpressions.size());

        for (var entry : invalidIntegerExpressions.entrySet()){
            Expression expression = entry.getKey();
            BigFraction value = entry.getValue();

            BigDecimal integerValue = value.toBigDecimal(0, RoundingMode.DOWN);

            BigFraction fraction = value.subtract(BigFraction.valueOf(integerValue));
            BigFraction cost = BigFraction.zero();

            for (var entry2 : expression.getVariableValues().entrySet()){
                Variable variable = entry2.getKey();
                BigFraction amount = entry2.getValue();
                cost = cost.add(fraction.multiply(amount.multiply(Objects.requireNonNullElse(objectiveFunction.getVariableValues().get(variable), BigFraction.zero()))));
            }

            expressionData.add(new ExpressionData(expression, integerValue, cost));
        }

        expressionData.sort(Comparator.<ExpressionData, BigFraction>comparing(e -> e.cost).reversed());

        List<Constraint> buildUp = new LinkedList<>();

        for (ExpressionData ed : expressionData){
            List<Constraint> c = new ArrayList<>(currentConstraints.size() + buildUp.size() + 1);
            c.addAll(currentConstraints);
            c.addAll(buildUp);

            Constraint gtec = ed.expression.gte(ed.integerValue.add(BigDecimal.ONE));
            Constraint ltec = ed.expression.lte(ed.integerValue);

            if (maximize){
                c.add(ltec);
                buildUp.add(gtec);
            }else{
                c.add(gtec);
                buildUp.add(ltec);
            }

            queue.add(0, c);
        }

        List<Constraint> c = new ArrayList<>(currentConstraints.size() + buildUp.size());
        c.addAll(currentConstraints);
        c.addAll(buildUp);
        queue.add(0, c);
    }
}
