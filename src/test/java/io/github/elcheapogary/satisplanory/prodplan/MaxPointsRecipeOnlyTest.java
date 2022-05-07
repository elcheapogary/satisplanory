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
import io.github.elcheapogary.satisplanory.model.test.TestGameData;
import io.github.elcheapogary.satisplanory.prodplan.lp.FractionExpression;
import io.github.elcheapogary.satisplanory.prodplan.lp.InfeasibleSolutionException;
import io.github.elcheapogary.satisplanory.prodplan.lp.Model;
import io.github.elcheapogary.satisplanory.prodplan.lp.OptimizationResult;
import io.github.elcheapogary.satisplanory.prodplan.lp.UnboundedSolutionException;
import io.github.elcheapogary.satisplanory.satisfactory.SatisfactoryData;
import io.github.elcheapogary.satisplanory.util.BigDecimalUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class MaxPointsRecipeOnlyTest
{
    public static void main(String[] args)
            throws UnboundedSolutionException, InfeasibleSolutionException, InterruptedException
    {
        TestGameData gd = TestGameData.getUpdate5GameData();

        Map<Item, BigDecimal> inputItems = Item.createMap();
        for (Map.Entry<String, Long> entry : SatisfactoryData.getResourceExtractionLimits().entrySet()){
            inputItems.put(gd.requireItemByName(entry.getKey()), BigDecimal.valueOf(entry.getValue()));
        }
        inputItems.put(gd.requireItemByName("Water"), BigDecimal.valueOf(500000000L));

        Collection<? extends Recipe> recipes = ProdPlanUtils.getRecipesWeCanBuild(gd.getRecipes(), inputItems.keySet());
        Collection<? extends Item> items = ProdPlanUtils.getItemsUsedInRecipes(recipes);

        Model model = new Model();

        Map<Item, FractionExpression> itemExpressionMap = Item.createMap();
        Map<Recipe, FractionExpression> recipeVariableMap = Recipe.createMap();

        for (Item item : items){
            FractionExpression expression = FractionExpression.zero();
            itemExpressionMap.put(item, expression);
        }

        for (Recipe recipe : recipes){
            FractionExpression variable = model.addFractionVariable();
            model.addConstraint(variable.nonNegative());
            recipeVariableMap.put(recipe, variable);

            for (Recipe.RecipeItem ri : recipe.getIngredients()){
                itemExpressionMap.compute(ri.getItem(), (item, expression) ->
                        Objects.requireNonNullElse(expression, FractionExpression.zero())
                                .subtract(ri.getAmount().getAmountPerMinute(), variable)
                );
            }
            for (Recipe.RecipeItem ri : recipe.getProducts()){
                itemExpressionMap.compute(ri.getItem(), (item, expression) ->
                        Objects.requireNonNullElse(expression, FractionExpression.zero())
                                .add(ri.getAmount().getAmountPerMinute(), variable)
                );
            }
        }

        FractionExpression objectiveFunction = FractionExpression.zero();

        for (Item item : items){
            BigDecimal min = Objects.requireNonNullElse(inputItems.get(item), BigDecimal.ZERO).negate();
            if (item.getMatterState() == MatterState.SOLID){
                objectiveFunction = objectiveFunction.add(item.getSinkValue(), itemExpressionMap.get(item));
                model.addConstraint(itemExpressionMap.get(item).gte(min));
            }else{
                if (min.signum() == 0){
                    model.addConstraint(itemExpressionMap.get(item).eq(0));
                }else{
                    model.addConstraint(itemExpressionMap.get(item).gte(min));
                    model.addConstraint(itemExpressionMap.get(item).lte(0));
                }
            }
        }

        OptimizationResult result;
        {
            long startTime = System.nanoTime();

            result = model.maximize(objectiveFunction);

            Duration duration = Duration.ofNanos(System.nanoTime() - startTime);

            System.out.println("================================================================================");
            System.out.println("================================================================================");
            System.out.println("================================================================================");
            System.out.println("Runtime: " + duration.toString());
        }

        System.out.println("Points: " + BigDecimalUtils.normalize(result.getObjectiveFunctionValue().toBigDecimal(5, RoundingMode.HALF_UP)));

        for (var entry : itemExpressionMap.entrySet()){
            Item item = entry.getKey();
            FractionExpression expression = entry.getValue();

            System.out.println("Item: " + item.getName() + ": " + BigDecimalUtils.normalize(expression.getValue(result).toBigDecimal(5, RoundingMode.HALF_UP)));
        }
    }
}
