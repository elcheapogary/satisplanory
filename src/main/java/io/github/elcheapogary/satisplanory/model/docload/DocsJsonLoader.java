/*
 * Copyright (c) 2022 elcheapogary
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
import io.github.elcheapogary.satisplanory.util.CharStreams;
import io.github.elcheapogary.satisplanory.util.Pair;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Loads game data from the Satisfactory {@code Docs.json} file shipped with the game.
 */
public class DocsJsonLoader
{
    private static final String ITEM_DESCRIPTOR_NATIVE_CLASS = "Class'/Script/FactoryGame.FGItemDescriptor'";

    private DocsJsonLoader()
    {
    }

    private static Set<String> getItemDescriptorFields(JSONArray nativeClassesArray)
            throws IOException
    {
        for (int i = 0; i < nativeClassesArray.length(); i++){
            JSONObject object = nativeClassesArray.getJSONObject(i);

            String nativeClassName = object.optString("NativeClass", null);

            if (nativeClassName == null || !nativeClassName.equals(ITEM_DESCRIPTOR_NATIVE_CLASS)){
                continue;
            }

            JSONArray classesArray = object.getJSONArray("Classes");

            JSONObject firstClass = classesArray.getJSONObject(0);

            return firstClass.toMap().keySet();
        }

        throw new IOException("Unable to find native class: " + ITEM_DESCRIPTOR_NATIVE_CLASS);
    }

    private static Map<String, Item> getItemsByClassName(JSONArray nativeClassesArray)
            throws DataException, IOException
    {
        Set<String> itemDescriptorFields = getItemDescriptorFields(nativeClassesArray);

        Map<String, Item> itemsByClassName = new TreeMap<>();

        for (Object nativeClassObject : nativeClassesArray){
            if (!(nativeClassObject instanceof JSONObject nativeClass)){
                continue;
            }

            if (!nativeClassIsItemDescriptor(nativeClass, itemDescriptorFields)){
                continue;
            }

            String nativeClassName = nativeClass.getString("NativeClass");

            JSONArray classesArray = nativeClass.getJSONArray("Classes");

            for (int i = 0; i < classesArray.length(); i++){
                try {
                    JSONObject jsonItem = classesArray.getJSONObject(i);

                    Item.Builder itemBuilder = new Item.Builder();

                    itemBuilder.setName(jsonItem.getString("mDisplayName"));
                    itemBuilder.setClassName(jsonItem.getString("ClassName"));
                    itemBuilder.setDescription(jsonItem.getString("mDescription"));

                    switch (jsonItem.getString("mForm")){
                        case "RF_SOLID" -> itemBuilder.setMatterState(MatterState.SOLID);
                        case "RF_LIQUID" -> itemBuilder.setMatterState(MatterState.LIQUID);
                        case "RF_GAS" -> itemBuilder.setMatterState(MatterState.GAS);
                        default -> throw new DataException("Unknown mForm value for item: " + nativeClassName + "[" + i + "]: " + jsonItem.getString("mForm"));
                    }

                    itemBuilder.setSinkValue(jsonItem.optInt("mResourceSinkPoints", 0));

                    Item item = itemBuilder.build();
                    itemsByClassName.put(item.getClassName(), item);
                }catch (JSONException e){
                    throw new IOException("Error loading class " + i + " of native class: " + nativeClassName + ": " + e, e);
                }
            }
        }

        return itemsByClassName;
    }

