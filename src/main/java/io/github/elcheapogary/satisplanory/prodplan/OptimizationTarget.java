/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.prodplan;

import io.github.elcheapogary.satisplanory.model.Item;
import io.github.elcheapogary.satisplanory.model.MatterState;
import io.github.elcheapogary.satisplanory.model.Recipe;
import io.github.elcheapogary.satisplanory.prodplan.lp.Expression;
import io.github.elcheapogary.satisplanory.satisfactory.SatisfactoryData;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public enum OptimizationTarget
{
    MAX_OUTPUT_ITEMS{
        @Override
        List<? extends Expression> getObjectiveFunctions(OptimizationModel model)
        {
            List<Expression> objectiveFunctions = new LinkedList<>();

            Map<Item, ? extends BigFraction> itemMaximizeWeights = model.getItemMaximizeWeightMap();

            boolean strictMaximizeRatios = false;

            if (itemMaximizeWeights.size() > 1){
                if (strictMaximizeRatios){
                    Expression balanceVariable = model.getLpModel().addVariable("Balance");
                    objectiveFunctions.add(balanceVariable);

                    for (var entry : itemMaximizeWeights.entrySet()){
                        Item item = entry.getKey();
                        BigFraction weight = entry.getValue();

                        model.getLpModel().addConstraint(model.getItemSurplusMap().get(item).eq(balanceVariable.multiply(item.fromDisplayAmount(weight))));
                    }
                }else{
                    Expression balanceExpression = Expression.zero();
                    List<Item> maxItems = new ArrayList<>(itemMaximizeWeights.keySet());
                    Map<Item, Expression> perItemBalanceVariableMap = Item.createMap();
                    for (int i = 0; i < maxItems.size() - 1; i++){
                        Item a = maxItems.get(i);
                        for (int j = i + 1; j < maxItems.size(); j++){
                            Item b = maxItems.get(j);

                            Expression balanceVariable = model.getLpModel().addVariable("Balance between " + a.getName() + " and " + b.getName());
                            balanceExpression = balanceExpression.add(balanceVariable);
                            model.getLpModel().addConstraint(model.getItemSurplusMap().get(a).gte(balanceVariable.multiply(a.fromDisplayAmount(itemMaximizeWeights.get(a)))));
                            model.getLpModel().addConstraint(model.getItemSurplusMap().get(b).gte(balanceVariable.multiply(b.fromDisplayAmount(itemMaximizeWeights.get(b)))));

                            model.getLpModel().addConstraint(perItemBalanceVariableMap.computeIfAbsent(a, item -> model.getLpModel().addVariable("Balance for item: " + item.getName())).lte(balanceVariable));
                            model.getLpModel().addConstraint(perItemBalanceVariableMap.computeIfAbsent(b, item -> model.getLpModel().addVariable("Balance for item: " + item.getName())).lte(balanceVariable));
                        }
                    }

                    objectiveFunctions.add(balanceExpression);

                    Expression finalVariable = model.getLpModel().addVariable("Final balance variable");

                    balanceExpression = Expression.zero();

                    for (Expression v : perItemBalanceVariableMap.values()){
                        balanceExpression = balanceExpression.add(v);
                        model.getLpModel().addConstraint(v.gte(finalVariable));
                    }

                    objectiveFunctions.add(0, balanceExpression);
                    objectiveFunctions.add(0, finalVariable);
                }
            }

            if (!itemMaximizeWeights.isEmpty()){
                Expression maximizeExpression = Expression.zero();
                for (var entry : itemMaximizeWeights.entrySet()){
                    Item item = entry.getKey();
                    BigFraction weight = entry.getValue();
                    maximizeExpression = maximizeExpression.add(
                            model.getItemSurplusMap().get(item)
                                    .multiply(item.toDisplayAmount(weight))
                    );
                }

                objectiveFunctions.add(maximizeExpression);
            }

            return objectiveFunctions;
        }
    },
    MIN_POWER{
        @Override
        List<? extends Expression> getObjectiveFunctions(OptimizationModel model)
        {
            Expression powerConsumption = Expression.zero();

            for (var entry : model.getRecipeMap().entrySet()){
                Recipe recipe = entry.getKey();
                Expression recipeExpression = entry.getValue();

                powerConsumption = powerConsumption.add(recipeExpression.multiply(recipe.getPowerConsumption()));
            }

            return Collections.singletonList(powerConsumption.negate());
        }
    },
    MIN_BUILDINGS{
        @Override
        List<? extends Expression> getObjectiveFunctions(OptimizationModel model)
        {
            Expression objectiveFunction = Expression.zero();

            for (Expression e : model.getRecipeMap().values()){
                objectiveFunction = objectiveFunction.subtract(e);
            }

            return Collections.singletonList(objectiveFunction);
        }
    },
    MAX_INPUT_ITEMS{
        @Override
        List<? extends Expression> getObjectiveFunctions(OptimizationModel model)
        {
            Expression objectiveFunction = Expression.zero();

            for (Expression e : model.getItemInputMap().values()){
                objectiveFunction = objectiveFunction.add(e);
            }

            return Collections.singletonList(objectiveFunction);
        }
    },
    MIN_INPUT_ITEMS{
        @Override
        List<? extends Expression> getObjectiveFunctions(OptimizationModel model)
        {
            Expression objectiveFunction = Expression.zero();

            for (Expression e : model.getItemInputMap().values()){
                objectiveFunction = objectiveFunction.subtract(e);
            }

            return Collections.singletonList(objectiveFunction);
        }
    },
    MIN_RESOURCE_SCARCITY{
        @Override
        List<? extends Expression> getObjectiveFunctions(OptimizationModel model)
        {
            Map<String, Long> limits = SatisfactoryData.getResourceExtractionLimits();
            Expression objectiveFunction = Expression.zero();

            for (var entry : model.getItemInputMap().entrySet()){
                Item item = entry.getKey();
                Expression expression = entry.getValue();

                Long l = limits.get(item.getName());

                if (l != null){
                    objectiveFunction = objectiveFunction.subtract(expression.divide(l));
                }
            }

            return Collections.singletonList(objectiveFunction);
        }
    },
    MIN_BYPRODUCTS{
        @Override
        List<? extends Expression> getObjectiveFunctions(OptimizationModel model)
        {
            Expression objectiveFunction = Expression.zero();

            for (Expression e : model.getItemSurplusMap().values()){
                objectiveFunction = objectiveFunction.subtract(e);
            }

            return Collections.singletonList(objectiveFunction);
        }
    },
    MAX_SINK_POINTS {
        @Override
        List<? extends Expression> getObjectiveFunctions(OptimizationModel model)
        {
            Expression objective = Expression.zero();

            for (var entry : model.getItemSurplusMap().entrySet()){
                Item item = entry.getKey();
                Expression expression = entry.getValue();

                if (item.getMatterState() == MatterState.SOLID && item.getSinkValue() > 0){
                    objective = objective.add(expression.multiply(item.getSinkValue()));
                }
            }

            return Collections.singletonList(objective);
        }
    };

    abstract List<? extends Expression> getObjectiveFunctions(OptimizationModel model);
}
