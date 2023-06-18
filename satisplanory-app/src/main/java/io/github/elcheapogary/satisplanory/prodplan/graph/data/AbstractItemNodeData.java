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

import io.github.elcheapogary.satisplanory.gamedata.Item;
import io.github.elcheapogary.satisplanory.util.BigFraction;

public abstract class AbstractItemNodeData
        extends ProdPlanNodeData
{
    private final Item item;
    private final BigFraction amount;

    public AbstractItemNodeData(Item item, BigFraction amount)
    {
        this.item = item;
        this.amount = amount;
    }

    public BigFraction getAmount()
    {
        return amount;
    }

    public Item getItem()
    {
        return item;
    }
}
