/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.prodplan;

import io.github.elcheapogary.satisplanory.prodplan.OptimizationTarget;
import java.util.List;
import java.util.function.BooleanSupplier;
import javafx.beans.value.ObservableBooleanValue;

enum OptimizationTargetModel
{
    MAXIMIZE_OUTPUT_ITEMS(
            "maximizeOutputItems",
            OptimizationTarget.MAX_OUTPUT_ITEMS,
            "Maximize Output Items",
            "Try create as much as possible of all output items with a weight > 0."
    ),
    MINIMIZE_INPUT_ITEMS(
            "minimizeInputItems",
            OptimizationTarget.MIN_INPUT_ITEMS,
            "Minimize Input Items",
            "Use as little as possible of the input items."
    ),
    MAXIMIZE_INPUT_ITEMS(
            "maximizeInputItems",
            OptimizationTarget.MAX_INPUT_ITEMS,
            "Maximize Input Items",
            "Use as much as possible of the input items."
    ),
    MINIMIZE_BY_PRODUCTS(
            "minimizeByProducts",
            OptimizationTarget.MIN_BYPRODUCTS,
            "Minimize By-Products",
            "Try create as little as possible of items that are not maximized output items (that do not "
                    + "have a weight configured in \"Output requirements\"). If you have output items with a "
                    + "configured minimum output per minute, the calculated plan will still produce that "
                    + "minimum amount.\n"
                    + "\n"
                    + "Use with \"" + MAXIMIZE_INPUT_ITEMS.title + "\" to ensure that only final products are "
                    + "produced, not intermediate items.\n"
                    + "\n"
                    + "Can also be used to avoid recipes that produce by-products."
    ),
    MINIMIZE_RESOURCE_SCARCITY(
            "minimizeResourceScarcity",
            OptimizationTarget.MIN_RESOURCE_SCARCITY,
            "Minimize Resource Scarcity",
            "Try use more abundant resources and less scarce when possible. Generate a plan that uses the "
                    + "smallest percentage of resources on the map."
    ),
    MINIMIZE_POWER_CONSUMPTION(
            "minimizePowerConsumption",
            OptimizationTarget.MIN_POWER,
            "Minimize Power Consumption",
            "Try and use as little power as possible."
    ),
    MINIMIZE_NUM_BUILDINGS(
            "minimizeNumberOfBuildings",
            OptimizationTarget.MIN_BUILDINGS,
            "Minimize Number of Buildings",
            "Try use as few buildings as possible."
    ),
    MAXIMIZE_SINK_POINTS(
            "maximizeSinkPoints",
            OptimizationTarget.MAX_SINK_POINTS,
            "Maximize AWESOME sink points",
            "Try generate items that can be sinked for as many AWESOME sink points as possible."
    );

    private final String saveCode;
    private final OptimizationTarget optimizationTarget;
    private final String title;
    private final String description;

    OptimizationTargetModel(String saveCode, OptimizationTarget optimizationTarget, String title, String description)
    {
        this.saveCode = saveCode;
        this.optimizationTarget = optimizationTarget;
        this.title = title;
        this.description = description;
    }

    public static OptimizationTargetModel forSaveCode(String saveCode)
    {
        for (OptimizationTargetModel target : values()){
            if (target.saveCode.equals(saveCode)){
                return target;
            }
        }
        return null;
    }

    public String getDescription()
    {
        return description;
    }

    public OptimizationTarget getOptimizationTarget()
    {
        return optimizationTarget;
    }

    public String getSaveCode()
    {
        return saveCode;
    }

    public String getTitle()
    {
        return title;
    }
}
