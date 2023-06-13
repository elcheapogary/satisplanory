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

import io.github.elcheapogary.satisplanory.model.test.TestGameData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class DocsJsonLoaderTest
{
    private static void checkGameData(TestGameData gameData)
    {
        for (String itemName : new String[]{"Iron Plate", "Iron Ore", "Crude Oil", "Wood", "Nitrogen Gas", "Portable Miner", "Water", "Uranium Waste", "Plutonium Fuel Rod"}){
            Assertions.assertTrue(gameData.getItemByName(itemName).isPresent(), "Failed to load item: " + itemName);
        }

        for (String buildingName : new String[]{"Smelter", "Constructor", "Assembler", "Particle Accelerator"}){
            Assertions.assertTrue(gameData.getBuildingByName(buildingName).isPresent(), "Failed to load building: " + buildingName);
        }

        String[] recipes = new String[]{
                "Alternate: Fused Quickwire"
        };

        for (String recipeName : recipes){
            Assertions.assertTrue(gameData.getRecipeByName(recipeName).isPresent(), "Failed to load recipe: " + recipeName);
        }

        Assertions.assertFalse(gameData.requireItemByName("Thermal Propulsion Rocket").getSinkValue() < 1);
    }

    @Test
    public void testLatestData()
    {
        TestGameData gd = TestGameData.getLatestTestData();
        Assumptions.assumeFalse(gd == null);
        checkGameData(gd);
    }

    @Test
    public void testUpdate7Data()
    {
        TestGameData gd = TestGameData.getUpdate7TestData();
        Assumptions.assumeFalse(gd == null);
        checkGameData(gd);
    }
}