    public static void loadDocsJson(GameData.Builder gameDataBuilder, InputStream in)
            throws IOException, DataException
    {
        try {
            JSONArray nativeClassesArray = parseJson(in);

            Map<String, Item> itemsByClassName = getItemsByClassName(nativeClassesArray);
            Map<String, Building> buildingsByClassName = new TreeMap<>();

            for (int i = 0; i < nativeClassesArray.length(); i++){
                JSONObject object = nativeClassesArray.getJSONObject(i);

                String nativeClassName = object.optString("NativeClass", null);

                if (nativeClassName == null){
                    continue;
                }

                Object classesObject = object.opt("Classes");

                if (!(classesObject instanceof JSONArray classesArray)){
                    continue;
                }

                if ("Class'/Script/FactoryGame.FGBuildableManufacturer'".equals(nativeClassName)
                        || "Class'/Script/FactoryGame.FGBuildableManufacturerVariablePower'".equals(nativeClassName)
                ){
                    for (int j = 0; j < classesArray.length(); j++){
                        JSONObject jsonBuilding = classesArray.getJSONObject(j);

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

            for (int i = 0; i < nativeClassesArray.length(); i++){
                JSONObject object = nativeClassesArray.getJSONObject(i);

                String nativeClassName = object.optString("NativeClass", null);

                if ("Class'/Script/FactoryGame.FGRecipe'".equals(nativeClassName)){
                    Set<String> unknownBuildingClasses = new TreeSet<>();
                    Collection<Recipe> recipes = new LinkedList<>();

                    JSONArray classesArray = object.getJSONArray("Classes");

                    for (int j = 0; j < classesArray.length(); j++){
                        JSONObject jsonRecipe = classesArray.getJSONObject(j);

                        String displayName = jsonRecipe.getString("mDisplayName");

                        Building producedIn = null;

                        {
                            String producedInStr = jsonRecipe.getString("mProducedIn");

                            if (producedInStr.isEmpty()){
                                continue;
                            }

                            List<String> producedInClasses;

                            try (Stream<String> stream = BracketObjectNotation.parseArray(producedInStr).stream()) {
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

                        BigDecimal craftingTimeSeconds = jsonRecipe.getBigDecimal("mManufactoringDuration");

                        Collection<Pair<Item, Integer>> ingredients;

                        try {
                            ingredients = parseItemAmountList(jsonRecipe.getString("mIngredients"), itemsByClassName);
                        }catch (BracketObjectNotationParseException e){
                            throw new DataException("Error parsing ingredients for recipe: " + displayName + ": " + jsonRecipe.getString("mIngredients") + ": " + e, e);
                        }

                        Collection<Pair<Item, Integer>> products;

                        try {
                            products = parseItemAmountList(jsonRecipe.getString("mProduct"), itemsByClassName);
                        }catch (BracketObjectNotationParseException e){
                            throw new DataException("Error parsing products for recipe: " + displayName + ": " + jsonRecipe.getString("mProduct") + ": " + e, e);
                        }

                        Recipe.Builder recipeBuilder = new Recipe.Builder()
                                .setName(displayName)
                                .setCycleTimeSeconds(craftingTimeSeconds)
                                .setProducedInBuilding(producedIn)
                                .setVariablePowerConstant(jsonRecipe.getBigDecimal("mVariablePowerConsumptionConstant"))
                                .setVariablePowerFactor(jsonRecipe.getBigDecimal("mVariablePowerConsumptionFactor"));

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
        }catch (JSONException e){
            throw new IOException("Error parsing json data: " + e.getMessage(), e);
        }
    }

    public static void loadDocsJson(GameData.Builder gameDataBuilder, File f)
            throws IOException, DataException
    {
        try {
            try (InputStream in = new FileInputStream(f)) {
                loadDocsJson(gameDataBuilder, in);
            }
        }catch (IOException | RuntimeException e){
            throw new IOException("Error loading json data from file: " + f.getAbsolutePath() + ": " + e.getMessage(), e);
        }
    }

    private static boolean nativeClassIsItemDescriptor(JSONObject nativeClass, Set<String> itemDescriptorFields)
    {
        Object classesObject = nativeClass.opt("Classes");

        if (!(classesObject instanceof JSONArray classesArray)){
            return false;
        }

        JSONObject firstClass = classesArray.getJSONObject(0);

        for (String fieldName : itemDescriptorFields){
            if (!firstClass.has(fieldName)){
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

    private static JSONArray parseJson(InputStream in)
            throws IOException
    {
        try (Reader r = CharStreams.createReader(in)) {
            return new JSONArray(new JSONTokener(r));
        }
    }
}
