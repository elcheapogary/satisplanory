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
import io.github.elcheapogary.satisplanory.prodplan.graph.data.InputItemNodeData;
import io.github.elcheapogary.satisplanory.prodplan.graph.data.OutputItemNodeData;
import io.github.elcheapogary.satisplanory.prodplan.graph.data.ProdPlanEdgeData;
import io.github.elcheapogary.satisplanory.prodplan.graph.data.ProdPlanNodeData;
import io.github.elcheapogary.satisplanory.prodplan.graph.data.RecipeNodeData;
import io.github.elcheapogary.satisplanory.prodplan.graph.lib.Graph;
import io.github.elcheapogary.satisplanory.prodplan.graph.lib.Node;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class ProductionPlan
{
    private final Map<Recipe, BigFraction> recipeAmounts;
    private final Map<Item, BigFraction> inputItemAmounts;
    private final Map<Item, BigFraction> outputItemAmounts;

    public ProductionPlan(Map<Recipe, BigFraction> recipeAmounts, Map<Item, BigFraction> inputItemAmounts, Map<Item, BigFraction> outputItemAmounts)
    {
        this.recipeAmounts = recipeAmounts;
        this.inputItemAmounts = inputItemAmounts;
        this.outputItemAmounts = outputItemAmounts;
    }

    public Collection<? extends Item> getInputItems()
    {
        return inputItemAmounts.keySet();
    }

    public BigFraction getInputItemsPerMinute(Item item)
    {
        return Objects.requireNonNullElse(inputItemAmounts.get(item), BigFraction.ZERO);
    }

    public BigFraction getNumberOfMachinesWithRecipe(Recipe recipe)
    {
        return Objects.requireNonNullElse(recipeAmounts.get(recipe), BigFraction.ZERO);
    }

    public Collection<? extends Item> getOutputItems()
    {
        return outputItemAmounts.keySet();
    }

    public BigFraction getOutputItemsPerMinute(Item item)
    {
        return Objects.requireNonNullElse(outputItemAmounts.get(item), BigFraction.ZERO);
    }

    public Collection<? extends Recipe> getRecipes()
    {
        return recipeAmounts.keySet();
    }

    public Graph<ProdPlanNodeData, ProdPlanEdgeData> toGraph()
    {
        Graph<ProdPlanNodeData, ProdPlanEdgeData> graph = new Graph<>();

        Map<Node<ProdPlanNodeData, ProdPlanEdgeData>, Map<Item, BigFraction>> wantsMap = new TreeMap<>(Comparator.comparing(Node::getName));
        Map<Item, Map<Node<ProdPlanNodeData, ProdPlanEdgeData>, BigFraction>> suppliesMap = new TreeMap<>(Comparator.comparing(Item::getName));

        for (Map.Entry<Recipe, BigFraction> entry : recipeAmounts.entrySet()){
            Recipe recipe = entry.getKey();
            BigFraction amount = entry.getValue();
            var node = graph.createNode(recipe.getName(), new RecipeNodeData(recipe, amount));
            Map<Item, BigFraction> requirementsMap = new TreeMap<>(Comparator.comparing(Item::getName));
            wantsMap.put(node, requirementsMap);
            for (Recipe.RecipeItem recipeItem : recipe.getIngredients()){
                requirementsMap.put(recipeItem.getItem(), recipeItem.getAmount().getAmountPerMinuteFraction().multiply(amount));
            }
            for (Recipe.RecipeItem recipeItem : recipe.getProducts()){
                suppliesMap.computeIfAbsent(recipeItem.getItem(), item1 -> new TreeMap<Node<ProdPlanNodeData, ProdPlanEdgeData>, BigFraction>(Comparator.comparing(Node::getName)))
                        .put(node, recipeItem.getAmount().getAmountPerMinuteFraction().multiply(amount));
            }
        }

        for (Map.Entry<Item, BigFraction> entry : inputItemAmounts.entrySet()){
            Item item = entry.getKey();
            BigFraction amount = entry.getValue();
            var node = graph.createNode("Input Item: " + item.getName(), new InputItemNodeData(item, amount));
            suppliesMap.computeIfAbsent(item, item1 -> new TreeMap<Node<ProdPlanNodeData, ProdPlanEdgeData>, BigFraction>(Comparator.comparing(Node::getName)))
                    .put(node, amount);
        }

        for (Map.Entry<Item, BigFraction> entry : outputItemAmounts.entrySet()){
            Item item = entry.getKey();
            BigFraction amount = entry.getValue();
            var node = graph.createNode("Output Item: " + item.getName(), new OutputItemNodeData(item, amount));
            Map<Item, BigFraction> requirementsMap = new TreeMap<>(Comparator.comparing(Item::getName));
            requirementsMap.put(item, amount);
            wantsMap.put(node, requirementsMap);
        }

        while (!wantsMap.isEmpty()){
            var node = wantsMap.keySet().iterator().next();

            var requirementsMap = wantsMap.remove(node);

            for (var entry : requirementsMap.entrySet()){
                Item item = entry.getKey();
                BigFraction amountRequired = entry.getValue();

                var suppliers = suppliesMap.get(item);

                if (suppliers != null){
                    for (var it = suppliers.entrySet().iterator(); it.hasNext(); ){
                        var supplierEntry = it.next();
                        var supplierNode = supplierEntry.getKey();
                        BigFraction supplierAmount = supplierEntry.getValue();

                        BigFraction amountToLink = supplierAmount.min(amountRequired);

                        supplierAmount = supplierAmount.subtract(amountToLink);

                        if (supplierAmount.signum() == 0){
                            it.remove();
                        }else{
                            supplierEntry.setValue(supplierAmount);
                        }

                        var edge = supplierNode.getEdgeTo(node);

                        if (edge == null){
                            edge = supplierNode.link(node, new ProdPlanEdgeData());
                        }

                        edge.getData().addItem(item, amountToLink);

                        amountRequired = amountRequired.subtract(amountToLink);

                        if (amountRequired.signum() == 0){
                            break;
                        }
                    }
                }
            }
        }

        return graph;
    }
}
