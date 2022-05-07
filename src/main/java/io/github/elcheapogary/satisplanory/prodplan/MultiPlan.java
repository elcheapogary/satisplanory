/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.prodplan;

import io.github.elcheapogary.satisplanory.model.Item;
import io.github.elcheapogary.satisplanory.model.Recipe;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MultiPlan
{
    private final ProductionPlanner productionPlanner;
    private final ProductionPlan unmodifiedPlan;
    private final ProductionPlan planWithAllItems;
    private final ProductionPlan planWithAllRecipes;
    private final ProductionPlan planWithAllItemsAndRecipes;

    MultiPlan(ProductionPlanner productionPlanner, ProductionPlan unmodifiedPlan, ProductionPlan planWithAllItems, ProductionPlan planWithAllRecipes, ProductionPlan planWithAllItemsAndRecipes)
    {
        this.productionPlanner = productionPlanner;
        this.unmodifiedPlan = unmodifiedPlan;
        this.planWithAllItems = planWithAllItems;
        this.planWithAllRecipes = planWithAllRecipes;
        this.planWithAllItemsAndRecipes = planWithAllItemsAndRecipes;
    }

    public boolean canCreatePlanByAddingRecipes()
    {
        return planWithAllRecipes != null;
    }

    public boolean canCreatePlanByAddingResources()
    {
        return planWithAllItems != null;
    }

    public boolean canCreatePlanByAddingResourcesAndRecipes()
    {
        return planWithAllItemsAndRecipes != null;
    }

    public Set<? extends Recipe> getMissingRecipes()
    {
        ProductionPlan plan = Objects.requireNonNullElse(planWithAllRecipes, planWithAllItemsAndRecipes);

        Set<Recipe> retv = Recipe.createSet();

        retv.addAll(plan.getRecipes());
        retv.removeAll(productionPlanner.getRecipes());

        return Collections.unmodifiableSet(retv);
    }

    public Map<Item, BigDecimal> getMissingResources()
    {
        ProductionPlan plan = Objects.requireNonNullElse(planWithAllItems, planWithAllItemsAndRecipes);

        Map<Item, BigDecimal> retv = Item.createMap();

        for (Item item : plan.getInputItems()){
            BigDecimal requiredAmount = plan.getInputItemsPerMinute(item).toBigDecimal(4, RoundingMode.HALF_UP);

            requiredAmount = requiredAmount.subtract(Objects.requireNonNullElse(productionPlanner.getInputItems().get(item), BigDecimal.ZERO));

            retv.put(item, requiredAmount);
        }

        return Collections.unmodifiableMap(retv);
    }

    public ProductionPlan getPlanWithAllItems()
    {
        return planWithAllItems;
    }

    public ProductionPlan getPlanWithAllItemsAndRecipes()
    {
        return planWithAllItemsAndRecipes;
    }

    public ProductionPlan getPlanWithAllRecipes()
    {
        return planWithAllRecipes;
    }

    public ProductionPlanner getProductionPlanner()
    {
        return productionPlanner;
    }

    public ProductionPlan getUnmodifiedPlan()
    {
        return unmodifiedPlan;
    }

    public boolean isUnmodifiedPlanFeasible()
    {
        return unmodifiedPlan != null;
    }
}
