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

import io.github.elcheapogary.satisplanory.model.GameData;
import io.github.elcheapogary.satisplanory.model.Item;
import io.github.elcheapogary.satisplanory.model.Recipe;
import io.github.elcheapogary.satisplanory.satisfactory.SatisfactoryData;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class ProdPlanUtils
{
    private ProdPlanUtils()
    {
    }

    public static Collection<? extends Item> getItemsUsedInRecipes(Collection<? extends Recipe> recipes)
    {
        Collection<Item> items = Item.createSet();
        for (Recipe recipe : recipes){
            for (Recipe.RecipeItem ri : recipe.getIngredients()){
                items.add(ri.getItem());
            }
            for (Recipe.RecipeItem ri : recipe.getProducts()){
                items.add(ri.getItem());
            }
        }
        return items;
    }

    static Collection<? extends Recipe> getRecipesWeCanBuild(Collection<? extends Recipe> recipes, Collection<? extends Item> inputItems)
    {
        recipes = Recipe.createSet(recipes);
        Set<Recipe> producable = Recipe.createSet();

        Set<Item> availableItems = Item.createSet();
        availableItems.addAll(inputItems);

        while (true){
            boolean addedRecipe = false;

            for (Iterator<? extends Recipe> it = recipes.iterator(); it.hasNext(); ){
                Recipe recipe = it.next();

                boolean allIngredientsAvailable = true;
                for (Recipe.RecipeItem ri : recipe.getIngredients()){
                    if (!availableItems.contains(ri.getItem())){
                        allIngredientsAvailable = false;
                        break;
                    }
                }

                if (allIngredientsAvailable){
                    addedRecipe = true;
                    it.remove();
                    producable.add(recipe);
                    for (Recipe.RecipeItem ri : recipe.getProducts()){
                        availableItems.add(ri.getItem());
                    }
                }
            }

            if (!addedRecipe){
                break;
            }
        }

        return producable;
    }

    private static void removeMaximizeWeights(ProductionPlanner planner, ProductionPlanner.Builder newPlanner)
    {
        newPlanner.clearOutputItems();
        for (Item item : planner.getOutputItems()){
            BigDecimal min = planner.getOutputItemMinimumPerMinute(item);
            if (min != null && min.signum() > 0){
                newPlanner.requireOutputItemsPerMinute(item, min);
            }
        }
    }

    public static MultiPlan getMultiPlan(GameData gameData, ProductionPlanner planner)
            throws ProductionPlanInternalException, InterruptedException, ProductionPlanNotFeatisbleException
    {
        ProductionPlan unmodifiedPlan = null;
        ProductionPlan planWithAllItems = null;
        ProductionPlan planWithAllRecipes = null;
        ProductionPlan planWithAllItemsAndRecipes = null;

        try {
            unmodifiedPlan = planner.createPlan();
        }catch (ProductionPlanNotFeatisbleException ignore){
        }

        if (unmodifiedPlan == null){
            {
                ProductionPlanner.Builder pb = planner.toBuilder();

                removeMaximizeWeights(planner, pb);

                for (var entry : SatisfactoryData.getResourceExtractionLimits().entrySet()){
                    gameData.getItemByName(entry.getKey())
                            .ifPresent(item -> {
                                pb.addInputItem(item, entry.getValue());
                            });
                }

                try {
                    planWithAllItems = pb.build().createPlan();
                }catch (ProductionPlanNotFeatisbleException ignore){
                }
            }
            {
                ProductionPlanner.Builder pb = planner.toBuilder();

                pb.addRecipes(gameData.getRecipes());

                try {
                    planWithAllRecipes = pb.build().createPlan();
                }catch (ProductionPlanNotFeatisbleException ignore){
                }
            }

            /*
             * We only add a plan with all recipes and all input items if we have no other plan
             * This is like asking "Is it possible to create these required output items at all in this game?"
             * The answer is mostly "yes", and is only actually useful if we have no other plan.
             */
            if (planWithAllItems == null && planWithAllRecipes == null){
                ProductionPlanner.Builder pb = planner.toBuilder();

                removeMaximizeWeights(planner, pb);

                for (var entry : SatisfactoryData.getResourceExtractionLimits().entrySet()){
                    gameData.getItemByName(entry.getKey())
                            .ifPresent(item -> {
                                pb.addInputItem(item, entry.getValue());
                            });
                }

                pb.addRecipes(gameData.getRecipes());

                planWithAllItemsAndRecipes = pb.build().createPlan();
            }
        }

        return new MultiPlan(planner, unmodifiedPlan, planWithAllItems, planWithAllRecipes, planWithAllItemsAndRecipes);
    }
}
