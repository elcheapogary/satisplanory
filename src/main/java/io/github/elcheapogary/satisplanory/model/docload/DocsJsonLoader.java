/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.model.docload;

import io.github.elcheapogary.satisplanory.model.Building;
import io.github.elcheapogary.satisplanory.model.GameData;
import io.github.elcheapogary.satisplanory.model.Item;
import io.github.elcheapogary.satisplanory.model.MatterState;
import io.github.elcheapogary.satisplanory.model.Recipe;
import io.github.elcheapogary.satisplanory.util.Pair;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

/**
 * Loads game data from the Satisfactory {@code Docs.json} file shipped with the game.
 */
public class DocsJsonLoader
{
    private static final String ITEM_DESCRIPTOR_NATIVE_CLASS = "Class'/Script/FactoryGame.FGItemDescriptor'";

    private DocsJsonLoader()
    {
    }

    private static Set<String> getItemDescriptorFields(JsonArray nativeClassesArray)
            throws IOException
    {
        for (JsonObject object : nativeClassesArray.getValuesAs(JsonObject.class)){
            String nativeClassName = object.getString("NativeClass", null);

            if (nativeClassName == null || !nativeClassName.equals(ITEM_DESCRIPTOR_NATIVE_CLASS)){
                continue;
            }

            JsonArray classesArray = object.getJsonArray("Classes");

            JsonObject firstClass = classesArray.getJsonObject(0);

            Set<String> fieldNames = new TreeSet<>(firstClass.keySet());

            /*
             * Update 6 - some subclasses of FGItemDescriptor do not have mResourceSinkPoints??
             */
            fieldNames.remove("mResourceSinkPoints");

            return fieldNames;
        }

        throw new IOException("Unable to find native class: " + ITEM_DESCRIPTOR_NATIVE_CLASS);
    }

    private static Map<String, Item> getItemsByClassName(JsonArray nativeClassesArray)
            throws DataException, IOException
    {
        Set<String> itemDescriptorFields = getItemDescriptorFields(nativeClassesArray);

        Map<String, Item> itemsByClassName = new TreeMap<>();

        for (JsonObject nativeClass : nativeClassesArray.getValuesAs(JsonObject.class)){
            if (!nativeClassIsItemDescriptor(nativeClass, itemDescriptorFields)){
                continue;
            }

            String nativeClassName = nativeClass.getString("NativeClass");

            JsonArray classesArray = nativeClass.getJsonArray("Classes");

            for (JsonObject jsonItem : classesArray.getValuesAs(JsonObject.class)){
                Item.Builder itemBuilder = new Item.Builder();

                itemBuilder.setName(jsonItem.getString("mDisplayName"));
                itemBuilder.setClassName(jsonItem.getString("ClassName"));
                itemBuilder.setDescription(jsonItem.getString("mDescription"));

                switch (jsonItem.getString("mForm")){
                    case "RF_SOLID" -> itemBuilder.setMatterState(MatterState.SOLID);
                    case "RF_LIQUID" -> itemBuilder.setMatterState(MatterState.LIQUID);
                    case "RF_GAS" -> itemBuilder.setMatterState(MatterState.GAS);
                    default ->
                            throw new DataException("Unknown mForm value for item: " + nativeClassName + ": " + jsonItem.getString("mForm"));
                }

                itemBuilder.setSinkValue(jsonItem.getInt("mResourceSinkPoints", 0));

                Item item = itemBuilder.build();
                itemsByClassName.put(item.getClassName(), item);
            }
        }

        return itemsByClassName;
    }

