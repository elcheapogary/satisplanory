/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.prodplan.graph.data;

import io.github.elcheapogary.satisplanory.model.Recipe;
import io.github.elcheapogary.satisplanory.util.BigFraction;

public class RecipeNodeData
        extends ProdPlanNodeData
{
    private final Recipe recipe;
    private final BigFraction amount;

    public RecipeNodeData(Recipe recipe, BigFraction amount)
    {
        this.recipe = recipe;
        this.amount = amount;
    }

    public BigFraction getAmount()
    {
        return amount;
    }

    public Recipe getRecipe()
    {
        return recipe;
    }
}
