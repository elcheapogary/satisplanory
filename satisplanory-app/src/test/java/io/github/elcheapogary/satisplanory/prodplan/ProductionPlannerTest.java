/*
 * Copyright (c) 2023 elcheapogary
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
import java.math.RoundingMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProductionPlannerTest
{
    private static void assertOutputItems(ProductionPlan plan, TestGameData gameData, String itemName, long amount)
    {
        assertEquals(0, plan.getOutputItemsPerMinute(gameData.requireItemByName(itemName)).compareTo(BigFraction.valueOf(amount)), () -> "Incorrect number of output items: " + itemName + ": expected: " + amount + ", actual: " + plan.getOutputItemsPerMinute(gameData.requireItemByName(itemName)).toBigDecimal(4, RoundingMode.HALF_UP));
    }

    public static ProductionPlan createPlan(ProductionPlanner productionPlanner)
            throws ProductionPlanInternalException, ProductionPlanNotFeatisbleException, InterruptedException
    {
        return productionPlanner.createPlan();
    }

    @Test
    public void testBalanceIronPlatesAndRods()
            throws ProductionPlanNotFeatisbleException, InterruptedException, ProductionPlanInternalException
    {
        TestGameData gd = TestGameData.getLatestTestData();

        Assumptions.assumeFalse(gd == null);

        ProductionPlanner.Builder builder = new ProductionPlanner.Builder();

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
        TestGameData gd = TestGameData.getLatestTestData();

        Assumptions.assumeFalse(gd == null);

        ProductionPlanner.Builder pb = new ProductionPlanner.Builder();

        pb.addOptimizationTarget(OptimizationTarget.MAX_OUTPUT_ITEMS);

        Item plastic = gd.requireItemByName("Plastic");
        Item fuel = gd.requireItemByName("Fuel");
        Item crudeOil = gd.requireItemByName("Crude Oil");
        Item water = gd.requireItemByName("Water");

        pb.addInputItem(water, water.fromDisplayAmount(BigFraction.valueOf(999999999999L)));
        pb.addInputItem(crudeOil, crudeOil.fromDisplayAmount(BigFraction.valueOf(360)));

        pb.requireOutputItemsPerMinute(plastic, 20);
        pb.maximizeOutputItem(plastic, 1);
        pb.maximizeOutputItem(fuel, 1);

        pb.addRecipes(gd.getRecipes());

        ProductionPlan plan = pb.build().createPlan();

        BigFraction nPlastic = plan.getOutputItemsPerMinute(plastic);
        BigFraction nFuel = plan.getOutputItemsPerMinute(fuel);

        assertTrue(nPlastic.compareTo(BigFraction.valueOf(20)) > 0);
        assertEquals(plastic.toDisplayAmountFraction(nPlastic).subtract(20), fuel.toDisplayAmountFraction(nFuel));
    }

    @Test
    public void testCanProduceMoreThanInput()
            throws ProductionPlanInternalException, ProductionPlanNotFeatisbleException, InterruptedException
    {
        TestGameData gd = TestGameData.getLatestTestData();

        Assumptions.assumeFalse(gd == null);

        ProductionPlanner.Builder pb = new ProductionPlanner.Builder();

        pb.addOptimizationTarget(OptimizationTarget.MAX_OUTPUT_ITEMS);

        pb.addRecipe(gd.requireRecipeByName("Iron Ingot"));

        pb.addInputItem(gd.requireItemByName("Iron Ore"), 300);

        pb.addInputItem(gd.requireItemByName("Iron Ingot"), 60);

        pb.maximizeOutputItem(gd.requireItemByName("Iron Ingot"), 1);

        ProductionPlan plan = pb.build().createPlan();

        assertEquals(360L, plan.getOutputItemsPerMinute(gd.requireItemByName("Iron Ingot")).longValue());
    }

    @Test
    public void testErrorCase001()
            throws ProductionPlanInternalException, ProductionPlanNotFeatisbleException, InterruptedException
    {
        TestGameData gd = TestGameData.getLatestTestData();

        Assumptions.assumeFalse(gd == null);

        ProductionPlanner.Builder builder = new ProductionPlanner.Builder();

        builder.addRecipe(gd.requireRecipeByName("Iron Ingot"));
        builder.addRecipe(gd.requireRecipeByName("Iron Plate"));
        builder.addRecipe(gd.requireRecipeByName("Iron Rod"));
        builder.addRecipe(gd.requireRecipeByName("Screw"));
        builder.addRecipe(gd.requireRecipeByName("Alternate: Cast Screw"));
        builder.addRecipe(gd.requireRecipeByName("Reinforced Iron Plate"));

        builder.addInputItem(gd.requireItemByName("Iron Ore"), 111);

        builder.addOutputItem(gd.requireItemByName("Reinforced Iron Plate"), BigDecimal.ONE, BigDecimal.ONE);

        builder.addOptimizationTarget(OptimizationTarget.MAX_OUTPUT_ITEMS);
        builder.addOptimizationTarget(OptimizationTarget.MIN_RESOURCE_SCARCITY);
        builder.addOptimizationTarget(OptimizationTarget.MIN_BYPRODUCTS);
        builder.addOptimizationTarget(OptimizationTarget.MIN_POWER);
        builder.addOptimizationTarget(OptimizationTarget.MIN_INPUT_ITEMS);

        MultiPlan multiPlan = ProdPlanUtils.getMultiPlan(gd, builder.build());

        assertTrue(multiPlan.isUnmodifiedPlanFeasible());

        ProductionPlan plan = multiPlan.getUnmodifiedPlan();

        assertNotNull(plan);
    }

    @Test
    public void testInfeasiblePlan()
    {
        TestGameData gd = TestGameData.getLatestTestData();

        Assumptions.assumeFalse(gd == null);

        ProductionPlanner.Builder builder = new ProductionPlanner.Builder();

        builder.requireOutputItemsPerMinute(gd.requireItemByName("AI Limiter"), 50);

        assertThrows(ProductionPlanNotFeatisbleException.class, () -> builder.build().createPlan());
    }

    /*
     * We should not create plans with surplus fluid, as fluids cannot be sinked.
     */
    @Test
    public void testNoFluidByProducts() throws ProductionPlanInternalException, ProductionPlanNotFeatisbleException, InterruptedException
    {
        TestGameData gd = TestGameData.getLatestTestData();

        Assumptions.assumeFalse(gd == null);

        ProductionPlanner.Builder builder = new ProductionPlanner.Builder();

        builder.addOptimizationTarget(OptimizationTarget.MAX_OUTPUT_ITEMS);
        builder.addOptimizationTarget(OptimizationTarget.MIN_RESOURCE_SCARCITY);
        builder.addRecipes(gd.getRecipes());

        builder.addInputItem(gd.requireItemByName("Packaged Water"), 1);
        builder.addOutputItem(gd.requireItemByName("Empty Canister"), BigFraction.one(), BigFraction.zero());

        /*
         * The only way to generate the required empty canisters is by unpackaging water, resulting in surplus water.
         * The plan should fail.
         */
        Assertions.assertThrows(ProductionPlanNotFeatisbleException.class, () -> createPlan(builder.build()));

        /*
         * If we add limestone, we are then able to convert the excess water into concrete using the wet concrete
         * recipe. Concrete, being sinkable, is an acceptable by-product, so the plan should succeed.
         */
        builder.addInputItem(gd.requireItemByName("Limestone"), BigFraction.valueOf(100));

        ProductionPlan plan = createPlan(builder.build());

        Assertions.assertFalse(plan.getOutputItems().contains(gd.requireItemByName("Water")));
    }
}
