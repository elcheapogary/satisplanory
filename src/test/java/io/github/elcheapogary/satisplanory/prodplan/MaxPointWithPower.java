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
import io.github.elcheapogary.satisplanory.model.MatterState;
import io.github.elcheapogary.satisplanory.model.Recipe;
import io.github.elcheapogary.satisplanory.model.test.TestGameData;
import io.github.elcheapogary.satisplanory.prodplan.lp.FractionExpression;
import io.github.elcheapogary.satisplanory.prodplan.lp.InfeasibleSolutionException;
import io.github.elcheapogary.satisplanory.prodplan.lp.Model;
import io.github.elcheapogary.satisplanory.prodplan.lp.OptimizationResult;
import io.github.elcheapogary.satisplanory.prodplan.lp.UnboundedSolutionException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MaxPointWithPower
{
    public static void main(String[] args)
            throws UnboundedSolutionException, InfeasibleSolutionException, InterruptedException
    {
        TestGameData gd = TestGameData.getUpdate5GameData();

        Model model = new Model();

        Map<Item, FractionExpression> itemExpressionMap = Item.createMap();
        for (Item item : gd.getItems()){
            itemExpressionMap.put(item, FractionExpression.zero());
        }

        FractionExpression powerExpression = FractionExpression.constant(4500); // add 4500MW from geothermal

        powerExpression = addGenerator(
                model,
                "Generator: Coal",
                75,
                gd.requireItemByName("Coal"),
                BigDecimal.valueOf(15),
                itemExpressionMap,
                powerExpression
        );
        powerExpression = addGenerator(
                model,
                "Generator: Coal",
                75,
                gd.requireItemByName("Compacted Coal"),
                BigDecimal.valueOf(60).divide(BigDecimal.valueOf(8.4), 10, RoundingMode.HALF_UP),
                itemExpressionMap,
                powerExpression
        );
        powerExpression = addGenerator(
                model,
                "Generator: Coal",
                75,
                gd.requireItemByName("Petroleum Coke"),
                BigDecimal.valueOf(25),
                itemExpressionMap,
                powerExpression
        );
        powerExpression = addGenerator(
                model,
                "Generator: Fuel",
                150,
                gd.requireItemByName("Fuel"),
                BigDecimal.valueOf(12000),
                itemExpressionMap,
                powerExpression
        );
        powerExpression = addGenerator(
                model,
                "Generator: Fuel",
                150,
                gd.requireItemByName("Liquid Biofuel"),
                BigDecimal.valueOf(12000),
                itemExpressionMap,
                powerExpression
        );
        powerExpression = addGenerator(
                model,
                "Generator: Fuel",
                150,
                gd.requireItemByName("Turbofuel"),
                BigDecimal.valueOf(4500),
                itemExpressionMap,
                powerExpression
        );

        Collection<Recipe> recipes = gd.getRecipes();

        {
            FractionExpression nGenerators = model.addFractionVariable();
            model.addConstraint(nGenerators.nonNegative());
            powerExpression = powerExpression.add(2500, nGenerators);
            itemExpressionMap.compute(gd.requireItemByName("Water"), (item, expression) -> Objects.requireNonNullElseGet(expression, FractionExpression::zero)
                    .subtract(300000, nGenerators));
            itemExpressionMap.compute(gd.requireItemByName("Uranium Fuel Rod"), (item, expression) -> Objects.requireNonNullElseGet(expression, FractionExpression::zero)
                    .subtract(BigDecimal.valueOf(0.2), nGenerators));
            itemExpressionMap.compute(gd.requireItemByName("Uranium Waste"), (item, expression) -> Objects.requireNonNullElseGet(expression, FractionExpression::zero)
                    .add(BigDecimal.valueOf(10), nGenerators));
        }

        if (1 == 0){
            FractionExpression nGenerators = model.addFractionVariable();
            model.addConstraint(nGenerators.nonNegative());
            powerExpression = powerExpression.add(2500, nGenerators);
            itemExpressionMap.compute(gd.requireItemByName("Water"), (item, expression) -> Objects.requireNonNullElseGet(expression, FractionExpression::zero)
                    .subtract(300000, nGenerators));
            itemExpressionMap.compute(gd.requireItemByName("Plutonium Fuel Rod"), (item, expression) -> Objects.requireNonNullElseGet(expression, FractionExpression::zero)
                    .subtract(BigDecimal.valueOf(0.1), nGenerators));
        }

        FractionExpression pointsExpression = FractionExpression.zero();

        for (Recipe recipe : recipes){
            FractionExpression numberOfBuildings = model.addFractionVariable();
            model.addConstraint(numberOfBuildings.nonNegative());

            powerExpression = powerExpression.subtract(recipe.getPowerConsumption(), numberOfBuildings);

            for (Recipe.RecipeItem ri : recipe.getIngredients()){
                itemExpressionMap.compute(ri.getItem(), (item, expression) ->
                        Objects.requireNonNullElse(expression, FractionExpression.zero())
                                .subtract(ri.getAmount().getAmountPerMinute(), numberOfBuildings)
                );
            }
            for (Recipe.RecipeItem ri : recipe.getProducts()){
                itemExpressionMap.compute(ri.getItem(), (item, expression) ->
                        Objects.requireNonNullElse(expression, FractionExpression.zero())
                                .add(ri.getAmount().getAmountPerMinute(), numberOfBuildings)
                );
            }
        }

        {
            List<Resource> resources = new LinkedList<>();

            resources.add(new Resource("Iron Ore", 33, 41, 46));
            resources.add(new Resource("Copper Ore", 9, 28, 12));
            resources.add(new Resource("Limestone", 12, 47, 27));
            resources.add(new Resource("Coal", 6, 29, 15));
            resources.add(new Resource("Caterium Ore", 0, 8, 8));
            resources.add(new Resource("Raw Quartz", 0, 11, 5));
            resources.add(new Resource("Sulfur", 1, 7, 3));
            resources.add(new Resource("Uranium", 1, 3, 0));
            resources.add(new Resource("Bauxite", 5, 6, 6));

            for (Resource r : resources){
                {
                    FractionExpression variable = model.addFractionVariable();
                    model.addConstraint(variable.nonNegative());
                    model.addConstraint(variable.lte(r.impureNodes));
                    itemExpressionMap.compute(gd.requireItemByName(r.itemName), (item, expression) -> Objects.requireNonNull(expression)
                            .add(300, variable));
                    powerExpression = powerExpression.subtract(130, variable);
                }
                {
                    FractionExpression variable = model.addFractionVariable();
                    model.addConstraint(variable.nonNegative());
                    model.addConstraint(variable.lte(r.normalNodes));
                    itemExpressionMap.compute(gd.requireItemByName(r.itemName), (item, expression) -> Objects.requireNonNull(expression)
                            .add(600, variable));
                    powerExpression = powerExpression.subtract(130, variable);
                }
                {
                    FractionExpression variable = model.addFractionVariable();
                    model.addConstraint(variable.nonNegative());
                    model.addConstraint(variable.lte(r.pureNodes));
                    itemExpressionMap.compute(gd.requireItemByName(r.itemName), (item, expression) -> Objects.requireNonNull(expression)
                            .add(780, variable));
                    powerExpression = powerExpression.subtract(100, variable);
                }
            }
        }

        {
            List<Resource> resources = new LinkedList<>();

            resources.add(new Resource("Nitrogen Gas", 2, 7, 36));
            resources.add(new Resource("Crude Oil", 6, 3, 3));
            resources.add(new Resource("Water", 5, 8, 42));

            for (Resource r : resources){
                {
                    FractionExpression variable = model.addFractionVariable();
                    model.addConstraint(variable.nonNegative());
                    model.addConstraint(variable.lte(r.impureNodes));
                    itemExpressionMap.compute(gd.requireItemByName(r.itemName), (item, expression) -> Objects.requireNonNull(expression)
                            .add(75000, variable));
                    powerExpression = powerExpression.subtract(649.82, variable);
                }
                {
                    FractionExpression variable = model.addFractionVariable();
                    model.addConstraint(variable.nonNegative());
                    model.addConstraint(variable.lte(r.normalNodes));
                    itemExpressionMap.compute(gd.requireItemByName(r.itemName), (item, expression) -> Objects.requireNonNull(expression)
                            .add(150000, variable));
                    powerExpression = powerExpression.subtract(649.82, variable);
                }
                {
                    FractionExpression variable = model.addFractionVariable();
                    model.addConstraint(variable.nonNegative());
                    model.addConstraint(variable.lte(r.pureNodes));
                    itemExpressionMap.compute(gd.requireItemByName(r.itemName), (item, expression) -> Objects.requireNonNull(expression)
                            .add(300000, variable));
                    powerExpression = powerExpression.subtract(649.82, variable);
                }
            }
        }

        {
            FractionExpression variable = model.addFractionVariable();
            model.addConstraint(variable.nonNegative());
            model.addConstraint(variable.lte(10));
            itemExpressionMap.compute(gd.requireItemByName("Crude Oil"), (item, expression) -> Objects.requireNonNull(expression)
                    .add(150000, variable));
            powerExpression = powerExpression.subtract(173.29, variable);
        }

        {
            FractionExpression variable = model.addFractionVariable();
            model.addConstraint(variable.nonNegative());
            model.addConstraint(variable.lte(12));
            itemExpressionMap.compute(gd.requireItemByName("Crude Oil"), (item, expression) -> Objects.requireNonNull(expression)
                    .add(300000, variable));
            powerExpression = powerExpression.subtract(173.29, variable);
        }

        {
            FractionExpression variable = model.addFractionVariable();
            model.addConstraint(variable.nonNegative());
            model.addConstraint(variable.lte(8));
            itemExpressionMap.compute(gd.requireItemByName("Crude Oil"), (item, expression) -> Objects.requireNonNull(expression)
                    .add(600000, variable));
            powerExpression = powerExpression.subtract(173.29, variable);
        }

        {
            FractionExpression nWaterExtractors = model.addFractionVariable();
            model.addConstraint(nWaterExtractors.nonNegative());

            powerExpression = powerExpression.subtract(20, nWaterExtractors);
            itemExpressionMap.compute(gd.requireItemByName("Water"), (item, expression) -> Objects.requireNonNullElseGet(expression, FractionExpression::zero)
                    .add(120000, nWaterExtractors));
        }

        for (var entry : itemExpressionMap.entrySet()){
            Item item = entry.getKey();
            FractionExpression expression = entry.getValue();

            if (item.getMatterState() == MatterState.SOLID){
                pointsExpression = pointsExpression.add(item.getSinkValue(), expression);
                model.addConstraint(expression.nonNegative());
            }else{
                model.addConstraint(expression.eq(0));
            }
        }

        model.addConstraint(itemExpressionMap.get(gd.requireItemByName("Uranium Waste")).eq(0));

        model.addConstraint(powerExpression.nonNegative());

        OptimizationResult result = model.maximize(pointsExpression);

        for (var entry : itemExpressionMap.entrySet()){
            Item item = entry.getKey();
            FractionExpression expression = entry.getValue();
            BigDecimal value = expression.getValue(result).toBigDecimal(4, RoundingMode.HALF_UP);

            if (value.signum() != 0){
                System.out.println(item.getName() + ": " + value);
            }
        }

        System.out.println("Points: " + result.getObjectiveFunctionValue().toBigDecimal(4, RoundingMode.HALF_UP));
    }

    private static FractionExpression addGenerator(Model model, String generatorName, long mwGenerated, Item fuel, BigDecimal fuelPerMinute, Map<Item, FractionExpression> itemExpressionMap, FractionExpression powerExpression)
    {
        FractionExpression numberOfGenerators = model.addFractionVariable();
        model.addConstraint(numberOfGenerators.nonNegative());
        itemExpressionMap.compute(fuel, (item, expression) -> Objects.requireNonNullElseGet(expression, FractionExpression::zero)
                .subtract(fuelPerMinute, numberOfGenerators));
        powerExpression = powerExpression.add(mwGenerated, numberOfGenerators);
        return powerExpression;
    }

    private static class Resource
    {
        private final String itemName;
        private final int impureNodes;
        private final int normalNodes;
        private final int pureNodes;

        public Resource(String itemName, int impureNodes, int normalNodes, int pureNodes)
        {
            this.itemName = itemName;
            this.impureNodes = impureNodes;
            this.normalNodes = normalNodes;
            this.pureNodes = pureNodes;
        }
    }
}
