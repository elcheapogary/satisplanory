/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.prodplan.graph.data;

import io.github.elcheapogary.satisplanory.model.Item;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class ProdPlanEdgeData
{
    private final Map<Item, BigFraction> itemAmountsMap = new TreeMap<>(Comparator.comparing(Item::getName));

    public void addItem(Item item, BigFraction amount)
    {
        itemAmountsMap.compute(item, (item1, existingAmount) ->
                Objects.requireNonNullElse(existingAmount, BigFraction.zero()).add(amount)
        );
    }

    public Map<Item, BigFraction> getItemAmountsMap()
    {
        return itemAmountsMap;
    }
}
