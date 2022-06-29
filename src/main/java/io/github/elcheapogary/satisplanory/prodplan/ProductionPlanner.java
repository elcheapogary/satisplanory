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
import io.github.elcheapogary.satisplanory.prodplan.lp.Expression;
import io.github.elcheapogary.satisplanory.prodplan.lp.InfeasibleSolutionException;
import io.github.elcheapogary.satisplanory.prodplan.lp.Model;
import io.github.elcheapogary.satisplanory.prodplan.lp.OptimizationResult;
import io.github.elcheapogary.satisplanory.prodplan.lp.UnboundedSolutionException;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class ProductionPlanner
{
    private final Map<Item, OutputRequirement> outputRequirements;
    private final Map<Item, BigFraction> inputItems;
    private final Set<Recipe> recipes;
    private final boolean strictMaximizeRatios;
    private final List<OptimizationTarget> optimizationTargets;

    protected ProductionPlanner(Builder builder)
    {
        this.inputItems = Collections.unmodifiableMap(Item.createMap(builder.inputItems));
        this.outputRequirements = Collections.unmodifiableMap(Item.createMap(builder.outputRequirements));
        this.recipes = Collections.unmodifiableSet(Recipe.createSet(builder.recipes));
        this.strictMaximizeRatios = builder.strictMaximizeRatios;
        this.optimizationTargets = List.copyOf(builder.optimizationTargets);
    }

    private static void filterRecipesAndItems(Collection<? extends Recipe> recipes, Collection<? extends Item> inputItems, Collection<? extends Item> outputItems, Collection<? super Recipe> filteredRecipes, Collection<? super Item> filteredItems)
    {
        recipes = ProdPlanUtils.getRecipesWeCanBuild(
                getRecipesThatBuildShitWeNeed(recipes, outputItems),
                inputItems
        );

        filteredRecipes.addAll(recipes);

        filteredItems.addAll(inputItems);
        filteredItems.addAll(outputItems);
        filteredItems.addAll(ProdPlanUtils.getItemsUsedInRecipes(recipes));
    }

    private static Collection<? extends Recipe> getRecipesThatBuildShitWeNeed(Collection<? extends Recipe> recipes, Collection<? extends Item> shitWeNeed)
    {
        Set<Item> seenItems = Item.createSet();
        seenItems.addAll(shitWeNeed);

        List<Item> todoItems = new LinkedList<>(shitWeNeed);

        Set<Recipe> unaddedRecipes = Recipe.createSet(recipes);
        Set<Recipe> retv = Recipe.createSet();

        while (!todoItems.isEmpty()){
            Item item = todoItems.remove(0);

            for (Iterator<Recipe> it = unaddedRecipes.iterator(); it.hasNext(); ){
                Recipe recipe = it.next();

                if (recipe.producesItem(item)){
                    it.remove();

                    retv.add(recipe);

                    for (Recipe.RecipeItem ri : recipe.getIngredients()){
                        if (seenItems.add(ri.getItem())){
                            todoItems.add(ri.getItem());
                        }
                    }
                }
            }
        }

        return retv;
    }

    private static <K> Map<K, BigFraction> getVariableValues(Map<K, Expression> variableMap, Supplier<Map<K, BigFraction>> mapFactory, OptimizationResult result)
    {
        Map<K, BigFraction> outputMap = mapFactory.get();

        for (Map.Entry<K, Expression> entry : variableMap.entrySet()){
            K key = entry.getKey();
            Expression variable = entry.getValue();
            BigFraction amount = result.getFractionValue(variable);
            if (amount.signum() > 0){
                outputMap.put(key, amount);
            }
        }

        return outputMap;
    }

    public ProductionPlan createPlan()
            throws ProductionPlanNotFeatisbleException, InterruptedException, ProductionPlanInternalException
    {
        Set<Recipe> recipes = Recipe.createSet();
        Set<Item> items = Item.createSet();

        filterRecipesAndItems(this.recipes, inputItems.keySet(), outputRequirements.keySet(), recipes, items);

        items.addAll(outputRequirements.keySet());

        Model model = new Model();

        Map<Item, Expression> itemOutputMap = Item.createMap();
        Map<Item, Expression> itemInputMap = Item.createMap();
        Map<Recipe, Expression> recipeMap = Recipe.createMap();

        {
            /*
             * This expression is non-negative because it is non-negative for recipe variables that are all non-negative
             */
            Map<Item, Expression> itemsProducedExpressionMap = Item.createMap();
            /*
             * This expression is non-negative because it is non-negative for recipe variables that are all non-negative
             */
            Map<Item, Expression> itemsConsumedExpressionMap = Item.createMap();

            for (Recipe recipe : recipes){
                Expression recipeVariable = model.addVariable("Recipe: " + recipe.getName());
                recipeMap.put(recipe, recipeVariable);

                for (Recipe.RecipeItem ri : recipe.getIngredients()){
                    itemsConsumedExpressionMap.compute(ri.getItem(), (item, expression) ->
                            Objects.requireNonNullElse(expression, Expression.zero())
                                    .add(recipeVariable.multiply(ri.getAmount().getAmountPerMinuteFraction()))
                    );
                }

                for (Recipe.RecipeItem ri : recipe.getProducts()){
                    itemsProducedExpressionMap.compute(ri.getItem(), (item, expression) ->
                            Objects.requireNonNullElse(expression, Expression.zero())
                                    .add(recipeVariable.multiply(ri.getAmount().getAmountPerMinuteFraction()))
                    );
                }
            }

            for (Item item : items){
                Expression consumed = Objects.requireNonNullElse(itemsConsumedExpressionMap.get(item), Expression.zero());
                Expression produced = Objects.requireNonNullElse(itemsProducedExpressionMap.get(item), Expression.zero());

                Expression input = Expression.zero();

                {
                    BigFraction inputAmountPerMinute = inputItems.get(item);
                    if (inputAmountPerMinute != null){
                        input = model.addVariable("Input: " + item.getName());
                        itemInputMap.put(item, input);
                        model.addConstraint(input.lte(inputAmountPerMinute));
                    }
                }

                Expression output = model.addVariable("Output: " + item.getName());
                itemOutputMap.put(item, output);

                model.addConstraint(output.subtract(input).eq(produced.subtract(consumed)));
            }
        }

        Map<Item, Expression> itemSurplusMap = Item.createMap();
        Map<Item, BigFraction> itemMaximizeWeightsMap = Item.createMap();

        for (Item item : items){
            BigFraction min = BigFraction.zero();
            BigFraction weight = null;

            OutputRequirement outputRequirement = outputRequirements.get(item);
            if (outputRequirement != null){
                min = Objects.requireNonNullElse(outputRequirement.itemsPerMinute(), min);
                weight = outputRequirement.maximizeWeight();
                if (weight != null && weight.signum() > 0){
                    itemMaximizeWeightsMap.put(item, weight);
                }
            }

            Expression itemSurplus;

            if (min.signum() == 0){
                itemSurplus = itemOutputMap.get(item);
            }else{
                itemSurplus = model.addVariable("Surplus: " + item.getName());
                model.addConstraint(itemSurplus.eq(itemOutputMap.get(item).subtract(min)));
            }

            itemSurplusMap.put(item, itemSurplus);
        }

        List<Expression> objectiveFunctions = new LinkedList<>();
        {
            OptimizationModel om = new OptimizationModel.Builder()
                    .setItemInputMap(itemInputMap)
                    .setItemMaximizeWeightMap(itemMaximizeWeightsMap)
                    .setItemOutputMap(itemOutputMap)
                    .setItemSurplusMap(itemSurplusMap)
                    .setLpModel(model)
                    .setRecipeMap(recipeMap)
                    .build();

            for (OptimizationTarget t : optimizationTargets){
                objectiveFunctions.addAll(t.getObjectiveFunctions(om));
            }
        }

        OptimizationResult result;

        try {
            result = model.maximize(objectiveFunctions);
        }catch (InfeasibleSolutionException e){
            throw new ProductionPlanNotFeatisbleException(e);
        }catch (UnboundedSolutionException e){
            throw new ProductionPlanInternalException(e);
        }

        return new ProductionPlan(
                getVariableValues(recipeMap, Recipe::createMap, result),
                getVariableValues(itemInputMap, Item::createMap, result),
                getVariableValues(itemOutputMap, Item::createMap, result)
        );
    }

    public Map<Item, BigFraction> getInputItems()
    {
        return inputItems;
    }

    public BigFraction getOutputItemMaximizeWeight(Item item)
    {
        return outputRequirements.get(item).maximizeWeight();
    }

    public BigFraction getOutputItemMinimumPerMinute(Item item)
    {
        return outputRequirements.get(item).itemsPerMinute();
    }

    public Collection<? extends Item> getOutputItems()
    {
        return outputRequirements.keySet();
    }

    public Set<Recipe> getRecipes()
    {
        return recipes;
    }

    public Builder toBuilder()
    {
        return new Builder(this);
    }

    public static class Builder
    {
        private final Map<Item, OutputRequirement> outputRequirements = Item.createMap();
        private final Map<Item, BigFraction> inputItems = Item.createMap();
        private final Set<Recipe> recipes = Recipe.createSet();
        private final List<OptimizationTarget> optimizationTargets = new LinkedList<>();
        private boolean strictMaximizeRatios = false;

        public Builder()
        {
        }

        public Builder(ProductionPlanner planner)
        {
            this.outputRequirements.putAll(planner.outputRequirements);
            this.inputItems.putAll(planner.inputItems);
            this.recipes.addAll(planner.recipes);
            this.strictMaximizeRatios = planner.strictMaximizeRatios;
            this.optimizationTargets.addAll(planner.optimizationTargets);
        }

        public Builder addInputItem(Item item, long itemsPerMinute)
        {
            return addInputItem(item, BigDecimal.valueOf(itemsPerMinute));
        }

        public Builder addInputItem(Item item, BigDecimal itemsPerMinute)
        {
            return addInputItem(item, BigFraction.valueOf(itemsPerMinute));
        }

        public Builder addInputItem(Item item, BigFraction itemsPerMinute)
        {
            inputItems.compute(item, (notused, amount) -> Objects.requireNonNullElse(amount, BigFraction.zero()).add(itemsPerMinute));
            return this;
        }

        public Builder addOptimizationTarget(OptimizationTarget target)
        {
            this.optimizationTargets.add(target);
            return this;
        }

        public Builder addOptimizationTargets(Collection<? extends OptimizationTarget> targets)
        {
            this.optimizationTargets.addAll(targets);
            return this;
        }

        public Builder addOutputItem(Item item, BigDecimal itemsPerMinute, BigDecimal weight)
        {
            return addOutputItem(
                    item,
                    Optional.ofNullable(itemsPerMinute)
                            .map(BigFraction::valueOf)
                            .orElse(BigFraction.zero()),
                    Optional.ofNullable(weight)
                            .map(BigFraction::valueOf)
                            .orElse(BigFraction.zero())
            );
        }

        public Builder addOutputItem(Item item, BigFraction itemsPerMinute, BigFraction weight)
        {
            outputRequirements.compute(item, (item1, existing) -> new OutputRequirement(
                    Optional.ofNullable(existing)
                            .map(OutputRequirement::itemsPerMinute)
                            .orElse(BigFraction.zero())
                            .add(Objects.requireNonNullElse(itemsPerMinute, BigFraction.zero())),
                    Optional.ofNullable(existing)
                            .map(OutputRequirement::maximizeWeight)
                            .orElse(BigFraction.zero())
                            .max(Objects.requireNonNullElse(weight, BigFraction.zero()))
            ));
            return this;
        }

        public Builder addRecipe(Recipe recipe)
        {
            recipes.add(recipe);
            return this;
        }

        public Builder addRecipes(Collection<? extends Recipe> recipes)
        {
            this.recipes.addAll(recipes);
            return this;
        }

        public ProductionPlanner build()
        {
            return new ProductionPlanner(this);
        }

        public Builder clearOptimizationTargets()
        {
            optimizationTargets.clear();
            return this;
        }

        public Builder clearOutputItems()
        {
            this.outputRequirements.clear();
            return this;
        }

        public List<OptimizationTarget> getOptimizationTargets()
        {
            return optimizationTargets;
        }

        public Builder maximizeOutputItem(Item item, long weight)
        {
            return maximizeOutputItem(item, BigFraction.valueOf(weight));
        }

        public Builder maximizeOutputItem(Item item, BigDecimal weight)
        {
            return addOutputItem(item, BigDecimal.ZERO, weight);
        }

        public Builder maximizeOutputItem(Item item, BigFraction weight)
        {
            return addOutputItem(item, BigFraction.zero(), weight);
        }

        public Builder requireOutputItemsPerMinute(Item item, BigDecimal itemsPerMinute)
        {
            return addOutputItem(item, itemsPerMinute, BigDecimal.ZERO);
        }

        public Builder requireOutputItemsPerMinute(Item item, BigFraction itemsPerMinute)
        {
            return addOutputItem(item, itemsPerMinute, BigFraction.zero());
        }

        public Builder requireOutputItemsPerMinute(Item item, long itemsPerMinute)
        {
            return requireOutputItemsPerMinute(item, BigFraction.valueOf(itemsPerMinute));
        }

        public Builder setStrictMaximizeRatios(boolean strictMaximizeRatios)
        {
            this.strictMaximizeRatios = strictMaximizeRatios;
            return this;
        }
    }

    private record OutputRequirement(BigFraction itemsPerMinute, BigFraction maximizeWeight)
    {
    }
}
