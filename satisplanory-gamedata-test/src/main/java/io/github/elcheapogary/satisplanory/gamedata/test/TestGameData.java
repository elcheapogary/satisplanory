/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.gamedata.test;

import io.github.elcheapogary.satisplanory.gamedata.GameData;
import io.github.elcheapogary.satisplanory.gamedata.Item;
import io.github.elcheapogary.satisplanory.gamedata.Recipe;
import java.io.IOException;

public class TestGameData
        extends GameData
{
    private static TestGameData update7GameData = null;
    private static TestGameData update8GameData = null;

    private TestGameData(AbstractBuilder<?> builder)
    {
        super(builder);
    }

    public static TestGameData getLatestTestData()
    {
        return getUpdate8TestData();
    }

    public static TestGameData getUpdate7TestData()
    {
        synchronized (TestGameData.class){
            if (update7GameData == null){
                try{
                    update7GameData = (TestGameData)GameData.loadUpdate7Data(new TestGameDataBuilder());
                }catch (IOException e){
                    throw new AssertionError(e);
                }
            }

            return update7GameData;
        }
    }

    public static TestGameData getUpdate8TestData()
    {
        synchronized (TestGameData.class){
            if (update8GameData == null){
                try{
                    update8GameData = (TestGameData)GameData.loadUpdate8Data(new TestGameDataBuilder());
                }catch (IOException e){
                    throw new AssertionError(e);
                }
            }

            return update8GameData;
        }
    }

    public Item requireItemByName(String itemName)
    {
        return getItemByName(itemName)
                .orElseThrow(() -> new RuntimeException("Unable to find item by name: " + itemName));
    }

    public Recipe requireRecipeByName(String recipeName)
    {
        return getRecipeByName(recipeName)
                .orElseThrow(() -> new RuntimeException("Unable to find recipe by name: " + recipeName));
    }

    private static class TestGameDataBuilder
            extends AbstractBuilder<TestGameDataBuilder>
    {
        @Override
        public TestGameData build()
        {
            return new TestGameData(this);
        }

        @Override
        protected TestGameDataBuilder self()
        {
            return this;
        }
    }
}
