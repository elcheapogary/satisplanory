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
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
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

    protected GameData(Builder builder)
    {
        {
            Map<String, Item> tmpItemsByName = new TreeMap<>();
            Map<String, Item> tmpItemsByClassName = new TreeMap<>();

            for (Item item : builder.items){
                if (tmpItemsByName.put(item.getName(), item) != null){
                    throw new IllegalArgumentException("Duplicate item name: " + item.getName());
                }
                if (tmpItemsByClassName.put(item.getClassName(), item) != null){
                    throw new IllegalArgumentException("Duplicate item class name: " + item.getClassName());
                }
            }
            this.itemsByName = Collections.unmodifiableMap(tmpItemsByName);
            this.itemsByClassName = Collections.unmodifiableMap(tmpItemsByClassName);
        }
        {
            Map<String, Manufacturer> tmpBuildingsByName = new TreeMap<>();
            Map<String, Manufacturer> tmpBuildingsByClassName = new TreeMap<>();

            for (Manufacturer manufacturer : builder.manufacturers){
                if (tmpBuildingsByName.put(manufacturer.getName(), manufacturer) != null){
                    throw new IllegalArgumentException("Duplicate building name: " + manufacturer.getName());
                }
                if (tmpBuildingsByClassName.put(manufacturer.getClassName(), manufacturer) != null){
                    throw new IllegalArgumentException("Duplicate building class name: " + manufacturer.getClassName());
                }
            }
            this.manufacturerByNameMap = Collections.unmodifiableMap(tmpBuildingsByName);
            this.manufacturerByClassMap = Collections.unmodifiableMap(tmpBuildingsByClassName);
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
    }

    public static GameData fromJson(JsonObject json)
    {
        return fromJson(json, new Builder());
    }

    public static GameData fromJson(JsonObject json, GameData.Builder builder)
    {
        Map<String, Manufacturer> manufacturerByClassMap = new TreeMap<>();

        for (JsonObject jo : json.getJsonArray("manufacturers").getValuesAs(JsonObject.class)){
            Manufacturer manufacturer = Manufacturer.fromJson(jo);
            manufacturerByClassMap.put(manufacturer.getClassName(), manufacturer);
            builder.addBuilding(manufacturer);
        }

        Map<String, Item> itemByClassName = new TreeMap<>();

        for (JsonObject jo : json.getJsonArray("items").getValuesAs(JsonObject.class)){
            Item item = Item.fromJson(jo);
            itemByClassName.put(item.getClassName(), item);
            builder.addItem(item);
        }

        for (JsonObject jo : json.getJsonArray("recipes").getValuesAs(JsonObject.class)){
            builder.addRecipe(Recipe.fromJson(jo, manufacturerByClassMap::get, itemByClassName::get));
        }

        return builder.build();
    }

    private static GameData loadFromResource(String resourceName, GameData.Builder builder)
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

    public static GameData loadUpdate8Data(GameData.Builder builder)
            throws IOException
    {
        return loadFromResource("u8-data.json", builder);
    }

    public static GameData loadUpdate7Data()
            throws IOException
    {
        return loadUpdate7Data(new Builder());
    }

    public static GameData loadUpdate7Data(Builder builder)
            throws IOException
    {
        return loadFromResource("u7-data.json", builder);
    }

    public static GameData loadLatestGameData()
            throws IOException
    {
        return loadLatestGameData(new Builder());
    }

    public static GameData loadLatestGameData(Builder builder)
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

        return jsonGameData.build();
    }

    public static class Builder
    {
        private final Collection<Item> items = Item.createSet();
        private final Collection<Manufacturer> manufacturers = new LinkedList<>();
        private final Collection<Recipe> recipes = new LinkedList<>();

        public Builder addBuilding(Manufacturer manufacturer)
        {
            this.manufacturers.add(manufacturer);
            return this;
        }

        public Builder addItem(Item item)
        {
            this.items.add(item);
            return this;
        }

        public Builder addRecipe(Recipe recipe)
        {
            this.recipes.add(recipe);
            return this;
        }

        public GameData build()
        {
            return new GameData(this);
        }
    }
}
