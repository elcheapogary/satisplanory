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

import io.github.elcheapogary.satisplanory.model.Item;
import io.github.elcheapogary.satisplanory.model.Recipe;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

class ProdPlanData
{
    private final List<InputItem> inputItems = new LinkedList<>();
    private final List<OutputItem> outputItems = new LinkedList<>();
    private final Set<Recipe> enabledRecipes = new TreeSet<>(Comparator.comparing(Recipe::getName));

    public Settings settings = new Settings();

    public Set<Recipe> getEnabledRecipes()
    {
        return enabledRecipes;
    }

    public List<InputItem> getInputItems()
    {
        return inputItems;
    }

    public List<OutputItem> getOutputItems()
    {
        return outputItems;
    }

    public static class InputItem
    {
        private Item item;
        private BigDecimal amount;

        public InputItem(Item item, BigDecimal amount)
        {
            this.item = item;
            this.amount = amount;
        }

        public BigDecimal getAmount()
        {
            return amount;
        }

        public void setAmount(BigDecimal amount)
        {
            this.amount = amount;
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

    public static class Settings
    {
        public OptimizationWeights weights = new OptimizationWeights();

        public static class OptimizationWeights
        {
            public int balance = 15;
            public int maximizeOutputItems = 14;
            public int minimizeInputItems = 1;
            public int maximizeInputItems = 0;
            public int power = 0;
            public int inputItemScarcity;
            public int minimizeByProducts = 0;
        }
    }
}
