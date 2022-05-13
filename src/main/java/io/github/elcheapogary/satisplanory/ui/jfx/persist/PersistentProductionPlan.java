/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.persist;

import io.github.elcheapogary.satisplanory.prodplan.OptimizationTarget;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.TreeSet;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import org.json.JSONArray;
import org.json.JSONObject;

public class PersistentProductionPlan
{
    private final StringProperty name = new SimpleStringProperty("Unnamed Factory");
    private final Input input;
    private Plan plan;

    public PersistentProductionPlan()
    {
        this.input = new Input();
    }

    public PersistentProductionPlan(JSONObject json)
    {
        this.input = new Input(json.getJSONObject("input"));
        if (json.has("name")){
            this.name.set(json.getString("name"));
        }
        if (json.has("plan")){
            this.plan = new Plan(json.getJSONObject("plan"));
        }
    }

    public Input getInput()
    {
        return input;
    }

    public String getName()
    {
        return name.get();
    }

    public void setName(String name)
    {
        this.name.set(name);
    }

    public Plan getPlan()
    {
        return plan;
    }

    public void setPlan(Plan plan)
    {
        this.plan = plan;
    }

    public StringProperty nameProperty()
    {
        return name;
    }

    public JSONObject toJson()
    {
        JSONObject json = new JSONObject();

        json.put("v", "1.0");
        json.put("input", input.toJson());
        json.put("name", name.get());

        if (plan != null){
            json.put("plan", plan.toJson());
        }

        return json;
    }

    public static class Input
    {
        private final RecipeSet recipes;
        private final Settings settings;
        private final ObservableMap<String, BigDecimal> inputItems = FXCollections.observableMap(new TreeMap<>());
        private final ObservableMap<String, BigDecimal> outputItemsPerMinute = FXCollections.observableMap(new TreeMap<>());
        private final ObservableMap<String, BigDecimal> maximizedOutputItems = FXCollections.observableMap(new TreeMap<>());

        public Input()
        {
            this.recipes = new RecipeSet();
            this.settings = new Settings();
        }

        public Input(JSONObject json)
        {
            this.settings = new Settings(json.getJSONObject("settings"));
            this.recipes = new RecipeSet(json.getJSONObject("recipes"));

            if (json.has("inputItems")){
                this.inputItems.putAll(toMap(json.getJSONObject("inputItems")));
            }

            if (json.has("outputItemsPerMinute")){
                this.outputItemsPerMinute.putAll(toMap(json.getJSONObject("outputItemsPerMinute")));
            }

            if (json.has("maximizedOutputItems")){
                this.outputItemsPerMinute.putAll(toMap(json.getJSONObject("maximizedOutputItems")));
            }
        }

        private static Map<String, BigDecimal> toMap(JSONObject json)
        {
            Map<String, BigDecimal> map = new TreeMap<>();
            for (String key : json.keySet()){
                map.put(key, json.getBigDecimal(key));
            }
            return map;
        }

        public ObservableMap<String, BigDecimal> getInputItems()
        {
            return inputItems;
        }

        public ObservableMap<String, BigDecimal> getMaximizedOutputItems()
        {
            return maximizedOutputItems;
        }

        public ObservableMap<String, BigDecimal> getOutputItemsPerMinute()
        {
            return outputItemsPerMinute;
        }

        public RecipeSet getRecipes()
        {
            return recipes;
        }

        public Settings getSettings()
        {
            return settings;
        }

        JSONObject toJson()
        {
            JSONObject json = new JSONObject();

            json.put("settings", settings.toJson());
            json.put("recipes", recipes.toJson());
            json.put("inputItems", inputItems);
            json.put("outputItemsPerMinute", outputItemsPerMinute);
            json.put("maximizedOutputItems", maximizedOutputItems);

            return json;
        }

        public static class RecipeSet
        {
            private final ObservableSet<String> recipeNames = FXCollections.observableSet(new TreeSet<>());

