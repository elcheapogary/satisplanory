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
import io.github.elcheapogary.satisplanory.model.Recipe;
import io.github.elcheapogary.satisplanory.prodplan.lp.FractionExpression;
import io.github.elcheapogary.satisplanory.satisfactory.SatisfactoryData;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.util.Map;

public enum OptimizationTarget
{
    MAX_OUTPUT_ITEMS(
            "Maximize Output Items",
            "Generate as much of the output items with a weight > 0"
    ){
        @Override
        FractionExpression getObjectiveFunction(FractionExpression maximizedOutputItems, FractionExpression balance, Map<Item, ? extends FractionExpression> itemInputMap, Map<Item, ? extends FractionExpression> itemOutputMap, Map<Item, ? extends FractionExpression> itemSurplusMap, Map<Recipe, ? extends FractionExpression> recipeMap)
        {
            return maximizedOutputItems.add(BigFraction.ONE.movePointRight(6), balance);
        }
    },
    MIN_POWER(
            "Minimize Power Consumption",
            "Generate a plan that uses as little power as possible"
    ){
        @Override
        FractionExpression getObjectiveFunction(FractionExpression maximizedOutputItems, FractionExpression balance, Map<Item, ? extends FractionExpression> itemInputMap, Map<Item, ? extends FractionExpression> itemOutputMap, Map<Item, ? extends FractionExpression> itemSurplusMap, Map<Recipe, ? extends FractionExpression> recipeMap)
        {
            FractionExpression powerConsumption = FractionExpression.zero();

            for (var entry : recipeMap.entrySet()){
                Recipe recipe = entry.getKey();
                FractionExpression recipeExpression = entry.getValue();

                powerConsumption = powerConsumption.add(recipe.getPowerConsumption(), recipeExpression);
            }

            return FractionExpression.zero().subtract(powerConsumption);
        }
    },
    MIN_BUILDINGS(
            "Minimize Number Of Buildings",
            "Use as few buildings as possible"
    ){
        @Override
        FractionExpression getObjectiveFunction(FractionExpression maximizedOutputItems, FractionExpression balance, Map<Item, ? extends FractionExpression> itemInputMap, Map<Item, ? extends FractionExpression> itemOutputMap, Map<Item, ? extends FractionExpression> itemSurplusMap, Map<Recipe, ? extends FractionExpression> recipeMap)
        {
            FractionExpression retv = FractionExpression.zero();

            for (FractionExpression e : recipeMap.values()){
                retv = retv.subtract(e);
            }

            return retv;
        }
    },
    MIN_INPUT_ITEMS(
            "Minimize Input Items",
            "Use as little of the input items as possible"
    ){
        @Override
        FractionExpression getObjectiveFunction(FractionExpression maximizedOutputItems, FractionExpression balance, Map<Item, ? extends FractionExpression> itemInputMap, Map<Item, ? extends FractionExpression> itemOutputMap, Map<Item, ? extends FractionExpression> itemSurplusMap, Map<Recipe, ? extends FractionExpression> recipeMap)
        {
            FractionExpression retv = FractionExpression.zero();

            for (FractionExpression e : itemInputMap.values()){
                retv = retv.subtract(e);
            }

            return retv;
        }
    },
    MIN_RESOURCE_SCARCITY(
            "Minimize Resource Scarcity",
            "Use as little scarce resources as possible. This will prefer using more abundant resources."
    ){
        @Override
        FractionExpression getObjectiveFunction(FractionExpression maximizedOutputItems, FractionExpression balance, Map<Item, ? extends FractionExpression> itemInputMap, Map<Item, ? extends FractionExpression> itemOutputMap, Map<Item, ? extends FractionExpression> itemSurplusMap, Map<Recipe, ? extends FractionExpression> recipeMap)
        {
            Map<String, Long> limits = SatisfactoryData.getResourceExtractionLimits();
            FractionExpression retv = FractionExpression.zero();

            for (var entry : itemInputMap.entrySet()){
                Item item = entry.getKey();
                FractionExpression expression = entry.getValue();

                Long l = limits.get(item.getName());

                if (l != null){
                    retv = retv.subtract(BigFraction.ONE.divide(l), expression);
                }
            }

            return retv;
        }
    };

    private final String displayName;
    private final String description;

    OptimizationTarget(String displayName, String description)
    {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    abstract FractionExpression getObjectiveFunction(FractionExpression maximizedOutputItems, FractionExpression balance, Map<Item, ? extends FractionExpression> itemInputMap, Map<Item, ? extends FractionExpression> itemOutputMap, Map<Item, ? extends FractionExpression> itemSurplusMap, Map<Recipe, ? extends FractionExpression> recipeMap);

    @Override
    public String toString()
    {
        return displayName;
    }
}
