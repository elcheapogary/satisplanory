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

import io.github.elcheapogary.satisplanory.gamedata.Item;
import io.github.elcheapogary.satisplanory.gamedata.Recipe;
import io.github.elcheapogary.satisplanory.gamedata.test.TestGameData;
import io.github.elcheapogary.satisplanory.util.BigDecimalUtils;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiPlanTest
{

    private static void assertCanBeBuiltWithAllRawResourcesAndRecipes(TestGameData gameData, String itemName)
            throws ProductionPlanInternalException, ProductionPlanNotFeatisbleException, InterruptedException
    {
        ProductionPlanner planner = new ProductionPlanner.Builder()
                .addOutputItem(gameData.requireItemByName(itemName), BigDecimal.ONE, BigDecimal.ZERO)
                .build();

        MultiPlan multiPlan = ProdPlanUtils.getMultiPlan(gameData, planner);

        assertFalse(multiPlan.isUnmodifiedPlanFeasible());
        assertFalse(multiPlan.canCreatePlanByAddingResources());
        assertFalse(multiPlan.canCreatePlanByAddingRecipes());

        assertTrue(multiPlan.canCreatePlanByAddingResourcesAndRecipes());
    }

    @Test
    public void testMissingRecipe()
            throws ProductionPlanInternalException, InterruptedException, ProductionPlanNotFeatisbleException
    {
        TestGameData gameData = TestGameData.getLatestTestData();

        Assumptions.assumeFalse(gameData == null);

        ProductionPlanner planner = new ProductionPlanner.Builder()
                .addInputItem(gameData.requireItemByName("Iron Ore"), 60)
                .addRecipe(gameData.requireRecipeByName("Iron Ingot"))
                .addOutputItem(gameData.requireItemByName("Iron Plate"), BigDecimal.ONE, BigDecimal.ONE)
                .build();

        MultiPlan multiPlan = ProdPlanUtils.getMultiPlan(gameData, planner);

        assertFalse(multiPlan.isUnmodifiedPlanFeasible());
        assertTrue(multiPlan.canCreatePlanByAddingRecipes());

        Set<? extends Recipe> missingRecipes = multiPlan.getMissingRecipes();

        assertEquals(1, missingRecipes.size());
        assertTrue(missingRecipes.contains(gameData.requireRecipeByName("Iron Plate")));

        assertFalse(multiPlan.canCreatePlanByAddingResources());
    }

    @Test
    public void testMissingRecipeAndResources()
            throws ProductionPlanInternalException, ProductionPlanNotFeatisbleException, InterruptedException
    {
        TestGameData gameData = TestGameData.getLatestTestData();

        Assumptions.assumeFalse(gameData == null);

        assertCanBeBuiltWithAllRawResourcesAndRecipes(gameData, "Alclad Aluminum Sheet");
        assertCanBeBuiltWithAllRawResourcesAndRecipes(gameData, "Iron Ingot");
        assertCanBeBuiltWithAllRawResourcesAndRecipes(gameData, "Crystal Oscillator");
        assertCanBeBuiltWithAllRawResourcesAndRecipes(gameData, "Sulfuric Acid");
    }

    @Test
    public void testMissingResources()
            throws ProductionPlanInternalException, InterruptedException, ProductionPlanNotFeatisbleException
    {
        TestGameData gameData = TestGameData.getLatestTestData();

        Assumptions.assumeFalse(gameData == null);

        ProductionPlanner planner = new ProductionPlanner.Builder()
                .addRecipe(gameData.requireRecipeByName("Iron Ingot"))
                .addRecipe(gameData.requireRecipeByName("Iron Plate"))
                .addOutputItem(gameData.requireItemByName("Iron Plate"), BigDecimal.ONE, BigDecimal.ONE)
                .build();

        MultiPlan multiPlan = ProdPlanUtils.getMultiPlan(gameData, planner);

        assertFalse(multiPlan.isUnmodifiedPlanFeasible());
        assertTrue(multiPlan.canCreatePlanByAddingResources());

        Map<Item, BigFraction> missingResources = multiPlan.getMissingResources();

        assertEquals(1, missingResources.size());
        assertTrue(missingResources.containsKey(gameData.requireItemByName("Iron Ore")));
        assertEquals("1.5", BigDecimalUtils.normalize(missingResources.get(gameData.requireItemByName("Iron Ore")).toBigDecimal(4, RoundingMode.HALF_UP)).toString());

        assertFalse(multiPlan.canCreatePlanByAddingRecipes());
    }
}
