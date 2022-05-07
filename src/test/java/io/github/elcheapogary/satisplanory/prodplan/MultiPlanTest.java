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
import io.github.elcheapogary.satisplanory.model.test.TestGameData;
import io.github.elcheapogary.satisplanory.util.BigDecimalUtils;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiPlanTest
{
    @Test
    public void testMissingRecipe()
            throws ProductionPlanInternalException, InterruptedException, ProductionPlanNotFeatisbleException
    {
        TestGameData gameData = TestGameData.getUpdate5GameData();

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
    public void testMissingResources()
            throws ProductionPlanInternalException, InterruptedException, ProductionPlanNotFeatisbleException
    {
        TestGameData gameData = TestGameData.getUpdate5GameData();

        Assumptions.assumeFalse(gameData == null);

        ProductionPlanner planner = new ProductionPlanner.Builder()
                .addRecipe(gameData.requireRecipeByName("Iron Ingot"))
                .addRecipe(gameData.requireRecipeByName("Iron Plate"))
                .addOutputItem(gameData.requireItemByName("Iron Plate"), BigDecimal.ONE, BigDecimal.ONE)
                .build();

        MultiPlan multiPlan = ProdPlanUtils.getMultiPlan(gameData, planner);

        assertFalse(multiPlan.isUnmodifiedPlanFeasible());
        assertTrue(multiPlan.canCreatePlanByAddingResources());

        Map<Item, BigDecimal> missingResources = multiPlan.getMissingResources();

        assertEquals(1, missingResources.size());
        assertTrue(missingResources.containsKey(gameData.requireItemByName("Iron Ore")));
        assertEquals("1.5", BigDecimalUtils.normalize(missingResources.get(gameData.requireItemByName("Iron Ore"))).toString());

        assertFalse(multiPlan.canCreatePlanByAddingRecipes());
    }
}