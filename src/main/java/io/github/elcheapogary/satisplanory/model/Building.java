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

import java.math.BigDecimal;
import java.util.Objects;

public class Building
{
    private final String name;
    private final String className;
    private final BigDecimal powerConsumption;

    protected Building(Builder builder)
    {
        this.name = Objects.requireNonNull(builder.name, "building requires name");
        this.className = Objects.requireNonNull(builder.className, "building requires className");
        this.powerConsumption = Objects.requireNonNull(builder.powerConsumption);
    }

    public String getClassName()
    {
        return className;
    }

    public String getName()
    {
        return name;
    }

    public BigDecimal getPowerConsumption()
    {
        return powerConsumption;
    }

    @Override
    public String toString()
    {
        return "Building{" +
                "name='" + name + '\'' +
                ", className='" + className + '\'' +
                '}';
    }

    public static class Builder
    {
        private String name;
        private String className;
        private BigDecimal powerConsumption;

        public Building build()
        {
            return new Building(this);
        }

        public Builder setClassName(String className)
        {
            this.className = className;
            return this;
        }

        public Builder setName(String name)
        {
            this.name = name;
            return this;
        }

        public Builder setPowerConsumption(BigDecimal powerConsumption)
        {
            this.powerConsumption = powerConsumption;
            return this;
        }
    }
}
