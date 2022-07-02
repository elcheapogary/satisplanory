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
import io.github.elcheapogary.satisplanory.util.function.throwing.tryreturn.Try;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.RecursiveAction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

class Tableau
{
    private final Consumer<String> logger;
    private final Map<Integer, Row> rows = new TreeMap<>();
    private final Map<Integer, TableauVariable> variables = new TreeMap<>();
    private final Collection<TableauVariable> artificialVariables = new TreeSet<>(Variable.COMPARATOR);
    private int maxVariableId;
    private int maxRowId;

    public Tableau(Consumer<String> logger, Collection<? extends DecisionVariable> decisionVariables)
    {
        this.logger = logger;
        for (DecisionVariable dv : decisionVariables){
            TableauVariable tv = new TableauVariable(dv.id, "x" + dv.id);
            this.variables.put(tv.id, tv);
        }
        maxVariableId = decisionVariables.size();
    }

    private Tableau(Tableau copy)
    {
        this.maxVariableId = copy.maxVariableId;
        this.maxRowId = copy.maxRowId;
        this.logger = copy.logger;

        Map<TableauVariable, TableauVariable> variableCopyMap = new TreeMap<>(Variable.COMPARATOR);
        for (TableauVariable v : copy.variables.values()){
            TableauVariable c = new TableauVariable(v.id, v.debugName);
            variableCopyMap.put(v, c);
            this.variables.put(c.id, c);
        }

        for (TableauVariable v : copy.artificialVariables){
            this.artificialVariables.add(variableCopyMap.get(v));
        }

        for (Row r : copy.rows.values()){
            Row c = new Row(r.id);
            rows.put(c.id, c);
            c.constant = r.constant;
            if (r.basicVariable != null){
                c.basicVariable = variableCopyMap.get(r.basicVariable);
                c.basicVariable.basicRow = c;
            }
            for (var entry : r.coefficients.entrySet()){
                TableauVariable v = variableCopyMap.get(entry.getKey());
                c.set(v, entry.getValue());
            }
        }
    }

    private static Tableau enforceIntegerConstraints(Tableau tableau, int objectiveRowId, Collection<? extends Expression> integerExpressions)
            throws UnboundedSolutionException, InfeasibleSolutionException
    {
        final BestSolutionHolder bestSolutionHolder = new BestSolutionHolder();

        List<BranchingConstraint> branchingConstraints = new LinkedList<>();

        for (Expression e : integerExpressions){
            branchingConstraints.add(new IntegerBranchingConstraint(e));
        }

        new ApplyBranchingConstraintsAction(tableau, objectiveRowId, branchingConstraints, bestSolutionHolder).fork().join();

        tableau = bestSolutionHolder.getBestTableau();

        if (tableau == null){
            throw new InfeasibleSolutionException();
        }

        return tableau;
    }

    private TableauVariable addArtificialVariable()
    {
        TableauVariable variable = addVariable("a");
        artificialVariables.add(variable);
        return variable;
    }

    public void addConstraint(Constraint constraint)
    {
        addConstraintRow(constraint);
    }