            public RecipeSet()
            {
            }

            public RecipeSet(JSONObject json)
            {
                if (json.has("recipeNames")){
                    JSONArray jsonArray = json.getJSONArray("recipeNames");

                    for (int i = 0; i < jsonArray.length(); i++){
                        recipeNames.add(jsonArray.getString(i));
                    }
                }
            }

            public ObservableSet<String> getRecipeNames()
            {
                return recipeNames;
            }

            public JSONObject toJson()
            {
                JSONObject json = new JSONObject();

                json.put("recipeNames", new JSONArray(recipeNames));

                return json;
            }
        }

        public static class Settings
        {
            private final ObjectProperty<OptimizationTarget> optimizationTarget = new SimpleObjectProperty<>(OptimizationTarget.DEFAULT);

            public Settings()
            {
            }

            public Settings(JSONObject json)
            {
                if (json.has("optimizationTarget")){
                    try {
                        optimizationTarget.set(OptimizationTarget.valueOf(json.getString("optimizationTarget")));
                    }catch (NoSuchElementException ignore){
                    }
                }
            }

            public OptimizationTarget getOptimizationTarget()
            {
                return optimizationTarget.get();
            }

            public void setOptimizationTarget(OptimizationTarget optimizationTarget)
            {
                this.optimizationTarget.set(optimizationTarget);
            }

            public ObjectProperty<OptimizationTarget> optimizationTargetProperty()
            {
                return optimizationTarget;
            }

            JSONObject toJson()
            {
                JSONObject json = new JSONObject();

                if (optimizationTarget.getValue() != null){
                    json.put("optimizationTarget", optimizationTarget.getValue().name());
                }

                return json;
            }
        }
    }

    public static class Plan
    {
        private final ObservableMap<String, BigFraction> inputItems = FXCollections.observableMap(new TreeMap<>());
        private final ObservableMap<String, BigFraction> outputItems = FXCollections.observableMap(new TreeMap<>());
        private final ObservableMap<String, BigFraction> recipes = FXCollections.observableMap(new TreeMap<>());

        public Plan()
        {
        }

        public Plan(JSONObject json)
        {
            if (json.has("inputItems")){
                inputItems.putAll(toBigFractionMap(json.getJSONObject("inputItems")));
            }
            if (json.has("outputItems")){
                outputItems.putAll(toBigFractionMap(json.getJSONObject("outputItems")));
            }
            if (json.has("recipes")){
                recipes.putAll(toBigFractionMap(json.getJSONObject("recipes")));
            }
        }

        private static Map<String, BigFraction> toBigFractionMap(JSONObject object)
        {
            Map<String, BigFraction> map = new TreeMap<>();

            for (String key : object.keySet()){
                String value = object.getString(key);

                int idx = value.indexOf("/");

                if (idx < 0){
                    map.put(key, BigFraction.valueOf(new BigInteger(value)));
                }else{
                    map.put(key, BigFraction.valueOf(new BigInteger(value.substring(0, idx))).divide(new BigInteger(value.substring(idx + 1))));
                }
            }

            return map;
        }

        public ObservableMap<String, BigFraction> getInputItems()
        {
            return inputItems;
        }

        public ObservableMap<String, BigFraction> getOutputItems()
        {
            return outputItems;
        }

        public ObservableMap<String, BigFraction> getRecipes()
        {
            return recipes;
        }

        JSONObject toJson()
        {
            JSONObject json = new JSONObject();

            json.put("inputItems", toJson(inputItems));
            json.put("outputItems", toJson(outputItems));
            json.put("recipes", toJson(recipes));

            return json;
        }

        private JSONObject toJson(Map<String, BigFraction> map)
        {
            JSONObject json = new JSONObject();

            for (var entry : map.entrySet()){
                json.put(entry.getKey(), entry.getValue().simplify().toString());
            }

            return json;
        }
    }
}
