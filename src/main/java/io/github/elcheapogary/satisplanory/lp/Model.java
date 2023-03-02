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
import java.io.StringReader;
import java.io.StringWriter;
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
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;

public class Model
{
    private final List<DecisionVariable> decisionVariables = new ArrayList<>();
    private final Collection<BranchingConstraint> branchingConstraints = new LinkedList<>();
    private final Collection<Constraint> constraints = new LinkedList<>();

    public static Model fromJson(String json)
    {
        try (JsonReader r = Json.createReader(new StringReader(json))){
            return fromJson(r.readObject());
        }
    }

    private static BranchingConstraint loadBranchingConstraintFromJson(JsonObject json, Map<Integer, ? extends DecisionVariable> decisionVariableMap)
    {
        String type = json.getString("type");

        if (IntegerBranchingConstraint.JSON_TYPE.equals(type)){
            return IntegerBranchingConstraint.fromJson(json, decisionVariableMap);
        }else if (ZeroIfLessThanBranchingConstraint.JSON_TYPE.equals(type)){
            return ZeroIfLessThanBranchingConstraint.fromJson(json, decisionVariableMap);
        }else if (ZeroIfGreaterThanBranchingConstraint.JSON_TYPE.equals(type)){
            return ZeroIfGreaterThanBranchingConstraint.fromJson(json, decisionVariableMap);
        }else{
            throw new IllegalArgumentException("Unsupported branching constraint type: " + type);
        }
    }

    public static Model fromJson(JsonObject jsonObject)
    {
        Model model = new Model();

        JsonObject decisionVariables = jsonObject.getJsonObject("variables");

        int maxId = -1;
        Map<Integer, DecisionVariable> decisionVariableMap = new TreeMap<>();

        for (String key : decisionVariables.keySet()){
            int id = Integer.parseInt(key);
            maxId = Math.max(id, maxId);
            DecisionVariable v = new DecisionVariable(id, decisionVariables.getString(key));
            model.decisionVariables.add(v);
            decisionVariableMap.put(id, v);
        }

        if (maxId + 1 != model.decisionVariables.size()){
            throw new IllegalArgumentException("Invalid decision variables list, maxId: " + maxId + " expected: " + (model.decisionVariables.size() - 1));
        }

        for (JsonObject jsonConstraint : jsonObject.getJsonArray("constraints").getValuesAs(JsonObject.class)){
            model.addConstraint(Constraint.fromJson(jsonConstraint, decisionVariableMap));
        }

        for (JsonObject jsonBranchingConstraint : jsonObject.getJsonArray("branching-constraints").getValuesAs(JsonObject.class)){
            model.branchingConstraints.add(loadBranchingConstraintFromJson(jsonBranchingConstraint, decisionVariableMap));
        }

        return model;
    }

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

            StringWriter sw = new StringWriter();
            try (JsonWriter w = Json.createWriter(sw)){
                w.writeObject(toJson());
            }
            logger.accept(sw.toString());
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

    public JsonObject toJson()
    {
        JsonObjectBuilder jsonModel = Json.createObjectBuilder();

        {
            JsonObjectBuilder jsonDecisionVariables = Json.createObjectBuilder();

            for (DecisionVariable v : decisionVariables){
                jsonDecisionVariables.add(Integer.toString(v.id), v.getName());
            }

            jsonModel.add("variables", jsonDecisionVariables.build());
        }

        {
            JsonArrayBuilder jsonConstraints = Json.createArrayBuilder();

            for (Constraint constraint : constraints){
                jsonConstraints.add(constraint.toJson());
            }

            jsonModel.add("constraints", jsonConstraints.build());
        }

        {
            JsonArrayBuilder jsonBranchingConstraints = Json.createArrayBuilder();

            for (BranchingConstraint branchingConstraint : branchingConstraints){
                jsonBranchingConstraints.add(branchingConstraint.toJson());
            }

            jsonModel.add("branching-constraints", jsonBranchingConstraints.build());
        }

        return jsonModel.build();
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