    private Row addConstraintRow(Constraint constraint)
    {
        Row row = addRow();

        BigFraction rhs = constraint.getExpression().getConstantValue().negate();

        if (constraint.getComparison() == Constraint.Comparison.LTE){
            if (rhs.signum() >= 0){
                TableauVariable slackVariable = addSlackVariable();
                row.setCoefficients(constraint.getExpression().getCoefficients(), false);
                row.constant = rhs;
                row.set(slackVariable, BigFraction.one());
                setBasicVariable(row, slackVariable);
            }else{
                TableauVariable artificialVariable = addArtificialVariable();
                TableauVariable slackVariable = addSlackVariable();
                row.setCoefficients(constraint.getExpression().getCoefficients(), true);
                row.constant = rhs.negate();
                row.set(slackVariable, BigFraction.one().negate());
                row.set(artificialVariable, BigFraction.one());
                setBasicVariable(row, artificialVariable);
            }
        }else if (constraint.getComparison() == Constraint.Comparison.GTE){
            if (rhs.signum() <= 0){
                TableauVariable slackVariable = addSlackVariable();
                row.setCoefficients(constraint.getExpression().getCoefficients(), true);
                row.constant = rhs.negate();
                row.set(slackVariable, BigFraction.one());
                setBasicVariable(row, slackVariable);
            }else{
                TableauVariable artificialVariable = addArtificialVariable();
                TableauVariable slackVariable = addSlackVariable();
                row.setCoefficients(constraint.getExpression().getCoefficients(), false);
                row.constant = rhs;
                row.set(slackVariable, BigFraction.one().negate());
                row.set(artificialVariable, BigFraction.one());
                setBasicVariable(row, artificialVariable);
            }
        }else if (constraint.getComparison() == Constraint.Comparison.EQ){
            if (rhs.signum() >= 0){
                TableauVariable artificialVariable = addArtificialVariable();
                row.setCoefficients(constraint.getExpression().getCoefficients(), false);
                row.constant = rhs;
                row.set(artificialVariable, BigFraction.one());
                setBasicVariable(row, artificialVariable);
            }else{
                TableauVariable artificialVariable = addArtificialVariable();
                row.setCoefficients(constraint.getExpression().getCoefficients(), true);
                row.constant = rhs.negate();
                row.set(artificialVariable, BigFraction.one());
                setBasicVariable(row, artificialVariable);
            }
        }else{
            throw new IllegalArgumentException("Unsupported comparison type: " + constraint.getComparison());
        }

        return row;
    }

    private Row addRow()
    {
        Row row = new Row(maxRowId);
        maxRowId++;
        rows.put(row.id, row);
        return row;
    }

    private TableauVariable addSlackVariable()
    {
        return addVariable("s");
    }

    private TableauVariable addVariable(String prefix)
    {
        TableauVariable variable = new TableauVariable(maxVariableId, prefix + maxVariableId);
        maxVariableId++;
        variables.put(variable.id, variable);
        return variable;
    }

    private int debugGetMaxWidth(Collection<? extends Row> rows, Function<? super Row, String> stringExtractor)
    {
        int max = 0;

        for (Row r : rows){
            max = Math.max(max, stringExtractor.apply(r).length());
        }

        return max;
    }

