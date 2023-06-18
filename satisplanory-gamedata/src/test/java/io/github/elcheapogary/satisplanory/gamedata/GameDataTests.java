package io.github.elcheapogary.satisplanory.gamedata;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GameDataTests
{
    private static void checkGameData(GameData gameData)
    {
        for (String itemName : new String[]{"Iron Plate", "Iron Ore", "Crude Oil", "Wood", "Nitrogen Gas", "Portable Miner", "Water", "Uranium Waste", "Plutonium Fuel Rod"}){
            Assertions.assertTrue(gameData.getItemByName(itemName).isPresent(), "Failed to load item: " + itemName);
        }

        for (String buildingName : new String[]{"Smelter", "Constructor", "Assembler", "Particle Accelerator"}){
            Assertions.assertTrue(gameData.getManufacturerByName(buildingName).isPresent(), "Failed to load building: " + buildingName);
        }

        String[] recipes = new String[]{
                "Alternate: Fused Quickwire"
        };

        for (String recipeName : recipes){
            Assertions.assertTrue(gameData.getRecipeByName(recipeName).isPresent(), "Failed to load recipe: " + recipeName);
        }

        Assertions.assertFalse(gameData.getItemByName("Thermal Propulsion Rocket").orElseThrow(AssertionError::new).getSinkValue() < 1);
    }

    @Test
    public void testLoadUpdate7Data()
            throws IOException
    {
        checkGameData(GameData.loadUpdate7Data());
    }

    @Test
    public void testLoadUpdate8Data()
            throws IOException
    {
        checkGameData(GameData.loadUpdate8Data());
    }
}
