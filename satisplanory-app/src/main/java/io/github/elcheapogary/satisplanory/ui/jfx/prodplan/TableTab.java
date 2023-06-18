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

import io.github.elcheapogary.satisplanory.gamedata.Manufacturer;
import io.github.elcheapogary.satisplanory.gamedata.Item;
import io.github.elcheapogary.satisplanory.gamedata.Recipe;
import io.github.elcheapogary.satisplanory.prodplan.ProdPlanUtils;
import io.github.elcheapogary.satisplanory.prodplan.ProductionPlan;
import io.github.elcheapogary.satisplanory.ui.jfx.component.TableColumns;
import io.github.elcheapogary.satisplanory.ui.jfx.context.AppContext;
import io.github.elcheapogary.satisplanory.ui.jfx.tableexport.TableExportContextMenu;
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

    public static Tab create(AppContext appContext, ProdPlanModel model)
    {
        Tab tab = new Tab();
        tab.setClosable(false);
        tab.setText("Table");

        setContent(appContext, tab, model.getPlan());

        model.planProperty().addListener((observable, oldValue, newValue) -> setContent(appContext, tab, newValue));

        tab.disableProperty().bind(Bindings.createBooleanBinding(() -> model.planProperty().getValue() == null, model.planProperty()));

        return tab;
    }

    private static TableView<Row> createProductionPlanTableView(AppContext appContext, ProductionPlan plan)
    {
        TableView<Row> tableView = new TableView<>();

        tableView.setContextMenu(TableExportContextMenu.forTable(appContext, tableView));

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
                Optional.ofNullable(param.getValue().manufacturer)
                        .map(Manufacturer::getName)
                        .orElse(null)
        ));

        tableView.getColumns().add(TableColumns.createNumericColumn(
                "#",
                row -> Optional.ofNullable(row.number)
                        .map(bigFraction -> bigFraction.toBigDecimal(6, RoundingMode.HALF_UP))
                        .orElse(null),
                BigDecimal::toString,
                BigDecimal::compareTo
        ));

        for (Item item : items){
            tableView.getColumns().add(TableColumns.createNumericColumn(
                    item.getName(),
                    row -> Optional.ofNullable(row.itemAmounts.get(item))
                            .map(item::toDisplayAmount)
                            .orElse(null),
                    BigDecimal::toString,
                    BigDecimal::compareTo
            ));
        }

        {
            Map<Item, BigFraction> inputItems = Item.createMap();
            for (Item item : plan.getInputItems()){
                inputItems.put(item, Objects.requireNonNullElse(plan.getInputItemsPerMinute(item), BigFraction.zero()));
            }

            tableView.getItems().add(new Row("Input Items", null, null, inputItems));
        }

        for (Recipe recipe : plan.getRecipes()){
            BigFraction n = plan.getNumberOfMachinesWithRecipe(recipe);

            Map<Item, BigFraction> itemAmounts = Item.createMap();
            for (Recipe.RecipeItem recipeItem : recipe.getIngredients()){
                itemAmounts.put(
                        recipeItem.getItem(),
                        recipeItem.getAmountPerMinute()
                                .multiply(n)
                                .negate()
                );
            }
            for (Recipe.RecipeItem recipeItem : recipe.getProducts()){
                itemAmounts.compute(recipeItem.getItem(), (item, amount) ->
                        Objects.requireNonNullElse(amount, BigFraction.zero())
                                .add(recipeItem.getAmountPerMinute().multiply(n))
                );
            }
            tableView.getItems().add(new Row(recipe.getName(), recipe.getManufacturer(), n, itemAmounts));
        }

        {
            Map<Item, BigFraction> inputItems = Item.createMap();
            for (Item item : plan.getOutputItems()){
                inputItems.put(item, Objects.requireNonNullElse(plan.getOutputItemsPerMinute(item), BigFraction.zero()));
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

    private static void setContent(AppContext appContext, Tab tab, ProductionPlan plan)
    {
        if (plan == null){
            tab.setContent(new Pane());
        }else{
            tab.setContent(createProductionPlanTableView(appContext, plan));
        }
    }

    private static class Row
    {
        private final String name;
        private final Manufacturer manufacturer;
        private final BigFraction number;
        private final Map<Item, BigFraction> itemAmounts;

        public Row(String name, Manufacturer manufacturer, BigFraction number, Map<Item, BigFraction> itemAmounts)
        {
            this.name = name;
            this.manufacturer = manufacturer;
            this.number = number;
            this.itemAmounts = itemAmounts;
        }
    }
}
