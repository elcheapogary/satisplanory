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
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

class TwoPhaseSimplexSolver
        implements SimplexSolver
{
    private final PrintWriter logger;
    private final PivotSelectionRule pivotSelectionRule;

    protected TwoPhaseSimplexSolver(Builder builder)
    {
        this.logger = builder.logger;
        this.pivotSelectionRule = builder.pivotSelectionRule;
    }

    private static void maximize(Tableau tableau, int pivotRowSkip, PivotSelectionRule pivotSelectionRule, PrintWriter logger)
            throws InterruptedException, UnboundedSolutionException
    {
        while (true){
            if (Thread.interrupted()){
                throw new InterruptedException();
            }

            Pivot pivot = pivotSelectionRule.selectPivot(tableau, pivotRowSkip);

            if (pivot == null){
                break;
            }

            tableau.pivot(pivot.row, pivot.column);

            if (logger != null){
                tableau.print(logger);
            }
        }
    }

    private Tableau createTableau(TableauModel tableauModel)
    {
        if (!tableauModel.getArtificialVariables().isEmpty()){
            tableauModel.getAllVariables().removeAll(tableauModel.getArtificialVariables());
            tableauModel.getAllVariables().addAll(0, tableauModel.getArtificialVariables());

            TableauModel.Row firstStageObjective = new TableauModel.Row(new TableauModel.TableauVariable(TableauModel.VariableType.OBJECTIVE, "A"), BigFraction.ZERO);
            firstStageObjective.setCoefficient(firstStageObjective.basicVariable, BigFraction.ONE);
            tableauModel.getAllVariables().add(0, firstStageObjective.basicVariable);
            tableauModel.getRows().add(0, firstStageObjective);

            for (TableauModel.TableauVariable a : tableauModel.getArtificialVariables()){
                firstStageObjective.setCoefficient(a, BigFraction.ONE);
            }
        }

        BigFraction[][] array = tableauModel.createBigFractionTable();

        TableauModel.TableauVariable[] tableauVariables = new TableauModel.TableauVariable[array[0].length - 1];

        for (TableauModel.TableauVariable c : tableauModel.getAllVariables()){
            tableauVariables[c.columnIndex] = c;
        }

        TableauModel.TableauVariable[] basicVariables = new TableauModel.TableauVariable[array.length];

        {
            int rowIndex = 0;
            for (TableauModel.Row row : tableauModel.getRows()){
                basicVariables[rowIndex] = row.basicVariable;
                rowIndex++;
            }
        }

        return new Tableau(array, basicVariables, tableauVariables, 0, 0);
    }

    @Override
    public OptimizationResult maximize(Model model, Expression objectiveFunction, Collection<? extends Constraint> extraConstraints)
            throws InfeasibleSolutionException, UnboundedSolutionException, InterruptedException
    {
        TableauModel tableauModel = TableauModel.fromModel(model, extraConstraints);

        TableauModel.Row objectiveFunctionRow = new TableauModel.Row(new TableauModel.TableauVariable(TableauModel.VariableType.OBJECTIVE, "P"), BigFraction.ZERO);
        objectiveFunctionRow.setCoefficient(objectiveFunctionRow.basicVariable, BigFraction.ONE);
        tableauModel.getAllVariables().add(0, objectiveFunctionRow.basicVariable);
        tableauModel.getRows().add(0, objectiveFunctionRow);

        for (var entry : objectiveFunction.getVariableValues().entrySet()){
            Variable variable = entry.getKey();
            BigFraction weight = entry.getValue();

            TableauModel.VariableData vd = tableauModel.getVariableData()[variable.index];

            objectiveFunctionRow.setCoefficient(vd.primaryTableauVariable, weight.negate());

            if (vd.secondaryTableauVariable != null){
                objectiveFunctionRow.setCoefficient(vd.secondaryTableauVariable, weight);
            }
        }

        Tableau tableau = createTableau(tableauModel);

        if (logger != null){
            tableau.print(logger);
        }

        if (tableauModel.getArtificialVariables().isEmpty()){
            maximize(tableau, 0, pivotSelectionRule, logger);
        }else{
            for (int row = 1; row < tableau.array.length; row++){
                TableauModel.TableauVariable c = tableau.basicVariables[row];
                if (c.columnIndex <= tableauModel.getArtificialVariables().size()){
                    for (int column = 0; column < tableau.array[0].length; column++){
                        tableau.array[0][column] = tableau.array[0][column].subtract(tableau.array[row][column]).simplify();
                    }
                }
            }

            if (logger != null){
                logger.println();

                tableau.print(logger);
            }

            maximize(tableau, 1, pivotSelectionRule, logger);

            for (int row = 2; row < tableau.array.length; row++){
                TableauModel.VariableType type = tableau.basicVariables[row].getType();
                if (type == TableauModel.VariableType.ARTIFICIAL_FEASIBILITY_VARIABLE){
                    throw new InfeasibleSolutionException();
                }else if (type == TableauModel.VariableType.ARTIFICIAL_BASIC_VARIABLE){
                    if (tableau.array[row][tableau.array[row].length - 1].divide(tableau.array[row][tableau.basicVariables[row].columnIndex]).signum() != 0){
                        throw new InfeasibleSolutionException();
                    }
                }
            }

            tableau = new Tableau(tableau.array, tableau.basicVariables, tableau.tableauVariables, 1, tableauModel.getArtificialVariables().size() + 1);

            maximize(tableau, 0, pivotSelectionRule, logger);
        }

        BigFraction[] tableauVariableValues = new BigFraction[tableau.tableauVariables.length];
        Arrays.fill(tableauVariableValues, BigFraction.ZERO);

        for (int row = 0; row < tableau.array.length; row++){
            TableauModel.TableauVariable c = tableau.basicVariables[row];

            tableauVariableValues[c.columnIndex] = tableau.array[row][tableau.array[row].length - 1].divide(tableau.array[row][c.columnIndex]);
        }

        BigFraction[] modelVariableValues = new BigFraction[tableauModel.getVariableData().length];

        for (int i = 0; i < modelVariableValues.length; i++){
            TableauModel.VariableData vd = tableauModel.getVariableData()[i];

            modelVariableValues[i] = tableauVariableValues[vd.primaryTableauVariable.columnIndex];

            if (vd.secondaryTableauVariable != null){
                modelVariableValues[i] = modelVariableValues[i].subtract(tableauVariableValues[vd.secondaryTableauVariable.columnIndex]);
            }

            modelVariableValues[i] = modelVariableValues[i].simplify();
        }

        return new OptimizationResult(tableauVariableValues[objectiveFunctionRow.basicVariable.columnIndex], modelVariableValues);
    }

    public enum PivotSelectionRule
    {
        /**
         * This is the stock standard simplex algorithm. It selects the column with the most negative coefficient in the
         * top row, then the row with the lowest value of {@code RHS/x} where {@code x} is the coefficient of the
         * selected column. This algorithm has no cycle detection and has no mechanism for breaking cycles - it may run
         * forever.
         */
        DANZIG{
            @Override
            Pivot selectPivot(Tableau tableau, int pivotRowSkip)
                    throws UnboundedSolutionException
            {
                int pivotColumn = -1;
                {
                    BigFraction minColVal = BigFraction.ZERO;
                    for (int col = tableau.colOffset; col < tableau.rhsColumn; col++){
                        if (tableau.array[tableau.rowOffset][col].compareTo(minColVal) < 0){
                            pivotColumn = col;
                            minColVal = tableau.array[tableau.rowOffset][col];
                        }
                    }

                    if (pivotColumn < 0){
                        return null;
                    }
                }

                PivotRow pivotRow = selectPivotRow(tableau, pivotColumn, pivotRowSkip, false);

                return new Pivot(pivotRow.row, pivotColumn);
            }
        },
        /**
         * Bland's rule.
         */
        BLAND{
            @Override
            Pivot selectPivot(Tableau tableau, int pivotRowSkip)
                    throws UnboundedSolutionException
            {
                int pivotColumn = -1;
                {
                    for (int col = tableau.colOffset; col < tableau.rhsColumn; col++){
                        if (tableau.array[tableau.rowOffset][col].signum() < 0){
                            pivotColumn = col;
                            break;
                        }
                    }

                    if (pivotColumn < 0){
                        return null;
                    }
                }

                PivotRow pivotRow = selectPivotRow(tableau, pivotColumn, pivotRowSkip, true);

                return new Pivot(pivotRow.row, pivotColumn);
            }
        },
        /**
         * <p>
         * Selects the pivot which would have the greatest increase in the objective function.
         * </p>
         * <p>
         * Pivot row is selected according to Bland's rule. Because we choose the first pivot with a specific
         * increase in objective function and only choose a different pivot if it provides a greater increase in
         * objective function, in situations where no increase to objective function is possible, then this rule is
         * effectively Bland's rule and, as such, does not cycle.
         * </p>
         * <p>
         * To be clear, this algorithm is not Bland's rule - it is effectively Bland's rule only while no increase
         * in objective function is possible. This is also the only time you actually need Bland's rule. So this
         * algorithm gives the best of both worlds - fastest progress towards optimum while possible, and no cycling
         * while that is a concern.
         * </p>
         * <p>
         * I came up with this rule independently while writing Satisplanory. I Googled and found that in 2016 Etoa
         * published a similar rule, calling it Optimal Increase, but does not include to the fallback to Bland's rule.
         * I changed this rule name to match Etoa's rule name. Etoa's paper says optimal increase (which is without
         * fallback to Bland's rule in his paper) does not cycle. I don't believe that but am too lazy to prove it
         * wrong.
         * </p>
         */
        OPTIMAL_INCREASE{
            @Override
            Pivot selectPivot(Tableau tableau, int pivotRowSkip)
                    throws UnboundedSolutionException
            {
                int pivotColumn = -1;
                PivotRow pivotRow = null;
                BigFraction objectiveIncrease = null;

                for (int col = tableau.colOffset; col < tableau.rhsColumn; col++){
                    if (tableau.array[tableau.rowOffset][col].signum() < 0){
                        PivotRow pr = selectPivotRow(tableau, col, pivotRowSkip, true);

                        BigFraction tmp = pr.ratio.negate().multiply(tableau.array[tableau.rowOffset][col]);

                        if (objectiveIncrease == null || tmp.compareTo(objectiveIncrease) > 0){
                            objectiveIncrease = tmp;
                            pivotColumn = col;
                            pivotRow = pr;
                        }
                    }
                }

                if (pivotColumn < 0){
                    return null;
                }

                return new Pivot(pivotRow.row, pivotColumn);
            }
        };

        abstract Pivot selectPivot(Tableau tableau, int pivotRowSkip)
                throws UnboundedSolutionException;

        protected PivotRow selectPivotRow(Tableau tableau, int column, int pivotRowSkip, boolean blandsRule)
                throws UnboundedSolutionException
        {
            int pivotRow = -1;
            BigFraction minRatio = null;

            for (int row = tableau.rowOffset + 1 + pivotRowSkip; row < tableau.array.length; row++){
                if (tableau.array[row][tableau.rhsColumn].signum() >= 0 && tableau.array[row][column].signum() > 0){
                    BigFraction ratio = tableau.array[row][tableau.rhsColumn].divide(tableau.array[row][column]);

                    if (pivotRow == -1){
                        pivotRow = row;
                        minRatio = ratio;
                    }else{
                        int ratioCmp = ratio.compareTo(minRatio);

                        if (ratioCmp < 0 || (blandsRule && ratioCmp == 0 && tableau.basicVariables[row].columnIndex < tableau.basicVariables[pivotRow].columnIndex)){
                            pivotRow = row;
                            minRatio = ratio;
                        }
                    }
                }
            }

            if (pivotRow < 0){
                throw new UnboundedSolutionException();
            }

            return new PivotRow(pivotRow, minRatio);
        }

        private static class PivotRow
        {
            private final int row;
            private final BigFraction ratio;

            public PivotRow(int row, BigFraction ratio)
            {
                this.row = row;
                this.ratio = ratio;
            }
        }
    }

    public static class Builder
    {
        private PrintWriter logger = null;
        private PivotSelectionRule pivotSelectionRule = PivotSelectionRule.OPTIMAL_INCREASE;

        public TwoPhaseSimplexSolver build()
        {
            return new TwoPhaseSimplexSolver(this);
        }

        public Builder setLogger(PrintWriter logger)
        {
            this.logger = logger;
            return this;
        }

        public Builder setPivotSelectionRule(PivotSelectionRule pivotSelectionRule)
        {
            this.pivotSelectionRule = Objects.requireNonNull(pivotSelectionRule);
            return this;
        }
    }

    private static class Tableau
    {
        private final BigFraction[][] array;
        private final TableauModel.TableauVariable[] basicVariables;
        private final TableauModel.TableauVariable[] tableauVariables;
        private final int rowOffset;
        private final int colOffset;
        private final int rhsColumn;

        public Tableau(BigFraction[][] array, TableauModel.TableauVariable[] basicVariables, TableauModel.TableauVariable[] tableauVariables, int rowOffset, int colOffset)
        {
            this.array = array;
            this.basicVariables = basicVariables;
            this.tableauVariables = tableauVariables;
            this.rowOffset = rowOffset;
            this.colOffset = colOffset;
            this.rhsColumn = array[0].length - 1;
        }

        public void pivot(int pivotRow, int pivotColumn)
        {
            basicVariables[pivotRow] = tableauVariables[pivotColumn];

            {
                /*
                 * This block is not strictly required. We might think that adding it makes the algorithm slower,
                 * but in testing it actually reduces the time by about 25%, probably because of gaining the ability
                 * to take advantage of the BigFraction.ONE optimizations.
                 */
                BigFraction div = array[pivotRow][pivotColumn];

                for (int col = colOffset; col < array[pivotRow].length; col++){
                    array[pivotRow][col] = array[pivotRow][col].divide(div);
                }
            }

            for (int row = rowOffset; row < array.length; row++){
                if (row == pivotRow){
                    continue;
                }

                if (array[row][pivotColumn].signum() == 0){
                    continue;
                }

                BigFraction mult = array[pivotRow][pivotColumn].divide(array[row][pivotColumn]);

                for (int col = colOffset; col < array[row].length; col++){
                    BigFraction d = array[pivotRow][col];
                    if (d.signum() != 0){
                        array[row][col] = array[row][col].subtract(d.divide(mult)).simplify();
                    }
                }
            }
        }

        private void print(PrintWriter logger)
        {
            logger.print("\"\"");
            for (TableauModel.TableauVariable c : tableauVariables){
                logger.print(",\"");
                logger.print(c.getName());
                logger.print("\"");
            }
            logger.println(",\"RHS\"");

            for (int row = 0; row < array.length; row++){
                logger.print("\"");
                logger.print(basicVariables[row].getName());
                logger.print("\"");
                for (int col = 0; col < array[0].length; col++){
                    logger.print(",");
                    logger.print(array[row][col].toBigDecimal(5, RoundingMode.HALF_UP));
                }
                logger.println();
            }
        }
    }

    private static class Pivot
    {
        public final int row;
        public final int column;

        public Pivot(int row, int column)
        {
            this.row = row;
            this.column = column;
        }
    }
}
