/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.gamedata;

import io.github.elcheapogary.satisplanory.util.BigFraction;
import io.github.elcheapogary.satisplanory.util.Pair;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class Recipe
{
    private static final Comparator<? super Recipe> COMPARATOR = Comparator.comparing(Recipe::getName);
    private final String name;
    private final String className;
    private final Manufacturer manufacturer;
    private final BigDecimal cycleTimeSeconds;
    private final BigDecimal variablePowerConstant;
    private final BigDecimal variablePowerFactor;
    private final List<RecipeItem> ingredients;
    private final List<RecipeItem> productsNew;

    protected Recipe(Builder builder)
    {
        this.name = Objects.requireNonNull(builder.name, "name required");
        this.className = Objects.requireNonNull(builder.className, "className");
        this.manufacturer = Objects.requireNonNull(builder.manufacturer, "producedInBuilding required");
        this.cycleTimeSeconds = Objects.requireNonNull(builder.cycleTimeSeconds, "cycleTimeSeconds required");
        this.variablePowerConstant = Objects.requireNonNull(builder.variablePowerConstant);
        this.variablePowerFactor = Objects.requireNonNull(builder.variablePowerFactor);

        if (builder.ingredients.isEmpty()){
            throw new IllegalArgumentException("Recipe has no ingredients");
        }

        {
            List<RecipeItem> items = new ArrayList<>(builder.ingredients.size());

            for (var p : builder.ingredients){
                items.add(new RecipeItem(p.key(), p.value()));
            }

            this.ingredients = Collections.unmodifiableList(items);
        }

        if (builder.products.isEmpty()){
            throw new IllegalArgumentException("Recipe has no products");
        }

        {
            List<RecipeItem> items = new ArrayList<>(builder.products.size());

            for (var p : builder.products){
                items.add(new RecipeItem(p.key(), p.value()));
            }

            this.productsNew = Collections.unmodifiableList(items);
        }
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

    public static Recipe fromJson(JsonObject json, Function<? super String, ? extends Manufacturer> manufacturerResolver, Function<? super String, ? extends Item> itemResolver)
    {
        Recipe.Builder recipeBuilder = new Builder()
                .setName(json.getString("name"))
                .setClassName(json.getString("className"))
                .setManufacturer(manufacturerResolver.apply(json.getString("manufacturerClass")))
                .setCycleTimeSeconds(new BigDecimal(json.getString("cycleTimeSeconds")))
                .setVariablePowerConstant(new BigDecimal(json.getString("variablePowerConstant")))
                .setVariablePowerFactor(new BigDecimal(json.getString("variablePowerFactor")));

        for (JsonObject jsonIngredient : json.getJsonArray("ingredients").getValuesAs(JsonObject.class)){
            Item item = itemResolver.apply(jsonIngredient.getString("itemClass"));
            recipeBuilder.addIngredient(item, new BigDecimal(jsonIngredient.getString("amountPerCycle")));
        }

        for (JsonObject jsonProduct : json.getJsonArray("products").getValuesAs(JsonObject.class)){
            Item item = itemResolver.apply(jsonProduct.getString("itemClass"));
            recipeBuilder.addProduct(item, new BigDecimal(jsonProduct.getString("amountPerCycle")));
        }

        return recipeBuilder.build();
    }

    public boolean consumesItem(Item item)
    {
        for (RecipeItem ri : ingredients){
            if (ri.getItem().equals(item)){
                return true;
            }
        }

        return false;
    }

    public String getClassName()
    {
        return className;
    }

    public BigDecimal getCycleTimeSeconds()
    {
        return cycleTimeSeconds;
    }

    public BigFraction getCyclesPerMinute()
    {
        return BigFraction.valueOf(60).divide(BigFraction.valueOf(cycleTimeSeconds));
    }

    public List<RecipeItem> getIngredients()
    {
        return ingredients;
    }

    public Manufacturer getManufacturer()
    {
        return manufacturer;
    }

    public String getName()
    {
        return name;
    }

    public BigDecimal getPowerConsumption()
    {
        if (variablePowerFactor.compareTo(BigDecimal.ONE) == 0){
            return getManufacturer().getPowerConsumption();
        }else{
            return variablePowerConstant.add(variablePowerFactor.divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP));
        }
    }

    public RecipeItem getPrimaryProduct()
    {
        return productsNew.get(0);
    }

    public List<RecipeItem> getProducts()
    {
        return productsNew;
    }

    public boolean isAlternateRecipe()
    {
        return getName().startsWith("Alternate:");
    }

    public boolean producesItem(Item item)
    {
        for (RecipeItem ri : productsNew){
            if (ri.getItem().equals(item)){
                return true;
            }
        }
        return false;
    }

    public JsonObject toJson()
    {
        JsonObjectBuilder jsonRecipe = Json.createObjectBuilder();

        jsonRecipe = jsonRecipe.add("name", name)
                .add("className", className)
                .add("manufacturerClass", manufacturer.getClassName())
                .add("cycleTimeSeconds", cycleTimeSeconds.toString())
                .add("variablePowerConstant", variablePowerConstant.toString())
                .add("variablePowerFactor", variablePowerFactor.toString());

        {
            JsonArrayBuilder jsonIngredients = Json.createArrayBuilder();

            for (RecipeItem ri : ingredients){
                jsonIngredients.add(Json.createObjectBuilder()
                        .add("itemClass", ri.getItem().getClassName())
                        .add("amountPerCycle", ri.getAmountPerCycle().toString())
                        .build());
            }

            jsonRecipe.add("ingredients", jsonIngredients.build());
        }

        {
            JsonArrayBuilder jsonProducts = Json.createArrayBuilder();

            for (RecipeItem ri : productsNew){
                jsonProducts.add(Json.createObjectBuilder()
                        .add("itemClass", ri.getItem().getClassName())
                        .add("amountPerCycle", ri.getAmountPerCycle().toString())
                        .build());
            }

            jsonRecipe.add("products", jsonProducts.build());
        }

        return jsonRecipe.build();
    }

    public static class Builder
    {
        private final List<Pair<Item, BigDecimal>> ingredients = new LinkedList<>();
        private final List<Pair<Item, BigDecimal>> products = new LinkedList<>();
        private String name;
        private String className;
        private Manufacturer manufacturer;
        private BigDecimal cycleTimeSeconds;
        private BigDecimal variablePowerConstant;
        private BigDecimal variablePowerFactor;

        public Builder addIngredient(Item item, BigDecimal amountPerCycle)
        {
            this.ingredients.add(new Pair<>(item, amountPerCycle));
            return this;
        }

        public Builder addProduct(Item item, BigDecimal amountPerCycle)
        {
            this.products.add(new Pair<>(item, amountPerCycle));
            return this;
        }

        public Recipe build()
        {
            return new Recipe(this);
        }

        public Builder setClassName(String className)
        {
            this.className = className;
            return this;
        }

        public Builder setCycleTimeSeconds(BigDecimal cycleTimeSeconds)
        {
            this.cycleTimeSeconds = cycleTimeSeconds;
            return this;
        }

        public Builder setManufacturer(Manufacturer manufacturer)
        {
            this.manufacturer = manufacturer;
            return this;
        }

        public Builder setName(String name)
        {
            this.name = name;
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

    public class RecipeItem
    {
        private final Item item;
        private final BigDecimal amountPerCycle;

        public RecipeItem(Item item, BigDecimal amountPerCycle)
        {
            this.item = item;
            this.amountPerCycle = amountPerCycle;
        }

        public BigDecimal getAmountPerCycle()
        {
            return amountPerCycle;
        }

        public BigFraction getAmountPerMinute()
        {
            return BigFraction.valueOf(amountPerCycle).multiply(getCyclesPerMinute());
        }

        public Item getItem()
        {
            return item;
        }
    }
}
