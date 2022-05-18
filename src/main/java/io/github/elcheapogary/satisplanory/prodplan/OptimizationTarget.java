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
    MAX_OUTPUT_ITEMS(24){
        @Override
        FractionExpression getObjectiveFunction(FractionExpression maximizedOutputItems, FractionExpression balance, Map<Item, ? extends FractionExpression> itemInputMap, Map<Item, ? extends FractionExpression> itemOutputMap, Map<Item, ? extends FractionExpression> itemSurplusMap, Map<Recipe, ? extends FractionExpression> recipeMap)
        {
            return maximizedOutputItems.add(BigFraction.ONE.movePointLeft(6), balance);
        }
    },
    MIN_POWER{
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
    MIN_BUILDINGS{
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
    MAX_INPUT_ITEMS{
        @Override
        FractionExpression getObjectiveFunction(FractionExpression maximizedOutputItems, FractionExpression balance, Map<Item, ? extends FractionExpression> itemInputMap, Map<Item, ? extends FractionExpression> itemOutputMap, Map<Item, ? extends FractionExpression> itemSurplusMap, Map<Recipe, ? extends FractionExpression> recipeMap)
        {
            FractionExpression retv = FractionExpression.zero();

            for (FractionExpression e : itemInputMap.values()){
                retv = retv.add(e);
            }

            return retv;
        }
    },
    MIN_INPUT_ITEMS{
        @Override
        FractionExpression getObjectiveFunction(FractionExpression maximizedOutputItems, FractionExpression balance, Map<Item, ? extends FractionExpression> itemInputMap, Map<Item, ? extends FractionExpression> itemOutputMap, Map<Item, ? extends FractionExpression> itemSurplusMap, Map<Recipe, ? extends FractionExpression> recipeMap)
        {
            return FractionExpression.zero().subtract(MAX_INPUT_ITEMS.getObjectiveFunction(maximizedOutputItems, balance, itemInputMap, itemOutputMap, itemSurplusMap, recipeMap));
        }
    },
    MIN_RESOURCE_SCARCITY{
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
    },
    MIN_BYPRODUCTS{
        @Override
        FractionExpression getObjectiveFunction(FractionExpression maximizedOutputItems, FractionExpression balance, Map<Item, ? extends FractionExpression> itemInputMap, Map<Item, ? extends FractionExpression> itemOutputMap, Map<Item, ? extends FractionExpression> itemSurplusMap, Map<Recipe, ? extends FractionExpression> recipeMap)
        {
            FractionExpression retv = FractionExpression.zero();

            for (FractionExpression e : itemSurplusMap.values()){
                retv = retv.subtract(e);
            }

            return retv;
        }
    };

    private final int ordersOfMagnitude;

    OptimizationTarget()
    {
        this(6);
    }

    OptimizationTarget(int ordersOfMagnitude)
    {
        this.ordersOfMagnitude = ordersOfMagnitude;
    }

    abstract FractionExpression getObjectiveFunction(FractionExpression maximizedOutputItems, FractionExpression balance, Map<Item, ? extends FractionExpression> itemInputMap, Map<Item, ? extends FractionExpression> itemOutputMap, Map<Item, ? extends FractionExpression> itemSurplusMap, Map<Recipe, ? extends FractionExpression> recipeMap);

    int getOrdersOfMagnitude()
    {
        return ordersOfMagnitude;
    }
}
