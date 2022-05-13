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
import io.github.elcheapogary.satisplanory.ui.jfx.persist.PersistentProductionPlan;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

class ProdPlanData
{
    public final PersistentProductionPlan.Input.Settings settings;
    private final PersistentProductionPlan persistentProductionPlan;
    private final ObservableList<InputItem> inputItems = FXCollections.observableList(new ArrayList<>());
    private final ObservableList<OutputItem> outputItems = FXCollections.observableList(new ArrayList<>());
    private final ObservableSet<Recipe> enabledRecipes = FXCollections.observableSet(new TreeSet<>(Comparator.comparing(Recipe::getName)));

    public ProdPlanData(GameData gameData, PersistentProductionPlan persistentProductionPlan)
    {
        this.persistentProductionPlan = persistentProductionPlan;
        this.settings = persistentProductionPlan.getInput().getSettings();

        for (String recipeName : persistentProductionPlan.getInput().getRecipes().getRecipeNames()){
            gameData.getRecipeByName(recipeName).ifPresent(enabledRecipes::add);
        }

        for (var entry : persistentProductionPlan.getInput().getInputItems().entrySet()){
            gameData.getItemByName(entry.getKey())
                    .ifPresent(item -> inputItems.add(new InputItem(item, entry.getValue())));
        }

        for (Item item : gameData.getItems()){
            BigDecimal min = persistentProductionPlan.getInput().getOutputItemsPerMinute().get(item.getName());
            BigDecimal weight = persistentProductionPlan.getInput().getMaximizedOutputItems().get(item.getName());

            if (min != null || weight != null){
                outputItems.add(new OutputItem(item, Objects.requireNonNullElse(min, BigDecimal.ZERO), Objects.requireNonNullElse(weight, BigDecimal.ZERO)));
            }
        }

        enabledRecipes.addListener((SetChangeListener<Recipe>) change -> {
            if (change.wasAdded()){
                persistentProductionPlan.getInput().getRecipes().getRecipeNames().add(change.getElementAdded().getName());
            }else if (change.wasRemoved()){
                persistentProductionPlan.getInput().getRecipes().getRecipeNames().remove(change.getElementRemoved().getName());
            }
        });
    }

    public Set<Recipe> getEnabledRecipes()
    {
        return enabledRecipes;
    }

    public ObservableList<InputItem> getInputItems()
    {
        return inputItems;
    }

    public List<OutputItem> getOutputItems()
    {
        return outputItems;
    }

    public PersistentProductionPlan getPersistentProductionPlan()
    {
        return persistentProductionPlan;
    }

    public static class InputItem
    {
        private Item item;
        private final ObjectProperty<BigDecimal> amount = new SimpleObjectProperty<>();

        public InputItem(Item item, BigDecimal amount)
        {
            this.item = item;
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
            return item;
        }

        public void setItem(Item item)
        {
            this.item = item;
        }
    }

    public static class OutputItem
    {
        private Item item;
        private BigDecimal min;
        private BigDecimal weight;

        public OutputItem(Item item, BigDecimal min, BigDecimal weight)
        {
            this.item = item;
            this.min = min;
            this.weight = weight;
        }

        public Item getItem()
        {
            return item;
        }

        public void setItem(Item item)
        {
            this.item = item;
        }

        public BigDecimal getMin()
        {
            return min;
        }

        public void setMin(BigDecimal min)
        {
            this.min = min;
        }

        public BigDecimal getWeight()
        {
            return weight;
        }

        public void setWeight(BigDecimal weight)
        {
            this.weight = weight;
        }
    }

}
