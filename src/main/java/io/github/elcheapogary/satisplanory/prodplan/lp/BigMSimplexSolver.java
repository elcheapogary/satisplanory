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

import io.github.elcheapogary.satisplanory.util.BigDecimalUtils;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;

class BigMSimplexSolver
        implements SimplexSolver
{
    private final PrintWriter logger;

    public BigMSimplexSolver()
    {
        this(null);
    }

    public BigMSimplexSolver(PrintWriter logger)
    {
        this.logger = logger;
    }

    private static BigFraction getVariableValue(TableauModel.TableauVariable v, int[] variantBasicRows, BigFraction[][] array, int cols)
    {
        int row = variantBasicRows[v.columnIndex];

        if (row < 0){
            return BigFraction.ZERO;
        }

        return array[row][cols - 1].divide(array[row][v.columnIndex]);
    }

    @Override
    public OptimizationResult maximize(Model model, Expression objectiveFunction, Collection<? extends Constraint> extraConstraints)
            throws InfeasibleSolutionException, UnboundedSolutionException, InterruptedException
    {
        final TableauModel tableauModel = TableauModel.fromModel(model, extraConstraints);
        final BigFraction[][] array = tableauModel.createBigFractionTable();
        final TableauModel.TableauVariable[] basicVariables;
        final int[] variableBasicRows;
        final int rows = array.length;
        final int cols = array[0].length;
        final BigFraction[] costs = new BigFraction[cols - 1];
        Arrays.fill(costs, BigFraction.ZERO);

        BigFraction maxCoefficient = BigFraction.ZERO;

        for (Constraint c : model.getConstraints()){
            for (Variable v : c.getVariables()){
                maxCoefficient = maxCoefficient.max(c.getVariableMultiplier(v).abs());
            }
        }

        for (Constraint c : extraConstraints){
            for (Variable v : c.getVariables()){
                maxCoefficient = maxCoefficient.max(c.getVariableMultiplier(v).abs());
            }
        }

        for (BigFraction a : objectiveFunction.getVariableValues().values()){
            maxCoefficient = maxCoefficient.max(a.abs());
        }

        for (Variable variable : model.getVariables()){
            TableauModel.VariableData vd = tableauModel.getVariableData()[variable.index];
            BigFraction weight = objectiveFunction.getVariableValues().get(variable);
            if (weight != null && weight.signum() != 0){
                costs[vd.primaryTableauVariable.columnIndex] = weight;

                if (vd.secondaryTableauVariable != null){
                    costs[vd.secondaryTableauVariable.columnIndex] = weight.negate();
                }
            }
        }

        {
            BigFraction M = maxCoefficient.multiply(-100);
            for (TableauModel.TableauVariable v : tableauModel.getArtificialVariables()){
                costs[v.columnIndex] = M;
            }
        }

        variableBasicRows = new int[tableauModel.getAllVariables().size()];
        Arrays.fill(variableBasicRows, -1);

        basicVariables = new TableauModel.TableauVariable[rows];
        for (int row = 0; row < rows; row++){
            basicVariables[row] = tableauModel.getRows().get(row).basicVariable;
            variableBasicRows[basicVariables[row].columnIndex] = row;
        }

        while (true){
            if (Thread.interrupted()){
                throw new InterruptedException();
            }

            if (logger != null){
                logger.print("\"\",\"Cj\"");
                for (int i = 0; i < cols - 1; i++){
                    logger.print(",");
                    logger.print(BigDecimalUtils.normalize(costs[i].simplify().toBigDecimal(5, RoundingMode.HALF_UP)));
                }
                logger.println();

                logger.print("\"CB\",\"B\"");
                for (int i = 0; i < cols - 1; i++){
                    logger.print(",\"");
                    logger.print(tableauModel.getAllVariables().get(i).getName());
                    logger.print("\"");
                }
                logger.println(",\"RHS\"");

                for (int row = 0; row < rows; row++){
                    logger.print(BigDecimalUtils.normalize(costs[basicVariables[row].columnIndex].simplify().toBigDecimal(5, RoundingMode.HALF_UP)));
                    logger.print(",\"");
                    logger.print(basicVariables[row].getName());
                    logger.print("\"");

                    for (int col = 0; col < cols; col++){
                        logger.print(",");
                        logger.print(BigDecimalUtils.normalize(array[row][col].simplify().toBigDecimal(5, RoundingMode.HALF_UP)));
                    }
                    logger.println();
                }
            }

            int pivotColumn = -1;
            {
                if (logger != null){
                    logger.print("\"\",\"Zj-Cj\"");
                }
                BigFraction min = BigFraction.ZERO;
                for (int column = 0; column < cols - 1; column++){
                    BigFraction zj = BigFraction.ZERO;
                    for (int row = 0; row < rows; row++){
                        zj = zj.add(costs[basicVariables[row].columnIndex].multiply(array[row][column]));
                    }

                    BigFraction zjMinusCj = zj.subtract(costs[column]);

                    if (logger != null){
                        logger.print(",");
                        logger.print(BigDecimalUtils.normalize(zjMinusCj.simplify().toBigDecimal(5, RoundingMode.HALF_UP)));
                    }

                    if (zjMinusCj.compareTo(min) < 0){
                        min = zjMinusCj;
                        pivotColumn = column;
                    }
                }

                if (logger != null){
                    logger.println();
                }

                if (pivotColumn == -1){
                    break;
                }
            }

            int pivotRow = -1;
            {
                BigFraction min = null;
                for (int row = 0; row < array.length; row++){
                    if (array[row][pivotColumn].signum() > 0){
                        BigFraction ratio = array[row][cols - 1].divide(array[row][pivotColumn]);
                        if (pivotRow == -1 || ratio.compareTo(min) < 0){
                            pivotRow = row;
                            min = ratio;
                        }
                    }
                }
            }

            if (pivotRow == -1){
                throw new UnboundedSolutionException();
            }

            if (logger != null){
                logger.println();
                logger.print("\"In:\",\"");
                logger.print(tableauModel.getAllVariables().get(pivotColumn).getName());
                logger.print("\",\"Out:\",\"");
                logger.print(basicVariables[pivotRow].getName());
                logger.println("\"");
                logger.println();
            }

            BigFraction pivotValue = array[pivotRow][pivotColumn];

            if (pivotValue.compareTo(BigFraction.ONE) != 0){
                for (int column = 0; column < array[pivotRow].length; column++){
                    array[pivotRow][column] = array[pivotRow][column].divide(pivotValue).simplify();
                }
            }

            for (int row = 0; row < rows; row++){
                if (row == pivotRow){
                    continue;
                }

                BigFraction v = array[row][pivotColumn];
                if (v.signum() != 0){
                    for (int column = 0; column < cols; column++){
                        array[row][column] = array[row][column].subtract(v.multiply(array[pivotRow][column])).simplify();
                    }
                }
            }

            variableBasicRows[basicVariables[pivotRow].columnIndex] = -1;
            variableBasicRows[pivotColumn] = pivotRow;
            basicVariables[pivotRow] = tableauModel.getAllVariables().get(pivotColumn);
        }

        for (TableauModel.TableauVariable c : tableauModel.getArtificialVariables()){
            if (getVariableValue(c, variableBasicRows, array, cols).signum() != 0){
                throw new InfeasibleSolutionException();
            }
        }

        BigFraction[] variableValues = new BigFraction[model.getVariables().size()];

        for (Variable variable : model.getVariables()){
            TableauModel.VariableData vd = tableauModel.getVariableData()[variable.index];

            BigFraction v = getVariableValue(vd.primaryTableauVariable, variableBasicRows, array, cols);

            if (vd.secondaryTableauVariable != null){
                v = v.subtract(getVariableValue(vd.secondaryTableauVariable, variableBasicRows, array, cols));
            }

            variableValues[variable.index] = v;
        }

        BigFraction objectiveFunctionValue = BigFraction.ZERO;

        for (TableauModel.TableauVariable c : tableauModel.getAllVariables()){
            objectiveFunctionValue = objectiveFunctionValue.add(costs[c.columnIndex].multiply(getVariableValue(c, variableBasicRows, array, cols)));
        }

        return new OptimizationResult(objectiveFunctionValue, variableValues);
    }
}
