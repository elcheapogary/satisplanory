/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.model;

import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Item
{
    private static final Comparator<Item> COMPARATOR = Comparator.comparing(Item::getName);
    private final String className;
    private final String name;
    private final String description;
    private final MatterState matterState;
    private final int sinkValue;

    protected Item(AbstractBuilder<?> builder)
    {
        this.className = Objects.requireNonNull(builder.className, "item requires a className");
        this.name = Objects.requireNonNull(builder.name, "item requires a name");
        this.description = Objects.requireNonNull(builder.description, "item requires a description");
        this.matterState = Objects.requireNonNull(builder.matterState, "item requires a matterState");
        this.sinkValue = builder.sinkValue;
    }

    public static <V> Map<Item, V> createMap()
    {
        return new TreeMap<>(COMPARATOR);
    }

    public static <V> Map<Item, V> createMap(Map<Item, V> map)
    {
        Map<Item, V> retv = createMap();
        retv.putAll(map);
        return retv;
    }

    public static Set<Item> createSet()
    {
        return new TreeSet<>(COMPARATOR);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return className.equals(item.className) && name.equals(item.name) && matterState == item.matterState;
    }

    public BigDecimal fromDisplayAmount(BigDecimal amount)
    {
        return matterState.fromDisplayAmount(amount);
    }

    public BigFraction fromDisplayAmount(BigFraction amount)
    {
        return matterState.fromDisplayAmount(amount);
    }

    public String getClassName()
    {
        return className;
    }

    public String getDescription()
    {
        return description;
    }

    public MatterState getMatterState()
    {
        return matterState;
    }

    public String getName()
    {
        return name;
    }

    public int getSinkValue()
    {
        return sinkValue;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(className, name, matterState);
    }

    public BigDecimal toDisplayAmount(BigDecimal amount)
    {
        return matterState.toDisplayAmount(amount);
    }

    public BigFraction toDisplayAmount(BigFraction amount)
    {
        return matterState.toDisplayAmount(amount);
    }

    @Override
    public String toString()
    {
        return "Item{" +
                "className='" + className + '\'' +
                ", name='" + name + '\'' +
                ", matterState=" + matterState +
                '}';
    }

    protected static abstract class AbstractBuilder<B extends AbstractBuilder<B>>
    {
        private String name;
        private String className;
        private String description;
        private MatterState matterState;
        private int sinkValue;

        protected abstract B self();

        public B setClassName(String className)
        {
            this.className = className;
            return self();
        }

        public B setDescription(String description)
        {
            this.description = description;
            return self();
        }

        public B setMatterState(MatterState matterState)
        {
            this.matterState = matterState;
            return self();
        }

        public B setName(String name)
        {
            this.name = name;
            return self();
        }

        public B setSinkValue(int sinkValue)
        {
            this.sinkValue = sinkValue;
            return self();
        }
    }

    public static class Builder
            extends AbstractBuilder<Builder>
    {
        public Item build()
        {
            return new Item(this);
        }

        @Override
        protected Builder self()
        {
            return this;
        }
    }
}
