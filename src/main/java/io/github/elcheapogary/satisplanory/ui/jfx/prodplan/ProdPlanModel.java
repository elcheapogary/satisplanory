/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.prodplan;

import io.github.elcheapogary.satisplanory.model.GameData;
import io.github.elcheapogary.satisplanory.model.Item;
import io.github.elcheapogary.satisplanory.model.Recipe;
import io.github.elcheapogary.satisplanory.prodplan.MultiPlan;
import io.github.elcheapogary.satisplanory.prodplan.ProductionPlan;
import io.github.elcheapogary.satisplanory.ui.jfx.persist.PersistentProductionPlan;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

class ProdPlanModel
{
    private final StringProperty name = new SimpleStringProperty("Unnamed Factory");
    private final ObservableList<InputItem> inputItems = FXCollections.observableArrayList(param -> new Observable[]{
            param.item,
            param.amount
    });
    private final ObservableList<OutputItem> outputItems = FXCollections.observableArrayList(param -> new Observable[]{
            param.item,
            param.min,
            param.weight
    });
    private final ObservableSet<Recipe> enabledRecipes = FXCollections.observableSet(Recipe.createSet());
    private final ObjectProperty<ProductionPlan> plan = new SimpleObjectProperty<>();
    private final ObjectProperty<MultiPlan> multiPlan = new SimpleObjectProperty<>();

    private final Settings settings = new Settings();

    public ProdPlanModel()
    {
    }

    public static ProdPlanModel fromPersistent(GameData gameData, PersistentProductionPlan persistent)
    {
        ProdPlanModel model = new ProdPlanModel();

        model.setName(persistent.getName());
        model.nameProperty().addListener((observable, oldValue, name) -> persistent.setName(name));

        if (persistent.getPlan() != null){
            Map<Recipe, BigFraction> recipeMap = Recipe.createMap();
            Map<Item, BigFraction> inputItemsMap = Item.createMap();
            Map<Item, BigFraction> outputItemsMap = Item.createMap();

            for (var entry : persistent.getPlan().getRecipes().entrySet()){
                gameData.getRecipeByName(entry.getKey()).ifPresent(recipe -> recipeMap.put(recipe, entry.getValue()));
            }
            for (var entry : persistent.getPlan().getInputItems().entrySet()){
                gameData.getItemByName(entry.getKey()).ifPresent(item -> inputItemsMap.put(item, entry.getValue()));
            }
            for (var entry : persistent.getPlan().getOutputItems().entrySet()){
                gameData.getItemByName(entry.getKey()).ifPresent(item -> outputItemsMap.put(item, entry.getValue()));
            }
            model.setPlan(new ProductionPlan(recipeMap, inputItemsMap, outputItemsMap));
        }
        model.planProperty().addListener((observable, oldValue, plan) -> {
            if (plan != null){
                PersistentProductionPlan.Plan pp = new PersistentProductionPlan.Plan();

                for (Item item : plan.getInputItems()){
                    BigFraction amount = plan.getInputItemsPerMinute(item);
                    if (amount.signum() > 0){
                        pp.getInputItems().put(item.getName(), amount);
                    }
                }

                for (Item item : plan.getOutputItems()){
                    BigFraction amount = plan.getOutputItemsPerMinute(item);
                    if (amount.signum() > 0){
                        pp.getOutputItems().put(item.getName(), amount);
                    }
                }

                for (Recipe recipe : plan.getRecipes()){
                    BigFraction amount = plan.getNumberOfMachinesWithRecipe(recipe);
                    if (amount.signum() > 0){
                        pp.getRecipes().put(recipe.getName(), amount);
                    }
                }

                persistent.setPlan(pp);
            }
        });

        for (String s : persistent.getInput().getRecipes().getRecipeNames()){
            gameData.getRecipeByName(s).ifPresent(recipe -> model.getEnabledRecipes().add(recipe));
        }
        model.getEnabledRecipes().addListener((SetChangeListener<Recipe>) change -> {
            if (change.wasAdded()){
                persistent.getInput().getRecipes().getRecipeNames().add(change.getElementAdded().getName());
            }else if (change.wasRemoved()){
                persistent.getInput().getRecipes().getRecipeNames().remove(change.getElementRemoved().getName());
            }
        });

        for (var entry : persistent.getInput().getInputItems().entrySet()){
            gameData.getItemByName(entry.getKey()).ifPresent(item -> model.getInputItems().add(new InputItem(item, entry.getValue())));
        }
        model.getInputItems().addListener((ListChangeListener<InputItem>) c -> {
            persistent.getInput().getInputItems().clear();
            for (InputItem inputItem : model.getInputItems()){
                if (inputItem.getItem() != null){
                    persistent.getInput().getInputItems().put(inputItem.getItem().getName(), inputItem.getAmount());
                }
            }
        });

        for (Item item : gameData.getItems()){
            BigDecimal min = persistent.getInput().getOutputItemsPerMinute().get(item.getName());
            BigDecimal weight = persistent.getInput().getMaximizedOutputItems().get(item.getName());

            if ((min != null && min.signum() > 0) || (weight != null && weight.signum() > 0)){
                model.getOutputItems().add(new OutputItem(item, Objects.requireNonNullElse(min, BigDecimal.ZERO), Objects.requireNonNullElse(weight, BigDecimal.ZERO)));
            }
        }
        model.getOutputItems().addListener((ListChangeListener<OutputItem>) c -> {
            persistent.getInput().getOutputItemsPerMinute().clear();
            persistent.getInput().getMaximizedOutputItems().clear();

            for (OutputItem outputItem : model.getOutputItems()){
                if (outputItem.getItem() != null){
                    if (outputItem.getMin() != null && outputItem.getMin().signum() > 0){
                        persistent.getInput().getOutputItemsPerMinute().put(outputItem.getItem().getName(), outputItem.getMin());
                    }
                    if (outputItem.getWeight() != null && outputItem.getWeight().signum() > 0){
                        persistent.getInput().getMaximizedOutputItems().put(outputItem.getItem().getName(), outputItem.getWeight());
                    }
                }
            }
        });

        for (String s : persistent.getInput().getSettings().getOptimizationTargets()){
            OptimizationTargetModel otm = null;
            for (OptimizationTargetModel tmp : OptimizationTargetModel.values()){
                if (tmp.getSaveCode().equals(s)){
                    otm = tmp;
                    break;
                }
            }
            if (otm != null){
                model.getSettings().getOptimizationTargets().add(otm);
            }
        }
        model.getSettings().getOptimizationTargets().addListener((ListChangeListener<OptimizationTargetModel>) c -> {
            persistent.getInput().getSettings().getOptimizationTargets().clear();
            for (OptimizationTargetModel m : model.getSettings().getOptimizationTargets()){
                persistent.getInput().getSettings().getOptimizationTargets().add(m.getSaveCode());
            }
        });

        return model;
    }

