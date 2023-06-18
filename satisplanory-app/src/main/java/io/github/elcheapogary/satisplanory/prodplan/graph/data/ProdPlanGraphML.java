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
import io.github.elcheapogary.satisplanory.prodplan.graph.lib.Graph;
import io.github.elcheapogary.satisplanory.prodplan.graph.lib.GraphML;
import io.github.elcheapogary.satisplanory.util.BigDecimalUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.util.Map;
import java.util.TreeMap;

public class ProdPlanGraphML
{
    private ProdPlanGraphML()
    {
    }

    public static void export(OutputStream out, Graph<ProdPlanNodeData, ProdPlanEdgeData> graph)
            throws IOException
    {
        GraphML.export(
                out, graph,
                ProdPlanGraphML::getNodeDescription,
                null,
                ProdPlanGraphML::getNodeData,
                ProdPlanGraphML::getEdgeData
        );
    }

    private static Map<String, String> getEdgeData(ProdPlanEdgeData data)
    {
        Map<String, String> retv = new TreeMap<>();
        int id = 0;
        for (var entry : data.getItemAmountsMap().entrySet()){
            Item item = entry.getKey();
            retv.put("edge-item-array-name-" + id, item.getName());
            String amount = BigDecimalUtils.normalize(entry.getValue().toBigDecimal(4, RoundingMode.HALF_UP)).toString();
            retv.put("edge-item-array-amount-" + id, amount);
            retv.put("edge-item-amount" + item.getClassName(), amount);
            retv.put("edge-item-name" + item.getClassName(), item.getName());
            id++;
        }
        return retv;
    }

    private static Map<String, String> getNodeData(ProdPlanNodeData data)
    {
        Map<String, String> retv = new TreeMap<>();

        if (data instanceof RecipeNodeData d){
            retv.put("node-type", "recipe");
            retv.put("node-recipe-name", d.getRecipe().getName());
            retv.put("node-recipe-building-name", d.getRecipe().getManufacturer().getName());
            retv.put("node-recipe-amount", BigDecimalUtils.normalize(d.getAmount().toBigDecimal(6, RoundingMode.HALF_UP)).toString());
        }else if (data instanceof InputItemNodeData d){
            retv.put("node-type", "input");
            retv.put("node-item-name", d.getItem().getName());
            retv.put("node-item-class", d.getItem().getClassName());
            retv.put("node-item-amount", BigDecimalUtils.normalize(d.getAmount().toBigDecimal(4, RoundingMode.HALF_UP)).toString());
        }else if (data instanceof OutputItemNodeData d){
            retv.put("node-type", "output");
            retv.put("node-item-name", d.getItem().getName());
            retv.put("node-item-class", d.getItem().getClassName());
            retv.put("node-item-amount", BigDecimalUtils.normalize(d.getAmount().toBigDecimal(4, RoundingMode.HALF_UP)).toString());
        }
        return retv;
    }

    private static String getNodeDescription(ProdPlanNodeData data)
    {
        if (data instanceof InputItemNodeData d){
            return "Input: " + d.getItem().getName() + " " + BigDecimalUtils.normalize(d.getAmount().toBigDecimal(4, RoundingMode.HALF_UP)) + " / min";
        }else if (data instanceof OutputItemNodeData d){
            return "Output: " + d.getItem().getName() + " " + BigDecimalUtils.normalize(d.getAmount().toBigDecimal(4, RoundingMode.HALF_UP)) + " / min";
        }else if (data instanceof RecipeNodeData d){
            return d.getRecipe().getName() + " " + BigDecimalUtils.normalize(d.getAmount().toBigDecimal(6, RoundingMode.HALF_UP)) + " " + d.getRecipe().getManufacturer().getName();
        }else{
            return null;
        }
    }
}
