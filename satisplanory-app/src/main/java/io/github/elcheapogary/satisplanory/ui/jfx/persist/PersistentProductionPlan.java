/*
 * Copyright (c) 2023 elcheapogary
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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;

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

    public void loadJson(JsonObject json)
            throws UnsupportedVersionException
    {
        if (!json.containsKey("v")){
            throw new UnsupportedVersionException();
        }
        String version = json.getString("v");
        if (version.equals("1.0")){
            this.input.loadJson_1_0(json.getJsonObject("input"));
        }else if (version.equals("2.0")){
            this.input.loadJson_2_0(json.getJsonObject("input"));
        }else if (version.equals("3.0")){
            this.input.loadJson_3_0(json.getJsonObject("input"));
        }else{
            throw new UnsupportedVersionException();
        }
        if (json.containsKey("name")){
            this.name = json.getString("name");
        }
        if (json.containsKey("plan")){
            this.plan = new Plan(json.getJsonObject("plan"));
        }
    }

    public JsonObject toJson()
    {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("v", "3.0")
                .add("input", input.toJson())
                .add("name", name);

        if (plan != null){
            b = b.add("plan", plan.toJson());
        }

        return b.build();
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

        private static <V> Map<String, V> toMap(JsonObject json, BiFunction<? super JsonObject, String, V> extractor)
        {
            Map<String, V> map = new TreeMap<>();
            for (String key : json.keySet()){
                map.put(key, extractor.apply(json, key));
            }
            return map;
        }

        private static Map<String, String> toMapFromDecimal(JsonObject json)
        {
            return toMap(json, JsonObject::getString);
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

        public void loadJson_1_0(JsonObject json)
        {
            this.settings.loadJson_1_0(json.getJsonObject("settings"));
            this.recipes.loadJson(json.getJsonObject("recipes"));

            if (json.containsKey("inputItems")){
                this.inputItems.putAll(toMapFromDecimal(json.getJsonObject("inputItems")));
            }

            if (json.containsKey("outputItemsPerMinute")){
                this.outputItemsPerMinute.putAll(toMapFromDecimal(json.getJsonObject("outputItemsPerMinute")));
            }

            if (json.containsKey("maximizedOutputItems")){
                this.maximizedOutputItems.putAll(toMapFromDecimal(json.getJsonObject("maximizedOutputItems")));
            }
        }

        public void loadJson_2_0(JsonObject json)
        {
            this.settings.loadJson_2_0(json.getJsonObject("settings"));
            this.recipes.loadJson(json.getJsonObject("recipes"));

            if (json.containsKey("inputItems")){
                this.inputItems.putAll(toMapFromDecimal(json.getJsonObject("inputItems")));
            }

            if (json.containsKey("outputItemsPerMinute")){
                this.outputItemsPerMinute.putAll(toMapFromDecimal(json.getJsonObject("outputItemsPerMinute")));
            }

            if (json.containsKey("maximizedOutputItems")){
                this.maximizedOutputItems.putAll(toMapFromDecimal(json.getJsonObject("maximizedOutputItems")));
            }
        }

        public void loadJson_3_0(JsonObject json)
        {
            this.settings.loadJson_2_0(json.getJsonObject("settings"));
            this.recipes.loadJson(json.getJsonObject("recipes"));

            if (json.containsKey("inputItems")){
                this.inputItems.putAll(toMap(json.getJsonObject("inputItems"), JsonObject::getString));
            }

            if (json.containsKey("outputItemsPerMinute")){
                this.outputItemsPerMinute.putAll(toMap(json.getJsonObject("outputItemsPerMinute"), JsonObject::getString));
            }

            if (json.containsKey("maximizedOutputItems")){
                this.maximizedOutputItems.putAll(toMap(json.getJsonObject("maximizedOutputItems"), JsonObject::getString));
            }
        }

        JsonObject toJson()
        {
            return Json.createObjectBuilder()
                    .add("settings", settings.toJson())
                    .add("recipes", recipes.toJson())
                    .add("inputItems", stringMapToJsonObject(inputItems))
                    .add("outputItemsPerMinute", stringMapToJsonObject(outputItemsPerMinute))
                    .add("maximizedOutputItems", stringMapToJsonObject(maximizedOutputItems))
                    .build();
        }

        private static JsonObject stringMapToJsonObject(Map<String, String> map)
        {
            return mapToJsonObject(map, (jsonObjectBuilder, stringStringEntry) -> jsonObjectBuilder.add(stringStringEntry.getKey(), stringStringEntry.getValue()));
        }

        private static <V> JsonObject mapToJsonObject(Map<String, V> map, BiFunction<? super JsonObjectBuilder, Map.Entry<String, V>, ? extends JsonObjectBuilder> setter)
        {
            JsonObjectBuilder b = Json.createObjectBuilder();

            for (var e : map.entrySet()){
                b = setter.apply(b, e);
            }

            return b.build();
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

            public void loadJson(JsonObject json)
            {
                if (json.containsKey("recipeNames")){
                    JsonArray jsonArray = json.getJsonArray("recipeNames");

                    for (int i = 0; i < jsonArray.size(); i++){
                        recipeNames.add(jsonArray.getString(i));
                    }
                }
            }

            public JsonObject toJson()
            {
                return Json.createObjectBuilder()
                        .add("recipeNames", Json.createArrayBuilder(recipeNames))
                        .build();
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

            public void loadJson_1_0(JsonObject json)
            {
                if (json.containsKey("optimizationTarget")){
                    switch (json.getString("optimizationTarget")){
                        case "":{
                            break;
                        }
                    }
                }
            }

            public void loadJson_2_0(JsonObject json)
            {
                if (json.containsKey("optimizationTargets")){
                    JsonArray array = json.getJsonArray("optimizationTargets");
                    for (JsonString s : array.getValuesAs(JsonString.class)){
                        optimizationTargets.add(s.getString());
                    }
                }
            }

            JsonObject toJson()
            {
                return Json.createObjectBuilder()
                        .add("optimizationTargets", Json.createArrayBuilder(optimizationTargets).build())
                        .build();
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

        public Plan(JsonObject json)
        {
            if (json.containsKey("inputItems")){
                inputItems.putAll(toBigFractionMap(json.getJsonObject("inputItems")));
            }
            if (json.containsKey("outputItems")){
                outputItems.putAll(toBigFractionMap(json.getJsonObject("outputItems")));
            }
            if (json.containsKey("recipes")){
                recipes.putAll(toBigFractionMap(json.getJsonObject("recipes")));
            }
        }

        private static Map<String, BigFraction> toBigFractionMap(JsonObject object)
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

        JsonObject toJson()
        {
            return Json.createObjectBuilder()
                    .add("inputItems", toJson(inputItems))
                    .add("outputItems", toJson(outputItems))
                    .add("recipes", toJson(recipes))
                    .build();
        }

        private JsonObject toJson(Map<String, BigFraction> map)
        {
            JsonObjectBuilder b = Json.createObjectBuilder();

            for (var entry : map.entrySet()){
                b = b.add(entry.getKey(), entry.getValue().toString());
            }

            return b.build();
        }
    }
}
