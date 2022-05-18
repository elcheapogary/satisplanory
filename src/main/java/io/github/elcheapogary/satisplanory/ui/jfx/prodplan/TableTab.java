/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.prodplan;

import io.github.elcheapogary.satisplanory.model.Building;
import io.github.elcheapogary.satisplanory.model.Item;
import io.github.elcheapogary.satisplanory.model.Recipe;
import io.github.elcheapogary.satisplanory.prodplan.ProdPlanUtils;
import io.github.elcheapogary.satisplanory.prodplan.ProductionPlan;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import io.github.elcheapogary.satisplanory.util.Comparators;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;

class TableTab
{
    private TableTab()
    {
    }

    public static Tab create(ProdPlanModel model)
    {
        Tab tab = new Tab();
        tab.setClosable(false);
        tab.setText("Table");

        setContent(tab, model.getPlan());

        model.planProperty().addListener((observable, oldValue, newValue) -> setContent(tab, newValue));

        tab.disableProperty().bind(Bindings.createBooleanBinding(() -> model.planProperty().getValue() == null, model.planProperty()));

        return tab;
    }

    private static void setContent(Tab tab, ProductionPlan plan)
    {
        if (plan == null){
            tab.setContent(new Pane());
        }else{
            tab.setContent(createProductionPlanTableView(plan));
        }
    }

    private static TableView<Row> createProductionPlanTableView(ProductionPlan plan)
    {
        TableView<Row> tableView = new TableView<>();

        tableView.setPrefWidth(0);

        Set<Item> items = Item.createSet();

        items.addAll(plan.getInputItems());
        items.addAll(plan.getOutputItems());
        items.addAll(ProdPlanUtils.getItemsUsedInRecipes(plan.getRecipes()));

        TableColumn<Row, String> descriptionColumn = new TableColumn<>("Recipe");
        tableView.getColumns().add(descriptionColumn);
        descriptionColumn.cellValueFactoryProperty().set(param -> new SimpleStringProperty(param.getValue().name));

        TableColumn<Row, String> machineColumns = new TableColumn<>("Machine");
        tableView.getColumns().add(machineColumns);
        machineColumns.cellValueFactoryProperty().set(param -> new SimpleStringProperty(
                Optional.ofNullable(param.getValue().building)
                        .map(Building::getName)
                        .orElse(null)
        ));

        TableColumn<Row, BigDecimal> countColumn = new TableColumn<>("#");
        tableView.getColumns().add(countColumn);
        countColumn.setStyle("-fx-alignment: CENTER_RIGHT;");
        countColumn.cellValueFactoryProperty().set(param -> new SimpleObjectProperty<>(Optional.ofNullable(param.getValue().number)
                .map(n -> n.toBigDecimal(6, RoundingMode.HALF_UP))
                .orElse(null)
        ));

        for (Item item : items){
            TableColumn<Row, BigDecimal> itemColumn = new TableColumn<>(item.getName());
            tableView.getColumns().add(itemColumn);
            itemColumn.setStyle("-fx-alignment: CENTER_RIGHT;");
            itemColumn.cellValueFactoryProperty().set(param -> {
                BigFraction value = item.toDisplayAmount(Objects.requireNonNullElse(param.getValue().itemAmounts.get(item), BigFraction.ZERO));
                if (value.signum() == 0){
                    return null;
                }
                return new SimpleObjectProperty<>(value.toBigDecimal(4, RoundingMode.HALF_UP));
            });
        }

        {
            Map<Item, BigFraction> inputItems = Item.createMap();
            for (Item item : plan.getInputItems()){
                inputItems.put(item, Objects.requireNonNullElse(plan.getInputItemsPerMinute(item), BigFraction.ZERO));
            }

            tableView.getItems().add(new Row("Input Items", null, null, inputItems));
        }

        for (Recipe recipe : plan.getRecipes()){
            BigFraction n = plan.getNumberOfMachinesWithRecipe(recipe);

            Map<Item, BigFraction> itemAmounts = Item.createMap();
            for (Recipe.RecipeItem recipeItem : recipe.getIngredients()){
                itemAmounts.put(
                        recipeItem.getItem(),
                        BigFraction.valueOf(recipeItem.getAmount().getAmountPerMinute())
                                .multiply(n)
                                .negate()
                );
            }
            for (Recipe.RecipeItem recipeItem : recipe.getProducts()){
                itemAmounts.compute(recipeItem.getItem(), (item, amount) ->
                        Objects.requireNonNullElse(amount, BigFraction.ZERO)
                                .add(BigFraction.valueOf(recipeItem.getAmount().getAmountPerMinute()).multiply(n))
                );
            }
            tableView.getItems().add(new Row(recipe.getName(), recipe.getProducedInBuilding(), n, itemAmounts));
        }

        {
            Map<Item, BigFraction> inputItems = Item.createMap();
            for (Item item : plan.getOutputItems()){
                inputItems.put(item, Objects.requireNonNullElse(plan.getOutputItemsPerMinute(item), BigFraction.ZERO));
            }

            tableView.getItems().add(new Row("Output Items", null, null, inputItems));
        }

        tableView.setSortPolicy(param -> {
            Comparator<Row> comparator = Comparators.<Row>sortFirst(row -> row.name.equals("Input Items"))
                    .thenComparing(Comparators.sortLast(row -> row.name.equals("Output Items")));

            if (param.getComparator() != null){
                comparator = comparator.thenComparing(param.getComparator());
            }

            FXCollections.sort(param.getItems(), comparator);
            return true;
        });

        return tableView;
    }

    private static class Row
    {
        private final String name;
        private final Building building;
        private final BigFraction number;
        private final Map<Item, BigFraction> itemAmounts;

        public Row(String name, Building building, BigFraction number, Map<Item, BigFraction> itemAmounts)
        {
            this.name = name;
            this.building = building;
            this.number = number;
            this.itemAmounts = itemAmounts;
        }
    }
}
