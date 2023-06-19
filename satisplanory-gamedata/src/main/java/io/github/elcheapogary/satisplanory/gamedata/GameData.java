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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParser;

public class GameData
{
    private final Map<String, Item> itemsByName;
    private final Map<String, Item> itemsByClassName;
    private final Map<String, Manufacturer> manufacturerByNameMap;
    private final Map<String, Manufacturer> manufacturerByClassMap;
    private final Map<String, Recipe> recipesByName;
    private final Map<String, Recipe> recipesByClassName;
    private final Map<Item, Long> rawResources;

    protected GameData(AbstractBuilder<?> builder)
    {
        this.itemsByName = Collections.unmodifiableMap(new TreeMap<>(builder.itemsByName));
        this.itemsByClassName = Collections.unmodifiableMap(new TreeMap<>(builder.itemsByClassName));
        {
            Map<String, Manufacturer> tmpBuildingsByName = new TreeMap<>();

            for (Manufacturer manufacturer : builder.manufacturersByClassName.values()){
                tmpBuildingsByName.put(manufacturer.getName(), manufacturer);
            }
            this.manufacturerByNameMap = Collections.unmodifiableMap(tmpBuildingsByName);
            this.manufacturerByClassMap = Collections.unmodifiableMap(new TreeMap<>(builder.manufacturersByClassName));
        }
        {
            Map<String, Recipe> tmpRecipesByName = new TreeMap<>();
            Map<String, Recipe> tmpRecipesByClassName = new TreeMap<>();

            for (Recipe r : builder.recipes){
                tmpRecipesByName.put(r.getName(), r);
                tmpRecipesByClassName.put(r.getClassName(), r);
            }

            this.recipesByName = Collections.unmodifiableMap(tmpRecipesByName);
            this.recipesByClassName = Collections.unmodifiableMap(tmpRecipesByClassName);
        }

        {
            Map<Item, Long> tmpRawResources = new TreeMap<>(Comparator.comparing(Item::getClassName));
            tmpRawResources.putAll(builder.rawResources);
            this.rawResources = Collections.unmodifiableMap(tmpRawResources);
        }
    }

    public static GameData fromJson(JsonObject json)
            throws IOException
    {
        return fromJson(json, new Builder());
    }

    public static GameData fromJson(JsonObject json, AbstractBuilder<?> builder)
            throws IOException
    {
        Map<String, Manufacturer> manufacturerByClassMap = new TreeMap<>();

        for (JsonObject jo : json.getJsonArray("manufacturers").getValuesAs(JsonObject.class)){
            Manufacturer manufacturer = Manufacturer.fromJson(jo);
            manufacturerByClassMap.put(manufacturer.getClassName(), manufacturer);
            builder.addManufacturer(manufacturer);
        }

        for (JsonObject jo : json.getJsonArray("items").getValuesAs(JsonObject.class)){
            Item item = Item.fromJson(jo);
            builder.addItem(item);
        }

        for (JsonObject jo : json.getJsonArray("recipes").getValuesAs(JsonObject.class)){
            builder.addRecipe(Recipe.fromJson(jo, manufacturerByClassMap::get, className -> builder.getItemByClassName(className).orElse(null)));
        }

        for (var entry : json.getJsonObject("rawResources").entrySet()){
            builder.addRawResources(builder.getItemByClassName(entry.getKey()).orElseThrow(() -> new IOException("Invalid raw resource class: " + entry.getKey())), ((JsonNumber)entry.getValue()).longValue());
        }

        return builder.build();
    }

    private static GameData loadFromResource(String resourceName, AbstractBuilder<?> builder)
            throws IOException
    {
        JsonObject json;

        try (JsonParser p = Json.createParser(Optional.ofNullable(GameData.class.getResourceAsStream(resourceName))
                .orElseThrow(() -> new IOException("Missing resource: " + resourceName))
        )){
            json = p.getObject();
        }

        return GameData.fromJson(json, builder);
    }

    public static GameData loadUpdate8Data()
            throws IOException
    {
        return loadUpdate8Data(new Builder());
    }

    public static GameData loadUpdate8Data(AbstractBuilder<?> builder)
            throws IOException
    {
        return loadFromResource("u8-data.json", builder);
    }

    public static GameData loadUpdate7Data()
            throws IOException
    {
        return loadUpdate7Data(new Builder());
    }

    public static GameData loadUpdate7Data(AbstractBuilder<?> builder)
            throws IOException
    {
        return loadFromResource("u7-data.json", builder);
    }

    public static GameData loadLatestGameData()
            throws IOException
    {
        return loadLatestGameData(new Builder());
    }

