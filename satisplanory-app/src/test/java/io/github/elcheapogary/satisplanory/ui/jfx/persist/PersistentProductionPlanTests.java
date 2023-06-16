/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.persist;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersistentProductionPlanTests
{
    @Test
    public void testInputSerialization()
    {
        PersistentProductionPlan.Input a = new PersistentProductionPlan.Input();
        a.getInputItems().put("a", "1");
        a.getInputItems().put("b", "2");
        a.getRecipes().getRecipeNames().add("recipe 1");
        a.getRecipes().getRecipeNames().add("recipe 2");
        a.getMaximizedOutputItems().put("c", "3");
        a.getMaximizedOutputItems().put("d", "4");
        a.getOutputItemsPerMinute().put("e", "5");
        a.getOutputItemsPerMinute().put("f", "6");
        a.getSettings().getOptimizationTargets().add("ZZ");

        PersistentProductionPlan.Input b = new PersistentProductionPlan.Input();
        b.loadJson_2_0(a.toJson());

        assertEquals(a.toJson().toString(), b.toJson().toString());

        assertEquals(2, b.getInputItems().size());
        assertEquals("1", b.getInputItems().get("a"));
        assertEquals("2", b.getInputItems().get("b"));

        assertEquals(2, b.getRecipes().getRecipeNames().size());
        assertTrue(b.getRecipes().getRecipeNames().contains("recipe 1"));
        assertTrue(b.getRecipes().getRecipeNames().contains("recipe 2"));

        assertEquals(2, b.getMaximizedOutputItems().size());
        assertEquals("3", b.getMaximizedOutputItems().get("c"));
        assertEquals("4", b.getMaximizedOutputItems().get("d"));

        assertEquals(2, b.getOutputItemsPerMinute().size());
        assertEquals("5", b.getOutputItemsPerMinute().get("e"));
        assertEquals("6", b.getOutputItemsPerMinute().get("f"));

        assertEquals(1, b.getSettings().getOptimizationTargets().size());
        assertEquals("ZZ", b.getSettings().getOptimizationTargets().get(0));
    }
}
