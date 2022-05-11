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

public enum OptimizationTarget
{
    MAX_OUTPUT_ITEMS("Maximize Output Items"),
    MIN_POWER("Minimize Power Consumption"),
    MIN_INPUT_ITEMS("Minimize Input Items");

    private final String title;

    OptimizationTarget(String title)
    {
        this.title = title;
    }

    @Override
    public String toString()
    {
        return title;
    }
}