    public ObservableSet<Recipe> getEnabledRecipes()
    {
        return enabledRecipes;
    }

    public ObservableList<InputItem> getInputItems()
    {
        return inputItems;
    }

    public MultiPlan getMultiPlan()
    {
        return multiPlan.get();
    }

    public void setMultiPlan(MultiPlan multiPlan)
    {
        this.multiPlan.set(multiPlan);
    }

    public String getName()
    {
        return name.get();
    }

    public void setName(String name)
    {
        this.name.set(name);
    }

    public ObservableList<OutputItem> getOutputItems()
    {
        return outputItems;
    }

    public ProductionPlan getPlan()
    {
        return plan.get();
    }

    public void setPlan(ProductionPlan plan)
    {
        this.plan.set(plan);
    }

    public Settings getSettings()
    {
        return settings;
    }

    public void initDefaults(GameData gameData)
    {
        getEnabledRecipes().addAll(gameData.getRecipes());
    }

    public ObjectProperty<MultiPlan> multiPlanProperty()
    {
        return multiPlan;
    }

    public StringProperty nameProperty()
    {
        return name;
    }

    public ObjectProperty<ProductionPlan> planProperty()
    {
        return plan;
    }

    public static class InputItem
    {
        private final ObjectProperty<Item> item = new SimpleObjectProperty<>();
        private final ObjectProperty<BigDecimal> amount = new SimpleObjectProperty<>();

        public InputItem(Item item, BigDecimal amount)
        {
            this.item.set(item);
            this.amount.set(amount);
        }

        public ObjectProperty<BigDecimal> amountProperty()
        {
            return amount;
        }

        public BigDecimal getAmount()
        {
            return amount.get();
        }

        public void setAmount(BigDecimal amount)
        {
            this.amount.set(amount);
        }

        public Item getItem()
        {
            return item.get();
        }

        public void setItem(Item item)
        {
            this.item.set(item);
        }

        public ObjectProperty<Item> itemProperty()
        {
            return item;
        }
    }

    public static class OutputItem
    {
        private final ObjectProperty<Item> item = new SimpleObjectProperty<>();
        private final ObjectProperty<BigDecimal> min = new SimpleObjectProperty<>();
        private final ObjectProperty<BigDecimal> weight = new SimpleObjectProperty<>();

        public OutputItem(Item item, BigDecimal min, BigDecimal weight)
        {
            this.item.set(item);
            this.min.set(min);
            this.weight.set(weight);
        }

        public Item getItem()
        {
            return item.get();
        }

        public void setItem(Item item)
        {
            this.item.set(item);
        }

        public BigDecimal getMin()
        {
            return min.get();
        }

        public void setMin(BigDecimal min)
        {
            this.min.set(min);
        }

        public BigDecimal getWeight()
        {
            return weight.get();
        }

        public void setWeight(BigDecimal weight)
        {
            this.weight.set(weight);
        }

        public ObjectProperty<Item> itemProperty()
        {
            return item;
        }

        public ObjectProperty<BigDecimal> minProperty()
        {
            return min;
        }

        public ObjectProperty<BigDecimal> weightProperty()
        {
            return weight;
        }
    }

    public static class Settings
    {
        private final ObservableList<OptimizationTargetModel> optimizationTargets = FXCollections.observableList(new ArrayList<>());

        public Settings()
        {
        }

        public ObservableList<OptimizationTargetModel> getOptimizationTargets()
        {
            return optimizationTargets;
        }
    }
}
