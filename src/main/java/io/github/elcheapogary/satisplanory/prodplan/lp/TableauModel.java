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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class TableauModel
{
    private final VariableData[] variableData;
    private final List<Row> rows;
    private final List<TableauVariable> tableauVariables;
    private final List<TableauVariable> decisionVariables;
    private final List<TableauVariable> slackVariables;
    private final List<TableauVariable> artificialVariables;

    private TableauModel(VariableData[] variableData, List<Row> rows, VariableSet variableSet)
    {
        this.variableData = variableData;
        this.rows = new ArrayList<>(rows);
        this.decisionVariables = new ArrayList<>(variableSet.getDecisionVariables());
        this.slackVariables = new ArrayList<>(variableSet.getSlackVariables());
        this.artificialVariables = new ArrayList<>(variableSet.getArtificialVariables());
        this.tableauVariables = new ArrayList<>(decisionVariables.size() + slackVariables.size() + artificialVariables.size());
        this.tableauVariables.addAll(this.decisionVariables);
        this.tableauVariables.addAll(this.slackVariables);
        this.tableauVariables.addAll(this.artificialVariables);
    }

    public static TableauModel fromModel(Model model, Collection<? extends Constraint> extraConstraints)
    {
        List<Constraint> constraints = new ArrayList<>(model.getNumberOfConstraints() + extraConstraints.size());
        constraints.addAll(model.getConstraints());
        constraints.addAll(extraConstraints);

        List<Row> rows = new LinkedList<>();
        VariableData[] variableData = new VariableData[model.getVariables().size()];
        VariableSet variableSet = new VariableSet();

        for (Variable variable : model.getVariables()){
            TableauVariable primaryTableauVariable = variableSet.addDecisionVariable();
            TableauVariable secondaryTableauVariable = null;
            if (!isVariableNonNegative(variable, constraints)){
                secondaryTableauVariable = variableSet.addDecisionNegativeVariable();
            }
            variableData[variable.index] = new VariableData(primaryTableauVariable, secondaryTableauVariable);
        }

        {
            for (Constraint constraint : constraints){
                BigDecimal min = constraint.getMin();
                BigDecimal max = constraint.getMax();

                if (min != null && max != null && min.compareTo(max) == 0){
                    TableauVariable a = variableSet.addArtificialBasicVariable();

                    if (min.signum() < 0){
                        Row row = new Row(a, min.negate());
                        rows.add(row);

                        setCoefficients(constraint, row, variableData, true);

                        row.setCoefficient(a, BigDecimal.ONE);
                    }else{
                        Row row = new Row(a, min);
                        rows.add(row);

                        setCoefficients(constraint, row, variableData, false);

                        row.setCoefficient(a, BigDecimal.ONE);
                    }
                }else{
                    if (min != null && !isSingleVariableNonNegativeMinConstraint(constraint)){
                        int zeroCmp = min.signum();
                        if (zeroCmp < 0){
                            TableauVariable slack = variableSet.addSlackVariable();

                            Row row = new Row(slack, min.negate());
                            rows.add(row);

                            setCoefficients(constraint, row, variableData, true);

                            row.setCoefficient(slack, BigDecimal.ONE);
                        }else if (zeroCmp == 0){
                            TableauVariable slack = variableSet.addSlackVariable();

                            Row row = new Row(slack, min);
                            rows.add(row);

                            setCoefficients(constraint, row, variableData, true);

                            row.setCoefficient(slack, BigDecimal.ONE);
                        }else{
                            TableauVariable slack = variableSet.addSlackVariable();

                            TableauVariable artificial = variableSet.addArtificialFeasibilityVariable();

                            Row row = new Row(artificial, min);
                            rows.add(row);

                            setCoefficients(constraint, row, variableData, false);

                            row.setCoefficient(slack, BigDecimal.ONE.negate());
                            row.setCoefficient(artificial, BigDecimal.ONE);
                        }
                    }

                    if (max != null && !isSingleVariableNonNegativeMaxConstraint(constraint)){
                        int zeroCmp = max.signum();
                        if (zeroCmp < 0){
                            TableauVariable slack = variableSet.addSlackVariable();

                            TableauVariable artificial = variableSet.addArtificialFeasibilityVariable();

                            Row row = new Row(artificial, max.negate());
                            rows.add(row);

                            setCoefficients(constraint, row, variableData, true);

                            row.setCoefficient(slack, BigDecimal.ONE.negate());
                            row.setCoefficient(artificial, BigDecimal.ONE);
                        }else{
                            TableauVariable slack = variableSet.addSlackVariable();

                            Row row = new Row(slack, max);
                            rows.add(row);

                            setCoefficients(constraint, row, variableData, false);

                            row.setCoefficient(slack, BigDecimal.ONE);
                        }
                    }
                }
            }
        }

        return new TableauModel(variableData, rows, variableSet);
    }

    private static boolean isSingleVariableNonNegativeMaxConstraint(Constraint c)
    {
        if (c.getVariables().size() != 1){
            return false;
        }

        Variable v = c.getVariables().iterator().next();

        BigDecimal m = c.getVariableMultiplier(v);

        return m.signum() < 0 && c.getMax().signum() == 0;
    }

    private static boolean isSingleVariableNonNegativeMinConstraint(Constraint c)
    {
        if (c.getVariables().size() != 1){
            return false;
        }

        Variable v = c.getVariables().iterator().next();

        BigDecimal m = c.getVariableMultiplier(v);

        return m.signum() > 0 && c.getMin().signum() == 0;
    }

    private static boolean isVariableNonNegative(Variable variable, Collection<? extends Constraint> constraints)
    {
        for (Constraint constraint : constraints){
            if (constraint.getVariables().size() == 1){
                BigDecimal m = constraint.getVariableMultiplier(variable);

                if (m == null){
                    continue;
                }

                if (m.signum() < 0 && constraint.getMax() != null && constraint.getMax().signum() <= 0){
                    return true;
                }else if (m.signum() > 0 && constraint.getMin() != null && constraint.getMin().signum() >= 0){
                    return true;
                }
            }
        }

        return false;
    }

    private static void setCoefficients(Constraint constraint, Row row, VariableData[] variableData, boolean negate)
    {
        for (Variable v : constraint.getVariables()){
            VariableData vd = variableData[v.index];
            BigDecimal multiple = constraint.getVariableMultiplier(v);
            if (multiple.signum() != 0){
                if (negate){
                    multiple = multiple.negate();
                }
                row.setCoefficient(vd.primaryTableauVariable, multiple);
                if (vd.secondaryTableauVariable != null){
                    row.setCoefficient(vd.secondaryTableauVariable, multiple.negate());
                }
            }
        }
    }

    public BigFraction[][] createBigFractionTable()
    {
        BigFraction[][] array = new BigFraction[rows.size()][tableauVariables.size() + 1];

        for (BigFraction[] a : array){
            Arrays.fill(a, BigFraction.ZERO);
        }

        {
            int columnIndex = 0;
            for (TableauVariable c : tableauVariables){
                c.columnIndex = columnIndex;
                columnIndex++;
            }
        }

        {
            int rowIndex = 0;
            for (TableauModel.Row row : rows){
                row.index = rowIndex;
                array[rowIndex][tableauVariables.size()] = BigFraction.valueOf(row.rhs).simplify();
                for (Map.Entry<TableauVariable, BigDecimal> entry : row.values.entrySet()){
                    TableauVariable c = entry.getKey();
                    BigDecimal v = entry.getValue();

                    array[rowIndex][c.columnIndex] = BigFraction.valueOf(v).simplify();
                }

                rowIndex++;
            }
        }

        return array;
    }

    public List<TableauVariable> getArtificialVariables()
    {
        return artificialVariables;
    }

    public List<TableauVariable> getAllVariables()
    {
        return tableauVariables;
    }

    public List<TableauVariable> getDecisionVariables()
    {
        return decisionVariables;
    }

    public List<Row> getRows()
    {
        return rows;
    }

    public List<TableauVariable> getSlackVariables()
    {
        return slackVariables;
    }

    public VariableData[] getVariableData()
    {
        return variableData;
    }

    enum VariableType
    {
        OBJECTIVE,
        DECISION_VARIABLE,
        DECISION_VARIABLE_NEGATIVE,
        SLACK_VARIABLE,
        ARTIFICIAL_FEASIBILITY_VARIABLE,
        ARTIFICIAL_BASIC_VARIABLE
    }

    static class VariableData
    {
        public final TableauVariable primaryTableauVariable;
        public final TableauVariable secondaryTableauVariable;

        public VariableData(TableauVariable primaryTableauVariable, TableauVariable secondaryTableauVariable)
        {
            this.primaryTableauVariable = primaryTableauVariable;
            this.secondaryTableauVariable = secondaryTableauVariable;
        }
    }

    static class Row
    {
        public final TableauVariable basicVariable;
        private final Map<TableauVariable, BigDecimal> values = new TreeMap<>(Comparator.comparing(TableauVariable::getName));
        private final BigDecimal rhs;
        public int index = -1;

        public Row(TableauVariable basicVariable, BigDecimal rhs)
        {
            this.basicVariable = basicVariable;
            this.rhs = rhs;
        }

        public Set<TableauVariable> getVariables()
        {
            return values.keySet();
        }

        public void setCoefficient(TableauVariable tableauVariable, BigDecimal value)
        {
            values.put(tableauVariable, value);
        }
    }

    static class TableauVariable
    {
        private final VariableType type;
        private final String name;
        public int columnIndex = -1;

        public TableauVariable(VariableType variableType, String name)
        {
            this.type = variableType;
            this.name = name;
        }

        public String getName()
        {
            return name;
        }

        public VariableType getType()
        {
            return type;
        }
    }

    private static class VariableSet
    {
        private final List<TableauVariable> decisionVariables = new LinkedList<>();
        private final List<TableauVariable> slackVariables = new LinkedList<>();
        private final List<TableauVariable> artificialVariables = new LinkedList<>();

        public TableauVariable addArtificialBasicVariable()
        {
            TableauVariable c = new TableauVariable(VariableType.ARTIFICIAL_BASIC_VARIABLE, "b" + (artificialVariables.size() + 1));
            artificialVariables.add(c);
            return c;
        }

        public TableauVariable addArtificialFeasibilityVariable()
        {
            TableauVariable c = new TableauVariable(VariableType.ARTIFICIAL_FEASIBILITY_VARIABLE, "a" + (artificialVariables.size() + 1));
            artificialVariables.add(c);
            return c;
        }

        public TableauVariable addDecisionNegativeVariable()
        {
            TableauVariable c = new TableauVariable(VariableType.DECISION_VARIABLE_NEGATIVE, "y" + (decisionVariables.size() + 1));
            decisionVariables.add(c);
            return c;
        }

        public TableauVariable addDecisionVariable()
        {
            TableauVariable c = new TableauVariable(VariableType.DECISION_VARIABLE, "x" + (decisionVariables.size() + 1));
            decisionVariables.add(c);
            return c;
        }

        public TableauVariable addSlackVariable()
        {
            TableauVariable c = new TableauVariable(VariableType.SLACK_VARIABLE, "s" + (slackVariables.size() + 1));
            slackVariables.add(c);
            return c;
        }

        private List<TableauVariable> getArtificialVariables()
        {
            return artificialVariables;
        }

        private List<TableauVariable> getDecisionVariables()
        {
            return decisionVariables;
        }

        private List<TableauVariable> getSlackVariables()
        {
            return slackVariables;
        }
    }
}