    private void debugTableau()
    {
        synchronized (logger){
            logger.accept("");
            int maxBasicVariableNameWidth = debugGetMaxWidth(rows.values(),
                    row -> Optional.ofNullable(row.basicVariable)
                            .map(TableauVariable::getDebugName)
                            .orElse("?")
            );
            int maxRhsWidth = debugGetMaxWidth(rows.values(), row -> row.constant.toBigDecimal(2, RoundingMode.HALF_UP).toString());

            Map<Variable, Integer> variableMaxWidths = new TreeMap<>(Variable.COMPARATOR);

            for (TableauVariable v : variables.values()){
                variableMaxWidths.put(v, Math.max(debugGetMaxWidth(v.rows, row -> row.coefficients.get(v).toBigDecimal(2, RoundingMode.HALF_UP).toString()), v.getDebugName().length()));
            }

            {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("%" + maxBasicVariableNameWidth + "s", "B"));

                for (Variable v : variables.values()){
                    sb.append(String.format(" %" + variableMaxWidths.get(v) + "s", v.getDebugName()));
                }

                sb.append(String.format(" %" + maxRhsWidth + "s", "RHS"));
                logger.accept(sb.toString());
            }

            for (Row row : rows.values()){
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("%" + maxBasicVariableNameWidth + "s", row.basicVariable == null ? "?" : row.basicVariable.getDebugName()));

                for (TableauVariable v : variables.values()){
                    BigFraction c = row.coefficients.get(v);
                    String s;
                    if (c == null){
                        s = "";
                    }else{
                        s = c.toBigDecimal(2, RoundingMode.UP).toString();
                    }
                    sb.append(String.format(" %" + variableMaxWidths.get(v) + "s", s));
                }

                sb.append(String.format(" %" + maxRhsWidth + "s", row.constant.toBigDecimal(2, RoundingMode.HALF_UP)));
                logger.accept(sb.toString());
            }
            logger.accept("");
        }
    }

    public void findInitialFeasibleSolution()
            throws UnboundedSolutionException, InterruptedException, InfeasibleSolutionException
    {
        if (artificialVariables.isEmpty()){
            return;
        }

        Row objectiveRow = addRow();

        TableauVariable objectiveVariable = addVariable("A");

        objectiveRow.set(objectiveVariable, BigFraction.one());
        setBasicVariable(objectiveRow, objectiveVariable);

        for (TableauVariable v : artificialVariables){
            objectiveRow.set(v, BigFraction.one());
        }

        if (logger != null){
            logger.accept("Finding initial feasible solution");
            debugTableau();
            logger.accept("Subtracting basic variable rows from artificial variable minimization objective row (A)");
        }

        objectiveRow.subtractBasicVariableRows();

        maximize(objectiveRow);

        for (TableauVariable v : artificialVariables){
            if (v.basicRow != null){
                if (v.basicRow.constant.signum() == 0){
                    TableauVariable pivotVariable = null;

                    for (TableauVariable ov : v.basicRow.coefficients.keySet()){
                        if (ov != v){
                            pivotVariable = ov;
                            break;
                        }
                    }

                    if (pivotVariable == null){
                        removeRow(v.basicRow);
                    }else{
                        if (logger != null){
                            logger.accept("Artificial variable " + v.getDebugName() + " is basic with zero value, pivoting on first non-basic variable: " + pivotVariable.getDebugName());
                        }

                        pivot(pivotVariable, v.basicRow);

                        if (logger != null){
                            debugTableau();
                        }
                    }
                }else{
                    throw new InfeasibleSolutionException();
                }
            }

            removeVariable(v);
        }

        artificialVariables.clear();

        removeVariable(objectiveVariable);
        removeRow(objectiveRow);

        if (logger != null){
            logger.accept("Artificial variable minimization objective and artificial variables removed");
            debugTableau();
        }
    }

    private BigFraction getValue(Expression expression)
    {
        try (var stream = expression.getCoefficients().entrySet().parallelStream()) {
            return stream.map(entry -> getValue(entry.getKey()).multiply(entry.getValue()))
                    .reduce(expression.getConstantValue(), BigFraction::add);
        }
    }

    public BigFraction getValue(DecisionVariable decisionVariable)
    {
        TableauVariable variable = variables.get(decisionVariable.id);

        if (variable.basicRow == null){
            return BigFraction.zero();
        }

        return variable.basicRow.constant.divide(variable.basicRow.coefficients.get(variable));
    }

    private BigFraction maximize(Row objectiveRow)
            throws UnboundedSolutionException, InterruptedException
    {
        while (true){
            if (Thread.interrupted()){
                throw new InterruptedException();
            }

            if (logger != null){
                debugTableau();
            }

            var catcher = Try.catching(UnboundedSolutionException.class);

            Pair<TableauVariable, Pair<Row, BigFraction>> pivot;

            try (var stream = objectiveRow.coefficients.entrySet().parallelStream()) {
                pivot = stream.filter(entry -> entry.getValue().signum() < 0)
                        .map(catcher.function(entry -> {
                            TableauVariable v = entry.getKey();
                            BigFraction objectiveRowValue = entry.getValue();

                            try (var rowStream = v.rows.parallelStream()) {
                                return rowStream.filter(row -> row != objectiveRow)
                                        .filter(row -> row.constant.signum() >= 0)
                                        .map(row -> Pair.of(row, row.coefficients.get(v)))
                                        .filter(p -> p.second.signum() > 0)
                                        .map(p -> Pair.of(p.first, p.first.constant.divide(p.second)))
                                        .min(Comparator.<Pair<Row, BigFraction>, BigFraction>comparing(o -> o.second)
                                                .thenComparingInt(o -> o.first.basicVariable.id)
                                        )
                                        .map(p -> {
                                            if (p.second.signum() == 0){
                                                return p;
                                            }else{
                                                return Pair.of(p.first, p.second.multiply(objectiveRowValue.negate()));
                                            }
                                        })
                                        .map(p -> Pair.of(v, p))
                                        .orElseThrow(() -> new UnboundedSolutionException(v));
                            }
                        }))
                        .collect(catcher.collector(Collectors.maxBy(Comparator.<Pair<TableauVariable, Pair<Row, BigFraction>>, BigFraction>comparing(t -> t.second.second)
                                .thenComparing(Comparator.<Pair<TableauVariable, Pair<Row, BigFraction>>>comparingInt(t -> t.first.id).reversed()))))
                        .getOrThrow()
                        .orElse(null);
            }

            if (pivot == null){
                if (logger != null){
                    logger.accept("Maximization complete");
                }
                return objectiveRow.constant;
            }

            TableauVariable pivotVariable = pivot.first;
            Row pivotRow = pivot.second.first;

            if (logger != null){
                logger.accept("Pivoting, entering: " + pivotVariable.getDebugName() + ((pivotRow.basicVariable == null) ? "" : (", exiting: " + pivotRow.basicVariable.getDebugName())));
                logger.accept("Pivot increase: " + pivot.second.second.toBigDecimal(2, RoundingMode.HALF_UP));
            }

            pivot(pivotVariable, pivotRow);
        }
    }

    public Tableau maximize(Expression expression, Collection<? extends Expression> integerVariables)
            throws UnboundedSolutionException, InterruptedException, InfeasibleSolutionException
    {
        Row objectiveRow = addRow();

        TableauVariable objectiveVariable = addVariable("P");

        objectiveRow.set(objectiveVariable, BigFraction.one());
        setBasicVariable(objectiveRow, objectiveVariable);

        objectiveRow.constant = expression.getConstantValue();

        for (var entry : expression.getCoefficients().entrySet()){
            TableauVariable v = variables.get(entry.getKey().id);
            BigFraction c = entry.getValue();

            objectiveRow.set(v, c.negate());
        }

        if (logger != null){
            debugTableau();
            logger.accept("Subtracting basic variable rows from objective row");
        }

        objectiveRow.subtractBasicVariableRows();

        maximize(objectiveRow);

        Tableau retv = this;

        if (!integerVariables.isEmpty()){
            retv = Tableau.enforceIntegerConstraints(new Tableau(this), objectiveRow.id, integerVariables);

            if (logger != null){
                logger.accept("Tableau after enforcing integer constraints, removing objective row and variable");
                retv.debugTableau();
            }

            retv.removeVariable(retv.variables.get(objectiveVariable.id));
            retv.removeRow(retv.rows.get(objectiveRow.id));
        }

        /*
         * We need to remove the objective row and variable from this tableau even if this is not the one being
         * returned. maximizeAndConstrain() calls this and then still uses this tableau afterwards.
         */
        removeVariable(objectiveVariable);
        removeRow(objectiveRow);

        if (logger != null){
            logger.accept("Tableau after removing objective row and variable");
            retv.debugTableau();
        }

        return retv;
    }

    public BigFraction maximizeAndConstrain(Expression expression, Collection<? extends Expression> integerVariables)
            throws UnboundedSolutionException, InterruptedException, InfeasibleSolutionException
    {
        if (logger != null){
            logger.accept("Maximizing " + expression);
        }

        Tableau t = maximize(expression, integerVariables);
        BigFraction objectiveValue = t.getValue(expression);

        if (logger != null){
            logger.accept("Adding constraint = " + objectiveValue.toBigDecimal(2, RoundingMode.HALF_UP));
        }

        Row constraintRow = addRow();

        for (var entry : expression.getCoefficients().entrySet()){
            constraintRow.set(variables.get(entry.getKey().id), entry.getValue());
        }

        constraintRow.constant = objectiveValue.subtract(expression.getConstantValue());

        if (logger != null){
            debugTableau();
            logger.accept("Subtracting basic variable rows");
        }

        constraintRow.subtractBasicVariableRows();

        if (logger != null){
            debugTableau();
        }

        /*
         * Remember - the values of this tableau may differ from the values in the tableau in which the integer
         * constraints were set (i.e. the tableau from which we get the objective value).
         *
         * Having subtracted the basic rows from the constraint row, the constraint row's constant may or may not
         * be zero.
         *
         * If non-zero:
         *      this value is the difference between the non-integer-constrained (this) tableau objective value and
         *      the constrained tableau objective value. We need to assign this difference to an artificial variable
         *      and solve initial feasibility (lose the artificial variable) to redistribute this value between
         *      whatever non-basic variable have coefficients in the row.
         *
         *      This solving of initial feasibility should never fail.
         */

        if (constraintRow.constant.signum() == 0){
            if (constraintRow.coefficients.isEmpty()){
                removeRow(constraintRow);
            }else{
                pivot(constraintRow.variables().iterator().next(), constraintRow);
            }
        }else{
            if (constraintRow.constant.signum() < 0){
                constraintRow.negate();
            }

            TableauVariable artificialVariable = addArtificialVariable();
            constraintRow.set(artificialVariable, BigFraction.one());
            setBasicVariable(constraintRow, artificialVariable);

            try {
                findInitialFeasibleSolution();
            }catch (InfeasibleSolutionException | UnboundedSolutionException e){
                e.printStackTrace();
                throw new AssertionError(e);
            }
        }

        return objectiveValue;
    }

    private void pivot(TableauVariable variable, Row pivotRow)
    {
        setBasicVariable(pivotRow, variable);

        {
            BigFraction pivotValue = pivotRow.coefficients.get(variable);
            if (!pivotValue.equals(BigFraction.one())){
                pivotRow.divide(pivotValue);
            }
        }

        try (var stream = new ArrayList<>(variable.rows).parallelStream()) {
            stream.filter(row -> row != pivotRow)
                    .forEach(r -> r.subtract(r.coefficients.get(variable), pivotRow));
        }
    }

    private void removeRow(Row r)
    {
        if (r.basicVariable != null){
            r.basicVariable.basicRow = null;
            r.basicVariable = null;
        }
        for (TableauVariable v : r.coefficients.keySet()){
            v.rows.remove(r);
        }
        r.coefficients.clear();
        rows.remove(r.id);
    }

    private void removeVariable(TableauVariable v)
    {
        if (v.basicRow != null){
            v.basicRow.basicVariable = null;
            v.basicRow = null;
        }
        for (Row row : v.rows){
            row.coefficients.remove(v);
        }
        v.rows.clear();
        variables.remove(v.id);
    }

    private void setBasicVariable(Row row, TableauVariable basicVariable)
    {
        if (row.basicVariable != null){
            row.basicVariable.basicRow = null;
        }

        basicVariable.basicRow = row;
        row.basicVariable = basicVariable;
    }

    private class Row
    {
        private static final Comparator<Row> COMPARATOR = Comparator.comparingInt(Row::getId);
        private final int id;
        private final Map<TableauVariable, BigFraction> coefficients = new TreeMap<>(Variable.COMPARATOR);
        private BigFraction constant = BigFraction.zero();
        private TableauVariable basicVariable;

        public Row(int id)
        {
            this.id = id;
        }

        public void add(BigFraction multiple, Row other)
        {
            constant = constant.add(multiple.multiply(other.constant));
            for (var entry : other.coefficients.entrySet()){
                TableauVariable variable = entry.getKey();
                BigFraction coefficient = entry.getValue();

                coefficients.compute(variable, (v, oldValue) -> {
                    BigFraction newValue = Objects.requireNonNullElse(oldValue, BigFraction.zero());
                    newValue = newValue.add(multiple.multiply(coefficient));

                    if (newValue.signum() == 0){
                        if (oldValue != null){
                            v.rows.remove(Row.this);
                        }
                        return null;
                    }else{
                        if (oldValue == null){
                            v.rows.add(Row.this);
                        }
                        return newValue;
                    }
                });
            }
        }

        public void divide(BigFraction divisor)
        {
            constant = constant.divide(divisor);
            try (var stream = coefficients.entrySet().parallelStream()) {
                stream.forEach(entry -> entry.setValue(entry.getValue().divide(divisor)));
            }
        }

        private int getId()
        {
            return id;
        }

        public void negate()
        {
            constant = constant.negate();
            for (var entry : coefficients.entrySet()){
                entry.setValue(entry.getValue().negate());
            }
        }

        public void set(TableauVariable variable, BigFraction coefficient)
        {
            if (coefficient.signum() == 0){
                coefficients.remove(variable);
                variable.rows.remove(this);
            }else{
                coefficients.put(variable, coefficient);
                variable.rows.add(this);
            }
        }

        public void setCoefficients(Map<DecisionVariable, BigFraction> coefficients, boolean negate)
        {
            for (var entry : coefficients.entrySet()){
                TableauVariable v = variables.get(entry.getKey().id);
                BigFraction value = entry.getValue();
                if (negate){
                    set(v, value.negate());
                }else{
                    set(v, value);
                }
            }
        }

        public void subtract(BigFraction multiple, Row other)
        {
            add(multiple.negate(), other);
        }

        public void subtractBasicVariableRows()
        {
            for (TableauVariable v : new ArrayList<>(coefficients.keySet())){
                if (v.basicRow != null && v.basicRow != this){
                    subtract(coefficients.get(v), v.basicRow);
                }
            }
        }

        public Collection<? extends TableauVariable> variables()
        {
            return coefficients.keySet();
        }
    }

    private static class ApplyBranchingConstraintsAction
            extends RecursiveAction
    {
        private final Tableau tableau;
        private final int objectiveRowId;
        private final Collection<? extends BranchingConstraint> branchingConstraints;
        private final BestSolutionHolder bestSolutionHolder;

        public ApplyBranchingConstraintsAction(Tableau tableau, int objectiveRowId, Collection<? extends BranchingConstraint> branchingConstraints, BestSolutionHolder bestSolutionHolder)
        {
            this.tableau = tableau;
            this.objectiveRowId = objectiveRowId;
            this.branchingConstraints = branchingConstraints;
            this.bestSolutionHolder = bestSolutionHolder;
        }

        @Override
        protected void compute()
        {
            for (BranchingConstraint branchingConstraint : branchingConstraints){
                Collection<? extends Constraint> constraints = branchingConstraint.getConstraints(tableau);

                if (!constraints.isEmpty()){
                    List<RecursiveAction> actions = new ArrayList<>(constraints.size());
                    boolean first = true;
                    for (Constraint constraint : constraints){
                        Tableau tableau;
                        if (first){
                            first = false;
                            tableau = this.tableau;
                        }else{
                            tableau = new Tableau(this.tableau);
                        }
                        actions.add(new BoundAction(tableau, objectiveRowId, branchingConstraints, bestSolutionHolder, constraint));
                    }
                    invokeAll(actions);
                    return;
                }
            }

            Row row = tableau.rows.get(objectiveRowId);
            BigFraction objectiveFunctionValue = row.constant.divide(row.coefficients.get(row.basicVariable));

            bestSolutionHolder.submitCompleteSolution(objectiveFunctionValue, tableau);
        }
    }

    private static class BestSolutionHolder
    {
        private BigFraction bestObjectiveValue;
        private Tableau bestTableau;
        private UnboundedSolutionException error;

        public synchronized Tableau getBestTableau()
                throws UnboundedSolutionException
        {
            if (error != null){
                throw error;
            }
            return bestTableau;
        }

        public synchronized boolean isIncompleteSolutionWorthContinuing(BigFraction objectiveValue)
        {
            if (error != null){
                return false;
            }else if (bestObjectiveValue == null){
                return true;
            }else{
                return objectiveValue.compareTo(bestObjectiveValue) > 0;
            }
        }

        public synchronized void setError(UnboundedSolutionException error)
        {
            this.error = error;
        }

        public synchronized void submitCompleteSolution(BigFraction objectiveValue, Tableau tableau)
        {
            if (bestObjectiveValue == null){
                bestObjectiveValue = objectiveValue;
                bestTableau = tableau;
            }
        }
    }

    private static class BoundAction
            extends RecursiveAction
    {
        private final Tableau tableau;
        private final int objectiveRowId;
        private final Collection<? extends BranchingConstraint> branchingConstraints;
        private final BestSolutionHolder bestSolutionHolder;
        private final Constraint constraint;

        public BoundAction(Tableau tableau, int objectiveRowId, Collection<? extends BranchingConstraint> branchingConstraints, BestSolutionHolder bestSolutionHolder, Constraint constraint)
        {
            this.tableau = tableau;
            this.objectiveRowId = objectiveRowId;
            this.branchingConstraints = branchingConstraints;
            this.bestSolutionHolder = bestSolutionHolder;
            this.constraint = constraint;
        }

        @Override
        protected void compute()
        {
            Row row = tableau.addConstraintRow(constraint);

            row.subtractBasicVariableRows();

            if (row.constant.signum() < 0){
                row.negate();
            }

            TableauVariable artificialVariable = tableau.addArtificialVariable();
            row.set(artificialVariable, BigFraction.one());
            tableau.setBasicVariable(row, artificialVariable);

            try {
                tableau.findInitialFeasibleSolution();
                BigFraction objectiveValue = tableau.maximize(tableau.rows.get(objectiveRowId));
                if (bestSolutionHolder.isIncompleteSolutionWorthContinuing(objectiveValue)){
                    invokeAll(new ApplyBranchingConstraintsAction(tableau, objectiveRowId, branchingConstraints, bestSolutionHolder));
                }
            }catch (InfeasibleSolutionException ignore){
            }catch (UnboundedSolutionException e){
                bestSolutionHolder.setError(e);
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }
    }

    public static abstract class BranchingConstraint
    {
        abstract Collection<? extends Constraint> getConstraints(Tableau tableau);
    }

    public static class IntegerBranchingConstraint
            extends BranchingConstraint
    {
        private final Expression expression;

        public IntegerBranchingConstraint(Expression expression)
        {
            this.expression = expression;
        }

        @Override
        Collection<? extends Constraint> getConstraints(Tableau tableau)
        {
            BigFraction value = tableau.getValue(expression);

            if (value.isInteger()){
                return Collections.emptyList();
            }

            BigInteger lower = value.toBigInteger();

            List<Constraint> constraints = new ArrayList<>(2);

            if (lower.signum() == 0){
                constraints.add(expression.eq(0));
            }else{
                constraints.add(expression.lte(lower));
            }

            constraints.add(expression.gte(lower.add(BigInteger.ONE)));

            return constraints;
        }
    }

    private record Pair<A, B>(A first, B second)
    {
        public static <A, B> Pair<A, B> of(A first, B second)
        {
            return new Pair<>(first, second);
        }
    }

    private static class TableauVariable
            extends Variable
    {
        private final String debugName;
        private final Set<Row> rows = Collections.synchronizedSet(new TreeSet<>(Row.COMPARATOR));
        private Row basicRow;

        public TableauVariable(int id, String debugName)
        {
            super(id);
            this.debugName = debugName;
        }

        @Override
        public String getDebugName()
        {
            return debugName;
        }
    }
}
