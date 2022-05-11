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
import io.github.elcheapogary.satisplanory.prodplan.lp.InfeasibleSolutionException;
import io.github.elcheapogary.satisplanory.prodplan.lp.Model;
import io.github.elcheapogary.satisplanory.prodplan.lp.OptimizationResult;
import io.github.elcheapogary.satisplanory.prodplan.lp.UnboundedSolutionException;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.math.BigDecimal;
import java.util.ArrayList;
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
    private final Map<Item, BigDecimal> inputItems;
    private final Set<Recipe> recipes;
    private final boolean strictMaximizeRatios;
    private final BigFraction maximizeOutputItemsWeight;
    private final BigFraction powerWeight;
    private final BigFraction minimizeInputItemWeight;
    private final BigFraction balanceWeight;
    private final BigFraction maximizeInputItemWeight;
    private final BigFraction minimizeSurplusWeight;

    protected ProductionPlanner(Builder builder)
    {
        this.inputItems = Collections.unmodifiableMap(Item.createMap(builder.inputItems));
        this.outputRequirements = Collections.unmodifiableMap(Item.createMap(builder.outputRequirements));
        this.recipes = Collections.unmodifiableSet(Recipe.createSet(builder.recipes));
        this.strictMaximizeRatios = builder.strictMaximizeRatios;
        this.maximizeOutputItemsWeight = Objects.requireNonNull(builder.maximizeOutputItemWeight);
        this.powerWeight = Objects.requireNonNull(builder.powerWeight);
        this.minimizeInputItemWeight = Objects.requireNonNull(builder.minimizeInputItemWeight);
        this.balanceWeight = Objects.requireNonNull(builder.balanceWeight);
        this.maximizeInputItemWeight = Objects.requireNonNull(builder.maximizeInputItemsWeight);
        this.minimizeSurplusWeight = Objects.requireNonNull(builder.minimizeSurplusWeight);
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

    private static <K> Map<K, BigFraction> getVariableValues(Map<K, FractionExpression> variableMap, Supplier<Map<K, BigFraction>> mapFactory, OptimizationResult result)
    {
        Map<K, BigFraction> outputMap = mapFactory.get();

        for (Map.Entry<K, FractionExpression> entry : variableMap.entrySet()){
            K key = entry.getKey();
            FractionExpression variable = entry.getValue();
            BigFraction amount = variable.getValue(result);
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

        Map<Item, FractionExpression> itemOutputVariableMap = Item.createMap();
        Map<Item, FractionExpression> itemInputVariableMap = Item.createMap();
        Map<Recipe, FractionExpression> recipeVariableMap = Recipe.createMap();
        FractionExpression powerConsumedExpression = FractionExpression.zero();

        {
            /*
             * This expression is non-negative because it is non-negative for recipe variables that are all non-negative
             */
            Map<Item, FractionExpression> itemsProducedExpressionMap = Item.createMap();
            /*
             * This expression is non-negative because it is non-negative for recipe variables that are all non-negative
             */
            Map<Item, FractionExpression> itemsConsumedExpressionMap = Item.createMap();

            for (Recipe recipe : recipes){
                FractionExpression recipeVariable = model.addFractionVariable();
                model.addConstraint(recipeVariable.nonNegative());
                recipeVariableMap.put(recipe, recipeVariable);

                powerConsumedExpression = powerConsumedExpression.add(recipe.getPowerConsumption(), recipeVariable);

                for (Recipe.RecipeItem ri : recipe.getIngredients()){
                    itemsConsumedExpressionMap.compute(ri.getItem(), (item, expression) ->
                            Objects.requireNonNullElse(expression, FractionExpression.zero())
                                    .add(ri.getAmount().getAmountPerMinuteFraction(), recipeVariable)
                    );
                }

                for (Recipe.RecipeItem ri : recipe.getProducts()){
                    itemsProducedExpressionMap.compute(ri.getItem(), (item, expression) ->
                            Objects.requireNonNullElse(expression, FractionExpression.zero())
                                    .add(ri.getAmount().getAmountPerMinuteFraction(), recipeVariable)
                    );
                }
            }

            for (Item item : items){
                FractionExpression consumed = Objects.requireNonNullElse(itemsConsumedExpressionMap.get(item), FractionExpression.zero());
                FractionExpression produced = Objects.requireNonNullElse(itemsProducedExpressionMap.get(item), FractionExpression.zero());

                FractionExpression inputVariable = FractionExpression.zero();

                {
                    BigDecimal inputAmountPerMinute = inputItems.get(item);
                    if (inputAmountPerMinute != null){
                        inputVariable = model.addFractionVariable();
                        itemInputVariableMap.put(item, inputVariable);
                        model.addConstraint(inputVariable.nonNegative());
                        model.addConstraint(inputVariable.lte(inputAmountPerMinute));
                        model.addConstraint(inputVariable.eq(consumed.subtract(produced)));
                    }
                }

                FractionExpression outputExpression = produced.subtract(consumed.subtract(inputVariable));
                itemOutputVariableMap.put(item, outputExpression);

                {
                    OutputRequirement outputRequirement = outputRequirements.get(item);
                    if (outputRequirement != null && outputRequirement.getItemsPerMinute() != null){
                        model.addConstraint(outputExpression.gte(outputRequirement.getItemsPerMinute()));
                    }else{
                        model.addConstraint(outputExpression.nonNegative());
                    }
                }
            }
        }

        FractionExpression maximizedOutputItemsExpression = FractionExpression.zero();
        FractionExpression surplusExpression = FractionExpression.zero();
        FractionExpression balanceExpression = FractionExpression.zero();

        {
            Map<Item, BigDecimal> outputItemWeights = Item.createMap();
            for (Item item : items){
                BigDecimal min = BigDecimal.ZERO;
                BigDecimal weight = null;

                OutputRequirement outputRequirement = outputRequirements.get(item);
                if (outputRequirement != null){
                    min = outputRequirement.getItemsPerMinute();
                    weight = outputRequirement.getMaximizeWeight();
                    if (weight != null && weight.signum() > 0){
                        outputItemWeights.put(item, weight);
                    }
                }

                FractionExpression itemSurplus;

                if (min.signum() == 0){
                    itemSurplus = itemOutputVariableMap.get(item);
                }else{
                    itemSurplus = model.addFractionVariable();
                    model.addConstraint(itemSurplus.eq(itemOutputVariableMap.get(item).subtract(min)));
                }

                if (weight == null || weight.signum() == 0){
                    surplusExpression = surplusExpression.add(item.toDisplayAmount(BigFraction.ONE), itemSurplus);
                }else{
                    maximizedOutputItemsExpression = maximizedOutputItemsExpression.add(item.toDisplayAmount(weight), itemSurplus);
                }
            }

            if (outputItemWeights.size() > 1){
                if (strictMaximizeRatios){
                    FractionExpression balanceVariable = model.addFractionVariable();
                    model.addConstraint(balanceVariable.nonNegative());
                    balanceExpression = balanceVariable;

                    for (var entry : outputItemWeights.entrySet()){
                        Item item = entry.getKey();
                        BigDecimal weight = entry.getValue();

                        model.addConstraint(itemOutputVariableMap.get(item).eq(balanceVariable.multiply(item.fromDisplayAmount(weight))));
                    }
                }else{
                    List<Item> maxItems = new ArrayList<>(outputItemWeights.keySet());
                    for (int i = 0; i < maxItems.size() - 1; i++){
                        Item a = maxItems.get(i);
                        for (int j = i + 1; j < maxItems.size(); j++){
                            Item b = maxItems.get(j);

                            FractionExpression balanceVariable = model.addFractionVariable();
                            balanceExpression = balanceExpression.add(balanceVariable);
                            model.addConstraint(balanceVariable.nonNegative());
                            model.addConstraint(itemOutputVariableMap.get(a).gte(balanceVariable.multiply(a.fromDisplayAmount(outputItemWeights.get(a)))));
                            model.addConstraint(itemOutputVariableMap.get(b).gte(balanceVariable.multiply(b.fromDisplayAmount(outputItemWeights.get(b)))));
                        }
                    }
                }
            }
        }

        FractionExpression inputItemsExpression = FractionExpression.zero();

        for (var entry : itemInputVariableMap.entrySet()){
            Item item = entry.getKey();
            FractionExpression expression = entry.getValue();
            inputItemsExpression = inputItemsExpression.add(item.toDisplayAmount(BigFraction.ONE), expression);
        }

        FractionExpression objectiveFunction = FractionExpression.zero()
                .add(maximizedOutputItemsExpression.multiply(maximizeOutputItemsWeight))
                .add(balanceExpression.multiply(balanceWeight))
                .add(inputItemsExpression.multiply(maximizeInputItemWeight))
                .subtract(inputItemsExpression.multiply(minimizeInputItemWeight))
                .subtract(powerConsumedExpression.multiply(powerWeight))
                .subtract(surplusExpression.multiply(minimizeSurplusWeight));

        OptimizationResult result;

        try {
            result = model.maximize(objectiveFunction);
        }catch (InfeasibleSolutionException e){
            throw new ProductionPlanNotFeatisbleException(e);
        }catch (UnboundedSolutionException e){
            throw new ProductionPlanInternalException(e);
        }

        return new ProductionPlan(
                getVariableValues(recipeVariableMap, Recipe::createMap, result),
                getVariableValues(itemInputVariableMap, Item::createMap, result),
                getVariableValues(itemOutputVariableMap, Item::createMap, result)
        );
    }

    public Map<Item, BigDecimal> getInputItems()
    {
        return inputItems;
    }

    public BigDecimal getOutputItemMaximizeWeight(Item item)
    {
        return outputRequirements.get(item).getMaximizeWeight();
    }

    public BigDecimal getOutputItemMinimumPerMinute(Item item)
    {
        return outputRequirements.get(item).getItemsPerMinute();
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
        private final Map<Item, BigDecimal> inputItems = Item.createMap();
        private final Set<Recipe> recipes = Recipe.createSet();
        private boolean strictMaximizeRatios = false;
        private BigFraction maximizeOutputItemWeight = BigFraction.ONE;
        private BigFraction powerWeight = BigFraction.ZERO;
        private BigFraction minimizeInputItemWeight = BigFraction.ZERO;
        private BigFraction balanceWeight = BigFraction.ONE.movePointRight(3);
        private BigFraction maximizeInputItemsWeight = BigFraction.ZERO;
        private BigFraction minimizeSurplusWeight = BigFraction.ZERO;

        public Builder()
        {
        }

        public Builder(ProductionPlanner planner)
        {
            this.outputRequirements.putAll(planner.outputRequirements);
            this.inputItems.putAll(planner.inputItems);
            this.recipes.addAll(planner.recipes);
            this.strictMaximizeRatios = planner.strictMaximizeRatios;
            this.maximizeOutputItemWeight = planner.maximizeOutputItemsWeight;
            this.powerWeight = planner.powerWeight;
            this.minimizeInputItemWeight = planner.minimizeInputItemWeight;
            this.balanceWeight = planner.balanceWeight;
            this.maximizeInputItemsWeight = planner.maximizeInputItemWeight;
            this.minimizeSurplusWeight = planner.minimizeSurplusWeight;
        }

        public Builder addInputItem(Item item, long itemsPerMinute)
        {
            return addInputItem(item, BigDecimal.valueOf(itemsPerMinute));
        }

        public Builder addInputItem(Item item, BigDecimal itemsPerMinute)
        {
            inputItems.compute(item, (notused, amount) -> Objects.requireNonNullElse(amount, BigDecimal.ZERO).add(itemsPerMinute));
            return this;
        }

        public Builder addOutputItem(Item item, BigDecimal itemsPerMinute, BigDecimal weight)
        {
            outputRequirements.put(item, new OutputRequirement(itemsPerMinute, weight));
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

        public Builder maximizeOutputItem(Item item, long weight)
        {
            return addOutputItem(item, null, BigDecimal.valueOf(weight));
        }

        public Builder maximizeOutputItem(Item item, BigDecimal weight)
        {
            outputRequirements.compute(item, (item1, outputRequirement) -> {
                Optional<OutputRequirement> o = Optional.ofNullable(outputRequirement);

                return new OutputRequirement(
                        o.map(OutputRequirement::getItemsPerMinute).orElse(null),
                        o.map(OutputRequirement::getMaximizeWeight).orElse(BigDecimal.ZERO)
                                .max(weight)
                );
            });
            return this;
        }

        public Builder requireOutputItemsPerMinute(Item item, BigDecimal itemsPerMinute)
        {
            outputRequirements.compute(item, (item1, outputRequirement) -> {
                Optional<OutputRequirement> o = Optional.ofNullable(outputRequirement);

                return new OutputRequirement(
                        o.map(OutputRequirement::getItemsPerMinute).orElse(BigDecimal.ZERO)
                                .add(itemsPerMinute),
                        o.map(OutputRequirement::getMaximizeWeight).orElse(null)
                );
            });
            return this;
        }

        public Builder requireOutputItemsPerMinute(Item item, long itemsPerMinute)
        {
            return requireOutputItemsPerMinute(item, BigDecimal.valueOf(itemsPerMinute));
        }

        public Builder setBalanceWeight(BigFraction balanceWeight)
        {
            this.balanceWeight = balanceWeight;
            return this;
        }

        public Builder setMaximizeInputItemsWeight(BigFraction maximizeInputItemsWeight)
        {
            this.maximizeInputItemsWeight = maximizeInputItemsWeight;
            return this;
        }

        public Builder setMaximizeOutputItemWeight(BigFraction maximizeOutputItemWeight)
        {
            this.maximizeOutputItemWeight = maximizeOutputItemWeight;
            return this;
        }

        public Builder setMinimizeInputItemWeight(BigFraction minimizeInputItemWeight)
        {
            this.minimizeInputItemWeight = minimizeInputItemWeight;
            return this;
        }

        public Builder setMinimizeSurplusWeight(BigFraction minimizeSurplusWeight)
        {
            this.minimizeSurplusWeight = minimizeSurplusWeight;
            return this;
        }

        public Builder setPowerWeight(BigFraction powerWeight)
        {
            this.powerWeight = powerWeight;
            return this;
        }

        public Builder setStrictMaximizeRatios(boolean strictMaximizeRatios)
        {
            this.strictMaximizeRatios = strictMaximizeRatios;
            return this;
        }
    }

    private static class OutputRequirement
    {
        private final BigDecimal itemsPerMinute;
        private final BigDecimal maximizeWeight;

        public OutputRequirement(BigDecimal itemsPerMinute, BigDecimal maximizeWeight)
        {
            this.itemsPerMinute = itemsPerMinute;
            this.maximizeWeight = maximizeWeight;
        }

        public BigDecimal getItemsPerMinute()
        {
            return itemsPerMinute;
        }

        public BigDecimal getMaximizeWeight()
        {
            return maximizeWeight;
        }
    }
}
