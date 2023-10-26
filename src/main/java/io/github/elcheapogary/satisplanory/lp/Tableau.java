/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package io.github.elcheapogary.satisplanory.lp;

import io.github.elcheapogary.satisplanory.util.BigFraction;
import io.github.elcheapogary.satisplanory.util.function.throwing.tryreturn.Try;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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

    public Tableau(Tableau copy)
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
            for (var entry : r.getCoefficients()){
                TableauVariable v = variableCopyMap.get(entry.getKey());
                c.set(v, entry.getValue());
            }
        }
    }

    private TableauVariable addArtificialVariable()
    {
        TableauVariable variable = addVariable("a");
        artificialVariables.add(variable);
        return variable;
    }

    public void addConstraint(Constraint constraint)
    {
        Row row = addRow();

        row.constant = constraint.getExpression().getConstantValue().negate();

        for (var entry : constraint.getExpression().getCoefficients().entrySet()){
            TableauVariable v = variables.get(entry.getKey().id);
            if (!v.knownZero){
                BigFraction value = entry.getValue();
                row.set(v, value);
            }
        }

        row.subtractBasicVariableRows();

        TableauVariable basicVariable = null;

        if (constraint.getComparison() == Constraint.Comparison.LTE){
            basicVariable = addSlackVariable();
            row.set(basicVariable, BigFraction.one());
        }else if (constraint.getComparison() == Constraint.Comparison.GTE){
            basicVariable = addSlackVariable();
            row.set(basicVariable, BigFraction.negativeOne());
        }

        if (row.constant.signum() < 0){
            row.negate();
        }

        if (row.constant.signum() == 0 && row.getCoefficients().isEmpty()){
            removeRow(row);
        }else{
            if (basicVariable == null || row.getCoefficient(basicVariable).signum() < 1){
                basicVariable = addArtificialVariable();
                row.set(basicVariable, BigFraction.one());
            }
            setBasicVariable(row, basicVariable);
        }
    }

    public Objective addObjective(Expression expression)
    {
        Row objectiveRow = addRow();

        TableauVariable objectiveVariable = addVariable("P");

        objectiveRow.set(objectiveVariable, BigFraction.one());
        setBasicVariable(objectiveRow, objectiveVariable);

        objectiveRow.constant = expression.getConstantValue();

        for (var entry : expression.getCoefficients().entrySet()){
            TableauVariable v = variables.get(entry.getKey().id);
            if (!v.knownZero){
                BigFraction c = entry.getValue();
                objectiveRow.set(v, c.negate());
            }
        }

        if (logger != null){
            debugTableau();
            logger.accept("Subtracting basic variable rows from objective row");
        }

        objectiveRow.subtractBasicVariableRows();

        return new Objective(objectiveRow.id, objectiveVariable.id);
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
                if (!v.knownZero){
                    variableMaxWidths.put(v, Math.max(debugGetMaxWidth(v.rows, row -> row.getCoefficient(v).toBigDecimal(2, RoundingMode.HALF_UP).toString()), v.getDebugName().length()));
                }
            }

            {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("%" + maxBasicVariableNameWidth + "s", "B"));

                for (TableauVariable v : variables.values()){
                    if (!v.knownZero){
                        sb.append(String.format(" %" + variableMaxWidths.get(v) + "s", v.getDebugName()));
                    }
                }

                sb.append(String.format(" %" + maxRhsWidth + "s", "RHS"));
                logger.accept(sb.toString());
            }

            for (Row row : rows.values()){
                if (row.constant.signum() == 0 && row.getCoefficients().isEmpty()){
                    continue;
                }

                StringBuilder sb = new StringBuilder();
                sb.append(String.format("%" + maxBasicVariableNameWidth + "s", row.basicVariable == null ? "?" : row.basicVariable.getDebugName()));

                for (TableauVariable v : variables.values()){
                    if (!v.knownZero){
                        BigFraction c = row.getCoefficient(v);
                        String s;
                        if (c == null || c.signum() == 0){
                            s = "";
                        }else{
                            s = c.toBigDecimal(2, RoundingMode.UP).toString();
                        }
                        sb.append(String.format(" %" + variableMaxWidths.get(v) + "s", s));
                    }
                }

                sb.append(String.format(" %" + maxRhsWidth + "s", row.constant.toBigDecimal(2, RoundingMode.HALF_UP)));
                logger.accept(sb.toString());
            }
            logger.accept("");
        }
    }

    public BigFraction getValue(Expression expression)
    {
        try (var stream = expression.getCoefficients().entrySet().parallelStream()) {
            return stream.map(entry -> getValue(entry.getKey()).multiply(entry.getValue()))
                    .filter(v -> v.signum() != 0)
                    .reduce(BigFraction.zero(), BigFraction::add)
                    .add(expression.getConstantValue());
        }
    }

    public BigFraction getValue(DecisionVariable decisionVariable)
    {
        TableauVariable variable = variables.get(decisionVariable.id);

        if (variable.basicRow == null){
            return BigFraction.zero();
        }

        return variable.basicRow.constant.divide(variable.basicRow.getCoefficient(variable));
    }

    public BigFraction getValue(Objective objective)
    {
        assert objective != null;

        Row row = rows.get(objective.rowId);
        TableauVariable variable = variables.get(objective.variableId);

        assert row != null;
        assert variable != null;
        assert variable.basicRow == row;
        assert row.basicVariable == variable;

        return row.constant;
    }

    public BigFraction maximize(Objective objective)
            throws UnboundedSolutionException, InterruptedException
    {
        assert objective != null;

        Row objectiveRow = rows.get(objective.rowId);
        TableauVariable objectiveVariable = variables.get(objective.variableId);

        assert objectiveRow != null;
        assert objectiveVariable != null;
        assert objectiveVariable.knownZero || objectiveVariable.basicRow == objectiveRow;

        return maximize(objectiveRow);
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

            try (var stream = objectiveRow.getCoefficients().parallelStream()) {
                pivot = stream.filter(entry -> entry.getValue().signum() < 0)
                        .map(catcher.function(entry -> {
                            TableauVariable v = entry.getKey();
                            BigFraction objectiveRowValue = entry.getValue();

                            try (var rowStream = v.rows.parallelStream()) {
                                return rowStream.filter(row -> row != objectiveRow)
                                        .filter(row -> row.constant.signum() >= 0)
                                        .map(row -> Pair.of(row, row.getCoefficient(v)))
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

    private void pivot(TableauVariable variable, Row pivotRow)
    {
        setBasicVariable(pivotRow, variable);

        {
            BigFraction pivotValue = pivotRow.getCoefficient(variable);
            if (!pivotValue.equals(BigFraction.one())){
                pivotRow.divide(pivotValue);
            }
        }

        try (var stream = new ArrayList<>(variable.rows).parallelStream()) {
            stream.filter(row -> row != pivotRow)
                    .forEach(r -> r.subtract(r.getCoefficient(variable), pivotRow));
        }
    }

    private void removeKnownZeros()
    {
        Set<Row> potentiallyRemovableRows = new TreeSet<>(Row.COMPARATOR);
        for (Row row : rows.values()){
            if (row.constant.signum() == 0){
                potentiallyRemovableRows.add(row);
            }
        }

        while (!potentiallyRemovableRows.isEmpty()){
            Set<Row> recheckRows = new TreeSet<>(Row.COMPARATOR);

            for (Row row : potentiallyRemovableRows){

                boolean hasNegativeCoefficients = false;
                for (var entry : row.getCoefficients()){
                    if (entry.getValue().signum() < 0){
                        hasNegativeCoefficients = true;
                        break;
                    }
                }

                if (!hasNegativeCoefficients){
                    Collection<? extends TableauVariable> variables = new ArrayList<>(row.variables());
                    for (TableauVariable v : variables){
                        for (Row r : v.rows){
                            if (r.constant.signum() == 0){
                                recheckRows.add(r);
                            }
                        }
                        setKnownZero(v);
                    }
                    /*
                     * We don't actually remove the row, because it may be referenced by an objective. The row
                     * will not take part in any future pivots though, since it has no coefficients.
                     */
                }

                recheckRows.remove(row);
            }

            potentiallyRemovableRows = recheckRows;
        }
    }

    public void removeObjective(Objective objective)
    {
        assert objective != null;

        Row row = rows.get(objective.rowId);
        TableauVariable variable = variables.get(objective.variableId);

        assert row != null;
        assert variable != null;

        removeVariable(variable);
        removeRow(row);
    }

    private void removeRow(Row r)
    {
        if (r.basicVariable != null){
            r.basicVariable.basicRow = null;
            r.basicVariable = null;
        }
        for (TableauVariable v : r.variables()){
            v.rows.remove(r);
        }
        r.coefficients.clear();
        rows.remove(r.id);
    }

    private void removeVariable(TableauVariable v)
    {
        setKnownZero(v);
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

    private void setKnownZero(TableauVariable v)
    {
        if (v.basicRow != null){
            v.basicRow.basicVariable = null;
            v.basicRow = null;
        }
        for (Row row : v.rows){
            row.coefficients.remove(v);
        }
        v.rows.clear();
        v.knownZero = true;
    }

    public void solveFeasibility()
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

                    for (TableauVariable ov : v.basicRow.variables()){
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
            logger.accept("Removing known zeros");
        }

        removeKnownZeros();

        if (logger != null){
            debugTableau();
        }
    }

    public static class Objective
    {
        private final int rowId;
        private final int variableId;

        public Objective(int rowId, int variableId)
        {
            this.rowId = rowId;
            this.variableId = variableId;
        }
    }

    private record Pair<A, B>(A first, B second)
    {
        public static <A, B> Pair<A, B> of(A first, B second)
        {
            return new Pair<>(first, second);
        }
    }

    private static class Row
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
            for (var entry : other.getCoefficients()){
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

        public BigFraction getCoefficient(TableauVariable variable)
        {
            BigFraction r = coefficients.get(variable);

            if (r == null){
                r = BigFraction.zero();
            }

            return r;
        }

        public Collection<? extends Map.Entry<? extends TableauVariable, ? extends BigFraction>> getCoefficients()
        {
            return coefficients.entrySet();
        }

        private int getId()
        {
            return id;
        }

        public void negate()
        {
            constant = constant.negate();
            try (var stream = coefficients.entrySet().parallelStream()) {
                stream.forEach(e -> e.setValue(e.getValue().negate()));
            }
        }

        public void set(TableauVariable variable, BigFraction coefficient)
        {
            if (coefficient.signum() == 0){
                BigFraction oldValue = coefficients.remove(variable);
                if (oldValue != null){
                    variable.rows.remove(this);
                }
            }else{
                BigFraction oldValue = coefficients.put(variable, coefficient);
                if (oldValue == null){
                    variable.rows.add(this);
                }
            }
        }

        public void subtract(BigFraction multiple, Row other)
        {
            add(multiple.negate(), other);
        }

        public void subtractBasicVariableRows()
        {
            Row r;

            try (var stream = coefficients.entrySet().parallelStream()) {
                r = stream.filter(e -> e.getKey().basicRow != null && e.getKey().basicRow != Row.this)
                        .map(e -> {
                            Row basicRow = e.getKey().basicRow;
                            Row retv = new Row(0);
                            retv.constant = basicRow.constant.multiply(e.getValue());
                            for (var entry : basicRow.coefficients.entrySet()){
                                retv.coefficients.put(entry.getKey(), entry.getValue().multiply(e.getValue()));
                            }
                            return retv;
                        })
                        .reduce(new Row(0), (r1, r2) -> {
                            Row retv = new Row(0);
                            retv.constant = r1.constant.add(r2.constant);
                            retv.coefficients.putAll(r1.coefficients);
                            for (var entry : r2.coefficients.entrySet()){
                                retv.coefficients.compute(entry.getKey(), (v, c) -> {
                                    BigFraction n;

                                    if (c == null){
                                        n = entry.getValue();
                                    }else{
                                        n = entry.getValue().add(c);
                                    }

                                    if (n.signum() == 0){
                                        return null;
                                    }

                                    return n;
                                });
                            }
                            return retv;
                        });
            }

            subtract(BigFraction.one(), r);
        }

        public Collection<? extends TableauVariable> variables()
        {
            return coefficients.keySet();
        }
    }

    private static class TableauVariable
            extends Variable
    {
        private final String debugName;
        private final Set<Row> rows = Collections.synchronizedSet(new TreeSet<>(Row.COMPARATOR));
        private Row basicRow;
        private boolean knownZero = false;

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