    public static GameData loadLatestGameData(AbstractBuilder<?> builder)
            throws IOException
    {
        return loadUpdate8Data(builder);
    }

    public Optional<Item> getItemByClassName(String itemClassName)
    {
        return Optional.ofNullable(itemsByClassName.get(itemClassName));
    }

    public Optional<Item> getItemByName(String itemName)
    {
        return Optional.ofNullable(itemsByName.get(itemName));
    }

    public Collection<Item> getItems()
    {
        return itemsByName.values();
    }

    public Optional<Manufacturer> getManufacturerByClassName(String buildingClassName)
    {
        return Optional.ofNullable(manufacturerByClassMap.get(buildingClassName));
    }

    public Optional<Manufacturer> getManufacturerByName(String buildingName)
    {
        return Optional.ofNullable(manufacturerByNameMap.get(buildingName));
    }

    public Collection<Manufacturer> getManufacturers()
    {
        return manufacturerByNameMap.values();
    }

    public Long getRawResourceMaxExtractionRatePerMinute(Item item)
    {
        return rawResources.get(item);
    }

    public Collection<? extends Item> getRawResources()
    {
        return rawResources.keySet();
    }

    public Optional<Recipe> getRecipeByClassName(String className)
    {
        return Optional.ofNullable(recipesByClassName.get(className));
    }

    public Optional<Recipe> getRecipeByName(String recipeName)
    {
        return Optional.ofNullable(recipesByName.get(recipeName));
    }

    public Collection<Recipe> getRecipes()
    {
        return recipesByName.values();
    }

    public JsonObject toJson()
    {
        JsonObjectBuilder jsonGameData = Json.createObjectBuilder();

        {
            JsonArrayBuilder jsonBuildings = Json.createArrayBuilder();

            for (Manufacturer manufacturer : manufacturerByNameMap.values()){
                jsonBuildings.add(manufacturer.toJson());
            }

            jsonGameData.add("manufacturers", jsonBuildings.build());
        }

        {
            JsonArrayBuilder jsonItems = Json.createArrayBuilder();

            for (Item item : itemsByClassName.values()){
                jsonItems.add(item.toJson());
            }

            jsonGameData.add("items", jsonItems.build());
        }

        {
            JsonArrayBuilder jsonRecipes = Json.createArrayBuilder();

            for (Recipe recipe : recipesByName.values()){
                jsonRecipes.add(recipe.toJson());
            }

            jsonGameData.add("recipes", jsonRecipes.build());
        }

        {
            JsonObjectBuilder jsonRawResources = Json.createObjectBuilder();

            for (var entry : rawResources.entrySet()){
                jsonRawResources.add(entry.getKey().getClassName(), entry.getValue());
            }

            jsonGameData.add("rawResources", jsonRawResources.build());
        }

        return jsonGameData.build();
    }

    public static class Builder
            extends AbstractBuilder<Builder>
    {
        @Override
        public GameData build()
        {
            return new GameData(this);
        }

        @Override
        protected Builder self()
        {
            return this;
        }
    }

    public static abstract class AbstractBuilder<B extends AbstractBuilder<B>>
    {
        private final Map<String, Item> itemsByName = new TreeMap<>();
        private final Map<String, Item> itemsByClassName = new TreeMap<>();
        private final Map<String, Manufacturer> manufacturersByClassName = new TreeMap<>();
        private final Collection<Recipe> recipes = new LinkedList<>();
        private final Map<Item, Long> rawResources = new TreeMap<>(Comparator.comparing(Item::getClassName));

        public B addItem(Item item)
        {
            this.itemsByName.put(item.getName(), item);
            this.itemsByClassName.put(item.getClassName(), item);
            return self();
        }

        public B addManufacturer(Manufacturer manufacturer)
        {
            this.manufacturersByClassName.put(manufacturer.getClassName(), manufacturer);
            return self();
        }

        public B addRawResources(Item item, long maxExtractionRatePerMinute)
        {
            this.rawResources.put(item, maxExtractionRatePerMinute);
            return self();
        }

        public B addRecipe(Recipe recipe)
        {
            this.recipes.add(recipe);
            return self();
        }

        public abstract GameData build();

        public Optional<Item> getItemByClassName(String className)
        {
            return Optional.ofNullable(itemsByClassName.get(Objects.requireNonNull(className)));
        }

        public Optional<Item> getItemByName(String name)
        {
            return Optional.ofNullable(itemsByName.get(Objects.requireNonNull(name)));
        }

        public Optional<Manufacturer> getManufacturerByClassName(String className)
        {
            return Optional.ofNullable(manufacturersByClassName.get(Objects.requireNonNull(className)));
        }

        protected abstract B self();
    }
}
