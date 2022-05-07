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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class GameData
{
    private final Map<String, Item> itemsByName;
    private final Map<String, Item> itemsByClassName;
    private final Map<String, Building> buildingsByName;
    private final Map<String, Building> buildingsByClassName;
    private final Map<String, Recipe> recipesByName;

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
            Map<String, Building> tmpBuildingsByName = new TreeMap<>();
            Map<String, Building> tmpBuildingsByClassName = new TreeMap<>();

            for (Building building : builder.buildings){
                if (tmpBuildingsByName.put(building.getName(), building) != null){
                    throw new IllegalArgumentException("Duplicate building name: " + building.getName());
                }
                if (tmpBuildingsByClassName.put(building.getClassName(), building) != null){
                    throw new IllegalArgumentException("Duplicate building class name: " + building.getClassName());
                }
            }
            this.buildingsByName = Collections.unmodifiableMap(tmpBuildingsByName);
            this.buildingsByClassName = Collections.unmodifiableMap(tmpBuildingsByClassName);
        }
        {
            Map<String, Recipe> tmpRecipesByName = new TreeMap<>();

            for (Recipe r : builder.recipes){
                tmpRecipesByName.put(r.getName(), r);
            }

            this.recipesByName = Collections.unmodifiableMap(tmpRecipesByName);
        }
    }

    public Optional<Building> getBuildingByClassName(String buildingClassName)
    {
        return Optional.ofNullable(buildingsByClassName.get(buildingClassName));
    }

    public Optional<Building> getBuildingByName(String buildingName)
    {
        return Optional.ofNullable(buildingsByName.get(buildingName));
    }

    public Collection<Building> getBuildings()
    {
        return buildingsByName.values();
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

    public Optional<Recipe> getRecipeByName(String recipeName)
    {
        return Optional.ofNullable(recipesByName.get(recipeName));
    }

    public Collection<Recipe> getRecipes()
    {
        return recipesByName.values();
    }

    public static class Builder
    {
        private final Collection<Item> items = Item.createSet();
        private final Collection<Building> buildings = new LinkedList<>();
        private final Collection<Recipe> recipes = new LinkedList<>();

        public Builder addBuilding(Building building)
        {
            this.buildings.add(building);
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
