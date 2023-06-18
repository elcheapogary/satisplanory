/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.gamedata;

import java.math.BigDecimal;
import java.util.Objects;
import javax.json.Json;
import javax.json.JsonObject;

public class Manufacturer
{
    private final String name;
    private final String className;
    private final BigDecimal powerConsumption;

    protected Manufacturer(Builder builder)
    {
        this.name = Objects.requireNonNull(builder.name, "name");
        this.className = Objects.requireNonNull(builder.className, "className");
        this.powerConsumption = Objects.requireNonNull(builder.powerConsumption);
    }

    public static Manufacturer fromJson(JsonObject json)
    {
        return new Manufacturer.Builder()
                .setName(json.getString("name"))
                .setClassName(json.getString("className"))
                .setPowerConsumption(new BigDecimal(json.getString("powerConsumption")))
                .build();
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

    public JsonObject toJson()
    {
        return Json.createObjectBuilder()
                .add("name", name)
                .add("className", className)
                .add("powerConsumption", powerConsumption.toString())
                .build();
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

        public Manufacturer build()
        {
            return new Manufacturer(this);
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
