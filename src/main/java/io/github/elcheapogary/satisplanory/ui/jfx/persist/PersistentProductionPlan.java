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

import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import org.json.JSONArray;
import org.json.JSONObject;

public class PersistentProductionPlan
{
    private final Input input = new Input();
    private String name = "Unnamed Factory";
    private Plan plan;

    public PersistentProductionPlan()
    {
    }

    public Input getInput()
    {
        return input;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Plan getPlan()
    {
        return plan;
    }

    public void setPlan(Plan plan)
    {
        this.plan = plan;
    }

    public void loadJson(JSONObject json)
            throws UnsupportedVersionException
    {
        if (!json.has("v")){
            throw new UnsupportedVersionException();
        }
        String version = json.getString("v");
        if (version.equals("1.0")){
            this.input.loadJson_1_0(json.getJSONObject("input"));
        }else if (version.equals("2.0")){
            this.input.loadJson_2_0(json.getJSONObject("input"));
        }else if (version.equals("3.0")){
            this.input.loadJson_3_0(json.getJSONObject("input"));
        }else{
            throw new UnsupportedVersionException();
        }
        if (json.has("name")){
            this.name = json.getString("name");
        }
        if (json.has("plan")){
            this.plan = new Plan(json.getJSONObject("plan"));
        }
    }

    public JSONObject toJson()
    {
        JSONObject json = new JSONObject();

        json.put("v", "3.0");
        json.put("input", input.toJson());
        json.put("name", name);

        if (plan != null){
            json.put("plan", plan.toJson());
        }

        return json;
    }

    public static class Input
    {
        private final RecipeSet recipes = new RecipeSet();
        private final Settings settings = new Settings();
        private final Map<String, String> inputItems = new TreeMap<>();
        private final Map<String, String> outputItemsPerMinute = new TreeMap<>();
        private final Map<String, String> maximizedOutputItems = new TreeMap<>();

        public Input()
        {
        }

        private static <V> Map<String, V> toMap(JSONObject json, BiFunction<? super JSONObject, String, V> extractor)
        {
            Map<String, V> map = new TreeMap<>();
            for (String key : json.keySet()){
                map.put(key, extractor.apply(json, key));
            }
            return map;
        }

        private static Map<String, String> toMapFromDecimal(JSONObject json)
        {
            return toMap(json, (jsonObject, s) -> jsonObject.getBigDecimal(s).toString());
        }

        public Map<String, String> getInputItems()
        {
            return inputItems;
        }

        public Map<String, String> getMaximizedOutputItems()
        {
            return maximizedOutputItems;
        }

        public Map<String, String> getOutputItemsPerMinute()
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

        public void loadJson_1_0(JSONObject json)
        {
            this.settings.loadJson_1_0(json.getJSONObject("settings"));
            this.recipes.loadJson(json.getJSONObject("recipes"));

            if (json.has("inputItems")){
                this.inputItems.putAll(toMapFromDecimal(json.getJSONObject("inputItems")));
            }

            if (json.has("outputItemsPerMinute")){
                this.outputItemsPerMinute.putAll(toMapFromDecimal(json.getJSONObject("outputItemsPerMinute")));
            }

            if (json.has("maximizedOutputItems")){
                this.maximizedOutputItems.putAll(toMapFromDecimal(json.getJSONObject("maximizedOutputItems")));
            }
        }

        public void loadJson_2_0(JSONObject json)
        {
            this.settings.loadJson_2_0(json.getJSONObject("settings"));
            this.recipes.loadJson(json.getJSONObject("recipes"));

            if (json.has("inputItems")){
                this.inputItems.putAll(toMapFromDecimal(json.getJSONObject("inputItems")));
            }

            if (json.has("outputItemsPerMinute")){
                this.outputItemsPerMinute.putAll(toMapFromDecimal(json.getJSONObject("outputItemsPerMinute")));
            }

            if (json.has("maximizedOutputItems")){
                this.maximizedOutputItems.putAll(toMapFromDecimal(json.getJSONObject("maximizedOutputItems")));
            }
        }

        public void loadJson_3_0(JSONObject json)
        {
            this.settings.loadJson_2_0(json.getJSONObject("settings"));
            this.recipes.loadJson(json.getJSONObject("recipes"));

            if (json.has("inputItems")){
                this.inputItems.putAll(toMap(json.getJSONObject("inputItems"), JSONObject::getString));
            }

            if (json.has("outputItemsPerMinute")){
                this.outputItemsPerMinute.putAll(toMap(json.getJSONObject("outputItemsPerMinute"), JSONObject::getString));
            }

            if (json.has("maximizedOutputItems")){
                this.maximizedOutputItems.putAll(toMap(json.getJSONObject("maximizedOutputItems"), JSONObject::getString));
            }
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
            private final Set<String> recipeNames = new TreeSet<>();

            public RecipeSet()
            {
            }

            public Set<String> getRecipeNames()
            {
                return recipeNames;
            }

            public void loadJson(JSONObject json)
            {
                if (json.has("recipeNames")){
                    JSONArray jsonArray = json.getJSONArray("recipeNames");

                    for (int i = 0; i < jsonArray.length(); i++){
                        recipeNames.add(jsonArray.getString(i));
                    }
                }
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
            private final List<String> optimizationTargets = new LinkedList<>();

            public Settings()
            {
            }

            public List<String> getOptimizationTargets()
            {
                return optimizationTargets;
            }

            public void loadJson_1_0(JSONObject json)
            {
                if (json.has("optimizationTarget")){
                    switch (json.getString("optimizationTarget")){
                        case "":{
                            break;
                        }
                    }
                }
            }

            public void loadJson_2_0(JSONObject json)
            {
                if (json.has("optimizationTargets")){
                    JSONArray array = json.getJSONArray("optimizationTargets");
                    for (int i = 0; i < array.length(); i++){
                        optimizationTargets.add(array.getString(i));
                    }
                }
            }

            JSONObject toJson()
            {
                JSONObject json = new JSONObject();

                json.put("optimizationTargets", optimizationTargets);

                return json;
            }
        }
    }

    public static class Plan
    {
        private final Map<String, BigFraction> inputItems = new TreeMap<>();
        private final Map<String, BigFraction> outputItems = new TreeMap<>();
        private final Map<String, BigFraction> recipes = new TreeMap<>();

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

        public Map<String, BigFraction> getInputItems()
        {
            return inputItems;
        }

        public Map<String, BigFraction> getOutputItems()
        {
            return outputItems;
        }

        public Map<String, BigFraction> getRecipes()
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
                json.put(entry.getKey(), entry.getValue().toString());
            }

            return json;
        }
    }
}
