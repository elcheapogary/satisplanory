/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.satisfactory;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class SatisfactoryData
{
    private static final Map<String, Long> resourceExtractionLimits;

    static {
        {
            Map<String, Long> tmp = new TreeMap<>();

            tmp.put("Iron Ore", 70380L);
            tmp.put("Copper Ore", 28860L);
            tmp.put("Limestone", 52860L);
            tmp.put("Coal", 30900L);
            tmp.put("Caterium Ore", 11040L);
            tmp.put("Raw Quartz", 10500L);
            tmp.put("Sulfur", 6840L);
            tmp.put("Uranium", 2100L);
            tmp.put("Bauxite", 9780L);
            tmp.put("Crude Oil", 11700000L);
            tmp.put("Nitrogen Gas", 12000000L);

            resourceExtractionLimits = Collections.unmodifiableMap(tmp);
        }
    }

    private SatisfactoryData()
    {
    }

    public static Map<String, Long> getResourceExtractionLimits()
    {
        return resourceExtractionLimits;
    }
}
