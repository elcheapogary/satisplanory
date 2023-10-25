/*
 * Copyright (c) 2023 elcheapogary
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
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
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
                            .ifPresent(item -> pb.addInputItem(item, entry.getValue()));
                }

                gameData.getItemByName("Water")
                        .ifPresent(item -> pb.addInputItem(item, Long.MAX_VALUE));

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
                            .ifPresent(item -> pb.addInputItem(item, entry.getValue()));
                }

                gameData.getItemByName("Water")
                        .ifPresent(item -> pb.addInputItem(item, Long.MAX_VALUE));

                pb.addRecipes(gameData.getRecipes());

                planWithAllItemsAndRecipes = pb.build().createPlan();
            }
        }

        return new MultiPlan(planner, unmodifiedPlan, planWithAllItems, planWithAllRecipes, planWithAllItemsAndRecipes);
    }

    static Collection<? extends Recipe> getRecipesWeCanBuild(Collection<? extends Recipe> recipes, Collection<? extends Item> inputItems)
    {
        Set<Recipe> producable = Recipe.createSet();

        Set<Item> availableItems = Item.createSet();
        availableItems.addAll(inputItems);

        class UnbuildableRecipeSet
        {
            private final Set<Item> missingIngredients = Item.createSet();
            private final Set<Recipe> recipes = Recipe.createSet();
        }

        Collection<UnbuildableRecipeSet> unbuildableRecipeSets = new LinkedList<>();

        for (Recipe recipe : recipes){
            Set<Item> missingIngredients = Item.createSet();

            for (Recipe.RecipeItem ri : recipe.getIngredients()){
                if (!availableItems.contains(ri.getItem())){
                    missingIngredients.add(ri.getItem());
                }
            }

            Set<Item> addedItems = Item.createSet();

            if (missingIngredients.isEmpty()){
                producable.add(recipe);
                for (Recipe.RecipeItem ri : recipe.getProducts()){
                    if (availableItems.add(ri.getItem())){
                        addedItems.add(ri.getItem());
                    }
                }
            }else{
                Collection<UnbuildableRecipeSet> newRecipeSets = new LinkedList<>();

                for (Iterator<UnbuildableRecipeSet> it = unbuildableRecipeSets.iterator(); it.hasNext(); ){
                    UnbuildableRecipeSet urs = it.next();

                    boolean recipeHelps = false;
                    for (Recipe.RecipeItem ri : recipe.getProducts()){
                        if (urs.missingIngredients.contains(ri.getItem())){
                            recipeHelps = true;
                            break;
                        }
                    }

                    if (!recipeHelps){
                        continue;
                    }

                    UnbuildableRecipeSet n = new UnbuildableRecipeSet();
                    n.recipes.addAll(urs.recipes);
                    n.recipes.add(recipe);

                    n.missingIngredients.addAll(urs.missingIngredients);

                    for (Recipe.RecipeItem ri : recipe.getProducts()){
                        n.missingIngredients.remove(ri.getItem());
                    }

                    Set<Item> copyOfMissingIngredients = Item.createSet();
                    copyOfMissingIngredients.addAll(missingIngredients);

                    for (Recipe otherRecipe : urs.recipes){
                        for (Recipe.RecipeItem ri : otherRecipe.getProducts()){
                            copyOfMissingIngredients.remove(ri.getItem());
                        }
                    }

                    n.missingIngredients.addAll(copyOfMissingIngredients);

                    if (n.missingIngredients.isEmpty()){
                        it.remove();
                        producable.addAll(n.recipes);
                        for (Recipe r : n.recipes){
                            for (Recipe.RecipeItem ri : r.getProducts()){
                                if (availableItems.add(ri.getItem())){
                                    addedItems.add(ri.getItem());
                                }
                            }
                        }
                        break;
                    }else{
                        newRecipeSets.add(n);
                    }
                }

                if (addedItems.isEmpty()){
                    UnbuildableRecipeSet urs = new UnbuildableRecipeSet();
                    urs.recipes.add(recipe);
                    urs.missingIngredients.addAll(missingIngredients);
                    newRecipeSets.add(urs);

                    unbuildableRecipeSets.addAll(newRecipeSets);
                }
            }

            while (!addedItems.isEmpty()){
                Item item;
                {
                    Iterator<Item> it = addedItems.iterator();
                    item = it.next();
                    it.remove();
                }

                for (Iterator<UnbuildableRecipeSet> it = unbuildableRecipeSets.iterator(); it.hasNext(); ){
                    UnbuildableRecipeSet urs = it.next();

                    urs.missingIngredients.remove(item);

                    if (urs.missingIngredients.isEmpty()){
                        it.remove();
                        producable.addAll(urs.recipes);
                        for (Recipe r : urs.recipes){
                            for (Recipe.RecipeItem ri : r.getProducts()){
                                if (availableItems.add(ri.getItem())){
                                    addedItems.add(ri.getItem());
                                }
                            }
                        }
                    }
                }
            }
        }

        return producable;
    }

    private static void removeMaximizeWeights(ProductionPlanner planner, ProductionPlanner.Builder newPlanner)
    {
        newPlanner.getOptimizationTargets().remove(OptimizationTarget.MAX_OUTPUT_ITEMS);

        newPlanner.clearOutputItems();
        for (Item item : planner.getOutputItems()){
            BigFraction min = planner.getOutputItemMinimumPerMinute(item);
            if (min != null && min.signum() > 0){
                newPlanner.requireOutputItemsPerMinute(item, min);
            }
        }
    }
}