    public static void loadDocsJson(GameData.Builder gameDataBuilder, InputStream in)
            throws IOException, DataException
    {
        try{
            JsonArray nativeClassesArray = Json.createParser(in).getArray();

            Map<String, Item> itemsByClassName = getItemsByClassName(nativeClassesArray);
            Map<String, Building> buildingsByClassName = new TreeMap<>();

            for (JsonObject nativeClassObject : nativeClassesArray.getValuesAs(JsonObject.class)){
                String nativeClassName = nativeClassObject.getString("NativeClass");

                if (nativeClassName == null){
                    continue;
                }

                if ("Class'/Script/FactoryGame.FGBuildableManufacturer'".equals(nativeClassName) ||
                        "Class'/Script/FactoryGame.FGBuildableManufacturerVariablePower'".equals(nativeClassName)){

                    for (JsonObject jsonBuilding : nativeClassObject.getJsonArray("Classes").getValuesAs(JsonObject.class)){
                        String name = jsonBuilding.getString("mDisplayName");
                        String className = jsonBuilding.getString("ClassName");
                        String powerConsumption = jsonBuilding.getString("mPowerConsumption");

                        Building building = new Building.Builder()
                                .setName(name)
                                .setClassName(className)
                                .setPowerConsumption(new BigDecimal(powerConsumption))
                                .build();

                        gameDataBuilder.addBuilding(building);
                        buildingsByClassName.put(className, building);
                    }
                }
            }

            for (JsonObject object : nativeClassesArray.getValuesAs(JsonObject.class)){
                String nativeClassName = object.getString("NativeClass", null);

                if ("Class'/Script/FactoryGame.FGRecipe'".equals(nativeClassName)){
                    Set<String> unknownBuildingClasses = new TreeSet<>();
                    Collection<Recipe> recipes = new LinkedList<>();

                    JsonArray classesArray = object.getJsonArray("Classes");

                    for (JsonObject jsonRecipe : classesArray.getValuesAs(JsonObject.class)){
                        String displayName = jsonRecipe.getString("mDisplayName");

                        Building producedIn = null;

                        {
                            String producedInStr = jsonRecipe.getString("mProducedIn");

                            if (producedInStr.isEmpty()){
                                continue;
                            }

                            List<String> producedInClasses;

                            try (Stream<String> stream = BracketObjectNotation.parseArray(producedInStr).stream()){
                                producedInClasses = stream.map(s -> {
                                    int idx = s.lastIndexOf(".");

                                    if (idx < 0){
                                        return s;
                                    }

                                    return s.substring(idx + 1);
                                }).collect(Collectors.toList());
                            }

                            for (String className : producedInClasses){
                                Building b = buildingsByClassName.get(className);

                                if (b == null){
                                    unknownBuildingClasses.add(className);
                                }else if (producedIn != null){
                                    throw new DataException("Recipe is made in multiple buildings: " + displayName);
                                }else{
                                    producedIn = b;
                                }
                            }
                        }

                        if (producedIn == null){
                            continue;
                        }

                        BigDecimal craftingTimeSeconds = new BigDecimal(jsonRecipe.getString("mManufactoringDuration"));

                        Collection<Pair<Item, Integer>> ingredients;

                        try{
                            ingredients = parseItemAmountList(jsonRecipe.getString("mIngredients"), itemsByClassName);
                        }catch (BracketObjectNotationParseException e){
                            throw new DataException("Error parsing ingredients for recipe: " + displayName + ": " + jsonRecipe.getString("mIngredients") + ": " + e, e);
                        }

                        Collection<Pair<Item, Integer>> products;

                        try{
                            products = parseItemAmountList(jsonRecipe.getString("mProduct"), itemsByClassName);
                        }catch (BracketObjectNotationParseException e){
                            throw new DataException("Error parsing products for recipe: " + displayName + ": " + jsonRecipe.getString("mProduct") + ": " + e, e);
                        }

                        Recipe.Builder recipeBuilder = new Recipe.Builder()
                                .setName(displayName)
                                .setCycleTimeSeconds(craftingTimeSeconds)
                                .setProducedInBuilding(producedIn)
                                .setVariablePowerConstant(new BigDecimal(jsonRecipe.getString("mVariablePowerConsumptionConstant")))
                                .setVariablePowerFactor(new BigDecimal(jsonRecipe.getString("mVariablePowerConsumptionFactor")));

                        for (Pair<Item, Integer> p : ingredients){
                            recipeBuilder.addIngredient(p.key(), p.value());
                        }

                        for (Pair<Item, Integer> p : products){
                            recipeBuilder.addProduct(p.key(), p.value());
                        }

                        recipes.add(recipeBuilder.build());
                    }

                    {
                        Set<String> itemClassesUsedInRecipes = new TreeSet<>();
                        for (Recipe r : recipes){
                            for (Recipe.RecipeItem ri : r.getIngredients()){
                                itemClassesUsedInRecipes.add(ri.getItem().getClassName());
                            }
                            for (Recipe.RecipeItem ri : r.getProducts()){
                                itemClassesUsedInRecipes.add(ri.getItem().getClassName());
                            }
                            gameDataBuilder.addRecipe(r);
                        }
                        itemsByClassName.keySet().retainAll(itemClassesUsedInRecipes);
                    }

                    for (Item item : itemsByClassName.values()){
                        gameDataBuilder.addItem(item);
                    }
                }
            }
        }catch (RuntimeException e){
            throw new IOException("Error parsing json data: " + e, e);
        }
    }

    public static void loadDocsJson(GameData.Builder gameDataBuilder, File f)
            throws IOException, DataException
    {
        try{
            try (InputStream in = new FileInputStream(f)){
                loadDocsJson(gameDataBuilder, in);
            }
        }catch (IOException | RuntimeException e){
            throw new IOException("Error loading json data from file: " + f.getAbsolutePath() + ": " + e.getMessage(), e);
        }
    }

    private static boolean nativeClassIsItemDescriptor(JsonObject nativeClass, Set<String> itemDescriptorFields)
    {
        JsonArray classesArray = nativeClass.getJsonArray("Classes");

        JsonObject firstClass = classesArray.getJsonObject(0);

        /*
         * Update 6 - building descriptors use mForm RF_INVALID - this gets rid of them.
         */
        if ("RF_INVALID".equals(firstClass.getString("mForm", null))){
            return false;
        }

        for (String fieldName : itemDescriptorFields){
            if (!firstClass.containsKey(fieldName)){
                return false;
            }
        }

        return true;
    }

    private static Collection<Pair<Item, Integer>> parseItemAmountList(String s, Map<String, Item> itemByClassNameMap)
            throws BracketObjectNotationParseException
    {
        List<Pair<Item, Integer>> retv = new LinkedList<>();

        for (BONObject o : BracketObjectNotation.parseArray(s, BracketObjectNotation::parseObject)){
            int amount = o.getInteger("Amount");

            String c = o.getString("ItemClass");

            if (c == null){
                throw new BracketObjectNotationParseException("Missing element: ItemClass");
            }

            int idx = c.lastIndexOf(".");

            if (idx < 0){
                throw new BracketObjectNotationParseException("Item class name does not contain \".\"");
            }

            if (!c.endsWith("\"'")){
                throw new BracketObjectNotationParseException("Item class not quoted as expected: " + c);
            }

            c = c.substring(idx + 1, c.length() - 2);

            Item item = itemByClassNameMap.get(c);

            if (item == null){
                throw new BracketObjectNotationParseException("Unrecognized item class name: " + c);
            }

            retv.add(new Pair<>(item, amount));
        }

        return retv;
    }
}
