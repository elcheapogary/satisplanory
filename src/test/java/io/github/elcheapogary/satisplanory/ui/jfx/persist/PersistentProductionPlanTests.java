/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.persist;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersistentProductionPlanTests
{
    @Test
    public void testInputSerialization()
    {
        PersistentProductionPlan.Input a = new PersistentProductionPlan.Input();
        a.getInputItems().put("a", BigDecimal.valueOf(1));
        a.getInputItems().put("b", BigDecimal.valueOf(2));
        a.getRecipes().getRecipeNames().add("recipe 1");
        a.getRecipes().getRecipeNames().add("recipe 2");
        a.getMaximizedOutputItems().put("c", BigDecimal.valueOf(3));
        a.getMaximizedOutputItems().put("d", BigDecimal.valueOf(4));
        a.getOutputItemsPerMinute().put("e", BigDecimal.valueOf(5));
        a.getOutputItemsPerMinute().put("f", BigDecimal.valueOf(6));
        a.getSettings().getOptimizationTargets().add("ZZ");

        PersistentProductionPlan.Input b = new PersistentProductionPlan.Input();
        b.loadJson_2_0(a.toJson());

        assertEquals(a.toJson().toString(), b.toJson().toString());

        assertEquals(2, b.getInputItems().size());
        assertEquals(BigDecimal.valueOf(1), b.getInputItems().get("a"));
        assertEquals(BigDecimal.valueOf(2), b.getInputItems().get("b"));

        assertEquals(2, b.getRecipes().getRecipeNames().size());
        assertTrue(b.getRecipes().getRecipeNames().contains("recipe 1"));
        assertTrue(b.getRecipes().getRecipeNames().contains("recipe 2"));

        assertEquals(2, b.getMaximizedOutputItems().size());
        assertEquals(BigDecimal.valueOf(3), b.getMaximizedOutputItems().get("c"));
        assertEquals(BigDecimal.valueOf(4), b.getMaximizedOutputItems().get("d"));

        assertEquals(2, b.getOutputItemsPerMinute().size());
        assertEquals(BigDecimal.valueOf(5), b.getOutputItemsPerMinute().get("e"));
        assertEquals(BigDecimal.valueOf(6), b.getOutputItemsPerMinute().get("f"));

        assertEquals(1, b.getSettings().getOptimizationTargets().size());
        assertEquals("ZZ", b.getSettings().getOptimizationTargets().get(0));
    }
}
