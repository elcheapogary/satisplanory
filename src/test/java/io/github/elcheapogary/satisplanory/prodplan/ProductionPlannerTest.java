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
import io.github.elcheapogary.satisplanory.model.test.TestGameData;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProductionPlannerTest
{
    public static ProductionPlan createPlan(ProductionPlanner productionPlanner)
            throws ProductionPlanInternalException, ProductionPlanNotFeatisbleException, InterruptedException
    {
        return productionPlanner.createPlan();
    }

    private static void assertOutputItems(ProductionPlan plan, TestGameData gameData, String itemName, long amount)
    {
        assertEquals(0, plan.getOutputItemsPerMinute(gameData.requireItemByName(itemName)).compareTo(BigFraction.valueOf(amount)));
    }

    @Test
    public void testBalanceIronPlatesAndRods()
            throws ProductionPlanNotFeatisbleException, InterruptedException, ProductionPlanInternalException
    {
        TestGameData gd = TestGameData.getUpdate5GameData();

        Assumptions.assumeFalse(gd == null);

        ProductionPlanner.Builder builder = new ProductionPlanner.Builder();

        builder.addOptimizationTarget(OptimizationTarget.MAX_OUTPUT_ITEM_BALANCE);
        builder.addOptimizationTarget(OptimizationTarget.MAX_OUTPUT_ITEMS);

        builder.addInputItem(gd.requireItemByName("Iron Ingot"), 50);

        builder.addOutputItem(gd.requireItemByName("Iron Rod"), null, BigDecimal.ONE);
        builder.addOutputItem(gd.requireItemByName("Iron Plate"), null, BigDecimal.ONE);

        builder.addRecipes(gd.getRecipes());

        ProductionPlan plan = createPlan(builder.build());

        assertOutputItems(plan, gd, "Iron Plate", 20);
        assertOutputItems(plan, gd, "Iron Rod", 20);
    }

    @Test
    public void testBalancePlasticAndFuel()
            throws ProductionPlanInternalException, ProductionPlanNotFeatisbleException, InterruptedException
    {
        TestGameData gd = TestGameData.getUpdate5GameData();

        Assumptions.assumeFalse(gd == null);

        ProductionPlanner.Builder pb = new ProductionPlanner.Builder();

        pb.addOptimizationTarget(OptimizationTarget.MAX_OUTPUT_ITEM_BALANCE)
                .addOptimizationTarget(OptimizationTarget.MAX_OUTPUT_ITEMS);

        Item plastic = gd.requireItemByName("Plastic");
        Item fuel = gd.requireItemByName("Fuel");
        Item crudeOil = gd.requireItemByName("Crude Oil");
        Item water = gd.requireItemByName("Water");

        pb.addInputItem(water, water.fromDisplayAmount(BigDecimal.valueOf(999999999999L)));
        pb.addInputItem(crudeOil, crudeOil.fromDisplayAmount(BigDecimal.valueOf(360)));

        pb.requireOutputItemsPerMinute(plastic, 20);
        pb.maximizeOutputItem(plastic, 1);
        pb.maximizeOutputItem(fuel, 1);

        pb.addRecipes(gd.getRecipes());

        ProductionPlan plan = pb.build().createPlan();

        BigFraction nPlastic = plan.getOutputItemsPerMinute(plastic);
        BigFraction nFuel = plan.getOutputItemsPerMinute(fuel);

        assertTrue(nPlastic.compareTo(BigFraction.valueOf(20)) > 0);
        assertEquals(plastic.toDisplayAmount(nPlastic).subtract(20), fuel.toDisplayAmount(nFuel));
    }

    @Test
    public void testInfeasiblePlan()
    {
        TestGameData gd = TestGameData.getUpdate5GameData();

        Assumptions.assumeFalse(gd == null);

        ProductionPlanner.Builder builder = new ProductionPlanner.Builder();

        builder.requireOutputItemsPerMinute(gd.requireItemByName("AI Limiter"), 50);

        assertThrows(ProductionPlanNotFeatisbleException.class, () -> builder.build().createPlan());
    }
}
