/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.model;

import io.github.elcheapogary.satisplanory.util.BigDecimalUtils;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Recipe
{
    private static final Comparator<? super Recipe> COMPARATOR = Comparator.comparing(Recipe::getName);
    private final String name;
    private final Building producedInBuilding;
    private final BigDecimal cycleTimeSeconds;
    private final Map<Item, Integer> ingredients;
    private final Map<Item, Integer> products;
    private final BigDecimal variablePowerConstant;
    private final BigDecimal variablePowerFactor;

    protected Recipe(Builder builder)
    {
        this.name = Objects.requireNonNull(builder.name, "name required");
        this.producedInBuilding = Objects.requireNonNull(builder.producedInBuilding, "producedInBuilding required");
        this.cycleTimeSeconds = Objects.requireNonNull(builder.cycleTimeSeconds, "cycleTimeSeconds required");
        this.variablePowerConstant = Objects.requireNonNull(builder.variablePowerConstant);
        this.variablePowerFactor = Objects.requireNonNull(builder.variablePowerFactor);

        if (builder.ingredients.isEmpty()){
            throw new IllegalArgumentException("Recipe has no ingredients");
        }
        this.ingredients = Collections.unmodifiableMap(Item.createMap(builder.ingredients));

        if (builder.products.isEmpty()){
            throw new IllegalArgumentException("Recipe has no products");
        }
        this.products = Collections.unmodifiableMap(Item.createMap(builder.products));
    }

    public static <V> Map<Recipe, V> createMap()
    {
        return new TreeMap<>(COMPARATOR);
    }

    public static Set<Recipe> createSet(Collection<? extends Recipe> recipes)
    {
        Set<Recipe> retv = new TreeSet<>(COMPARATOR);
        retv.addAll(recipes);
        return retv;
    }

    public static Set<Recipe> createSet()
    {
        return new TreeSet<>(COMPARATOR);
    }

    public boolean consumesItem(Item item)
    {
        return ingredients.get(item) != null;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recipe recipe = (Recipe) o;
        return name.equals(recipe.name) && producedInBuilding.equals(recipe.producedInBuilding) && cycleTimeSeconds.equals(recipe.cycleTimeSeconds) && ingredients.equals(recipe.ingredients) && products.equals(recipe.products);
    }

    public BigDecimal getCycleTimeSeconds()
    {
        return cycleTimeSeconds;
    }

    public Optional<RecipeItemAmount> getIngredientAmount(Item item)
    {
        return Optional.ofNullable(ingredients.get(item))
                .map(i -> new RecipeItemAmount(cycleTimeSeconds, BigDecimal.valueOf(i)));
    }

    public Collection<RecipeItem> getIngredients()
    {
        return ingredients.entrySet().stream()
                .map(e -> new RecipeItem(e.getKey(), new RecipeItemAmount(cycleTimeSeconds, BigDecimal.valueOf(e.getValue()))))
                .toList();
    }

    public String getName()
    {
        return name;
    }

    public BigDecimal getPowerConsumption()
    {
        if (variablePowerFactor.compareTo(BigDecimal.ONE) == 0){
            return getProducedInBuilding().getPowerConsumption();
        }else{
            return variablePowerConstant.add(variablePowerFactor.divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP));
        }
    }

    public Building getProducedInBuilding()
    {
        return producedInBuilding;
    }

    public Optional<RecipeItemAmount> getProductAmount(Item item)
    {
        return Optional.ofNullable(products.get(item))
                .map(i -> new RecipeItemAmount(cycleTimeSeconds, BigDecimal.valueOf(i)));
    }

    public Collection<RecipeItem> getProducts()
    {
        return products.entrySet().stream()
                .map(e -> new RecipeItem(e.getKey(), new RecipeItemAmount(cycleTimeSeconds, BigDecimal.valueOf(e.getValue()))))
                .toList();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, producedInBuilding, cycleTimeSeconds, ingredients, products);
    }

    public boolean isAlternateRecipe()
    {
        return getName().startsWith("Alternate:");
    }

    public boolean producesItem(Item item)
    {
        return products.get(item) != null;
    }

    @Override
    public String toString()
    {
        return "Recipe{" +
                "name='" + name + '\'' +
                ", producedInBuilding=" + producedInBuilding +
                ", cycleTimeSeconds=" + cycleTimeSeconds +
                ", ingredients=" + ingredients +
                ", products=" + products +
                '}';
    }

    public static class Builder
    {
        private final Map<Item, Integer> ingredients = Item.createMap();
        private final Map<Item, Integer> products = Item.createMap();
        private String name;
        private Building producedInBuilding;
        private BigDecimal cycleTimeSeconds;
        private BigDecimal variablePowerConstant;
        private BigDecimal variablePowerFactor;

        public Builder addIngredient(Item item, int amount)
        {
            this.ingredients.put(item, amount);
            return this;
        }

        public Builder addProduct(Item item, int amount)
        {
            this.products.put(item, amount);
            return this;
        }

        public Recipe build()
        {
            return new Recipe(this);
        }

        public Builder setCycleTimeSeconds(BigDecimal cycleTimeSeconds)
        {
            this.cycleTimeSeconds = cycleTimeSeconds;
            return this;
        }

        public Builder setName(String name)
        {
            this.name = name;
            return this;
        }

        public Builder setProducedInBuilding(Building producedInBuilding)
        {
            this.producedInBuilding = producedInBuilding;
            return this;
        }

        public Builder setVariablePowerConstant(BigDecimal variablePowerConstant)
        {
            this.variablePowerConstant = variablePowerConstant;
            return this;
        }

        public Builder setVariablePowerFactor(BigDecimal variablePowerFactor)
        {
            this.variablePowerFactor = variablePowerFactor;
            return this;
        }
    }

    public static class RecipeItemAmount
    {
        private final BigDecimal cycleTimeSeconds;
        private final BigDecimal amountPerCycle;

        public RecipeItemAmount(BigDecimal cycleTimeSeconds, BigDecimal amountPerCycle)
        {
            this.cycleTimeSeconds = cycleTimeSeconds;
            this.amountPerCycle = amountPerCycle;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RecipeItemAmount that = (RecipeItemAmount) o;
            return cycleTimeSeconds.equals(that.cycleTimeSeconds) && amountPerCycle.equals(that.amountPerCycle);
        }

        public BigDecimal getAmountPerCycle()
        {
            return amountPerCycle;
        }

        public BigDecimal getAmountPerMinute()
        {
            BigDecimal n = BigDecimal.valueOf(60).divide(cycleTimeSeconds, 10, RoundingMode.HALF_UP).multiply(amountPerCycle);
            return BigDecimalUtils.normalize(n);
        }

        public BigFraction getAmountPerMinuteFraction()
        {
            return BigFraction.valueOf(60).divide(BigFraction.valueOf(cycleTimeSeconds)).multiply(BigFraction.valueOf(amountPerCycle));
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(cycleTimeSeconds, amountPerCycle);
        }

        @Override
        public String toString()
        {
            return "RecipeItemAmount{" +
                    "cycleTimeSeconds=" + cycleTimeSeconds +
                    ", amountPerCycle=" + amountPerCycle +
                    '}';
        }
    }

    public static class RecipeItem
    {
        private final Item item;
        private final RecipeItemAmount amount;

        public RecipeItem(Item item, RecipeItemAmount amount)
        {
            this.item = item;
            this.amount = amount;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RecipeItem that = (RecipeItem) o;
            return item.equals(that.item) && amount.equals(that.amount);
        }

        public RecipeItemAmount getAmount()
        {
            return amount;
        }

        private BigDecimal getDisplayAmount(BigDecimal n)
        {
            return BigDecimalUtils.normalize(item.toDisplayAmount(n));
        }

        public BigDecimal getDisplayAmountPerCycle()
        {
            return getDisplayAmount(getAmount().getAmountPerCycle());
        }

        public BigDecimal getDisplayAmountPerMinute()
        {
            return getDisplayAmount(getAmount().getAmountPerMinute());
        }

        public Item getItem()
        {
            return item;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(item, amount);
        }

        @Override
        public String toString()
        {
            return "RecipeItem{" +
                    "item=" + item +
                    ", amount=" + amount +
                    '}';
        }
    }
}
