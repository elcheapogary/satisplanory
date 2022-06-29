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
import io.github.elcheapogary.satisplanory.prodplan.lp.Expression;
import io.github.elcheapogary.satisplanory.prodplan.lp.Model;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

class OptimizationModel
{
    private final Map<Item, ? extends Expression> itemInputMap;
    private final Map<Item, ? extends Expression> itemOutputMap;
    private final Map<Item, ? extends Expression> itemSurplusMap;
    private final Map<Recipe, ? extends Expression> recipeMap;
    private final Map<Item, ? extends BigFraction> itemMaximizeWeightMap;
    private final Model lpModel;

    protected OptimizationModel(Builder builder)
    {
        this.itemInputMap = Collections.unmodifiableMap(builder.itemInputMap);
        this.itemOutputMap = Collections.unmodifiableMap(builder.itemOutputMap);
        this.itemSurplusMap = Collections.unmodifiableMap(builder.itemSurplusMap);
        this.recipeMap = Collections.unmodifiableMap(builder.recipeMap);
        this.itemMaximizeWeightMap = Collections.unmodifiableMap(builder.itemMaximizeWeightMap);
        this.lpModel = Objects.requireNonNull(builder.lpModel);
    }

    public Map<Item, ? extends Expression> getItemInputMap()
    {
        return itemInputMap;
    }

    public Map<Item, ? extends BigFraction> getItemMaximizeWeightMap()
    {
        return itemMaximizeWeightMap;
    }

    public Map<Item, ? extends Expression> getItemOutputMap()
    {
        return itemOutputMap;
    }

    public Map<Item, ? extends Expression> getItemSurplusMap()
    {
        return itemSurplusMap;
    }

    public Model getLpModel()
    {
        return lpModel;
    }

    public Map<Recipe, ? extends Expression> getRecipeMap()
    {
        return recipeMap;
    }

    static class Builder
    {
        private Map<Item, ? extends Expression> itemInputMap;
        private Map<Item, ? extends Expression> itemOutputMap;
        private Map<Item, ? extends Expression> itemSurplusMap;
        private Map<Recipe, ? extends Expression> recipeMap;
        private Map<Item, ? extends BigFraction> itemMaximizeWeightMap;
        private Model lpModel;

        public OptimizationModel build()
        {
            return new OptimizationModel(this);
        }

        public Builder setItemInputMap(Map<Item, ? extends Expression> itemInputMap)
        {
            this.itemInputMap = itemInputMap;
            return this;
        }

        public Builder setItemMaximizeWeightMap(Map<Item, ? extends BigFraction> itemMaximizeWeightMap)
        {
            this.itemMaximizeWeightMap = itemMaximizeWeightMap;
            return this;
        }

        public Builder setItemOutputMap(Map<Item, ? extends Expression> itemOutputMap)
        {
            this.itemOutputMap = itemOutputMap;
            return this;
        }

        public Builder setItemSurplusMap(Map<Item, ? extends Expression> itemSurplusMap)
        {
            this.itemSurplusMap = itemSurplusMap;
            return this;
        }

        public Builder setLpModel(Model lpModel)
        {
            this.lpModel = lpModel;
            return this;
        }

        public Builder setRecipeMap(Map<Recipe, ? extends Expression> recipeMap)
        {
            this.recipeMap = recipeMap;
            return this;
        }
    }
}
