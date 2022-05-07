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
import io.github.elcheapogary.satisplanory.prodplan.MultiPlan;
import io.github.elcheapogary.satisplanory.prodplan.ProdPlanUtils;
import io.github.elcheapogary.satisplanory.prodplan.ProductionPlan;
import io.github.elcheapogary.satisplanory.ui.jfx.component.ZoomableScrollPane;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

class ResultsPane
{
    private ResultsPane()
    {
    }

    public static Node createResultsTabContent(MultiPlan results)
    {
        if (results.isUnmodifiedPlanFeasible()){
            return createProductionPlanTabPane(results.getUnmodifiedPlan());
        }else{
            BorderPane bp = new BorderPane();
            bp.setPadding(new Insets(10));

            TitledPane titledPane = new TitledPane();
            titledPane.setCollapsible(true);
            titledPane.setText("Production plan not possible");
            BorderPane.setMargin(titledPane, new Insets(0, 0, 10, 0));
            bp.setTop(titledPane);

            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(10));
            titledPane.setContent(vbox);

            vbox.getChildren().add(createWordWrapLabel("A production plan is not possible with the selected input items and recipes."));

            if (results.canCreatePlanByAddingResources()){
                vbox.getChildren().add(createWordWrapLabel("A production plan is possible with the configured recipes, but requires the addition of the following input items:"));
                vbox.getChildren().add(createMissingResourcesTable(results));
                Button button = new Button("Show this plan");
                vbox.getChildren().add(button);

                button.onActionProperty().set(event -> bp.setCenter(createTitledProductionPane("Production plan with missing input items", results.getPlanWithAllItems())));
            }

            if (results.canCreatePlanByAddingRecipes()){
                vbox.getChildren().add(createWordWrapLabel("A production plan is possible with the configured input items, but requires the following recipes:"));
                vbox.getChildren().add(createMissingRecipesList(results));
                Button button = new Button("Show this plan");
                vbox.getChildren().add(button);

                button.onActionProperty().set(event -> bp.setCenter(createTitledProductionPane("Production plan with missing recipes", results.getPlanWithAllRecipes())));
            }

            if (results.canCreatePlanByAddingResourcesAndRecipes()){
                vbox.getChildren().add(createWordWrapLabel("A production plan is possible with the addition of the following recipes and input items:"));
                HBox hbox = new HBox(10);
                vbox.getChildren().add(hbox);
                hbox.getChildren().add(createMissingRecipesList(results));
                {
                    Node n = createMissingResourcesTable(results);
                    HBox.setHgrow(n, Priority.ALWAYS);
                    hbox.getChildren().add(n);
                }
                Button button = new Button("Show this plan");
                vbox.getChildren().add(button);

                button.onActionProperty().set(event -> bp.setCenter(createTitledProductionPane("Production plan with missing recipes and input items", results.getPlanWithAllItemsAndRecipes())));
            }

            return bp;
        }
    }

    private static Node createMissingRecipesList(MultiPlan multiPlan)
    {
        ListView<String> listView = new ListView<>();
        listView.setPrefHeight(150);
        listView.setCellFactory(TextFieldListCell.forListView());
        List<String> missingRecipesList = new LinkedList<>();
        for (Recipe r : multiPlan.getMissingRecipes()){
            missingRecipesList.add(r.getName());
        }
        listView.setItems(FXCollections.observableList(new ArrayList<>(missingRecipesList)));
        return listView;
    }

    private static Label createWordWrapLabel(String text)
    {
        Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }

    private static Node createMissingResourcesTable(MultiPlan multiPlan)
    {
        TableView<Map.Entry<Item, BigDecimal>> tableView = new TableView<>();

        {
            TableColumn<Map.Entry<Item, BigDecimal>, String> col = new TableColumn<>("Item");
            tableView.getColumns().add(col);
            col.cellValueFactoryProperty().set(param -> new SimpleStringProperty(param.getValue().getKey().getName()));
        }

        {
            TableColumn<Map.Entry<Item, BigDecimal>, BigDecimal> col = new TableColumn<>("Configured items/min");
            tableView.getColumns().add(col);
            col.setStyle("-fx-alignment: CENTER_RIGHT;");
            col.cellValueFactoryProperty().set(param -> {
                Item item = param.getValue().getKey();
                BigDecimal configuredInputAmount = multiPlan.getProductionPlanner().getInputItems().get(item);
                configuredInputAmount = Objects.requireNonNullElse(configuredInputAmount, BigDecimal.ZERO);
                configuredInputAmount = configuredInputAmount.setScale(4, RoundingMode.HALF_UP);
                return new SimpleObjectProperty<>(configuredInputAmount);
            });
        }

        {
            TableColumn<Map.Entry<Item, BigDecimal>, BigDecimal> col = new TableColumn<>("Additional items/min");
            tableView.getColumns().add(col);
            col.setStyle("-fx-alignment: CENTER_RIGHT;");
            col.cellValueFactoryProperty().set(param -> new SimpleObjectProperty<>(param.getValue().getValue().setScale(4, RoundingMode.HALF_UP)));
        }

        {
            TableColumn<Map.Entry<Item, BigDecimal>, BigDecimal> col = new TableColumn<>("Total items/min");
            tableView.getColumns().add(col);
            col.setStyle("-fx-alignment: CENTER_RIGHT;");
            col.cellValueFactoryProperty().set(param -> {
                BigDecimal v = param.getValue().getValue();
                Item item = param.getValue().getKey();
                BigDecimal configuredInputAmount = multiPlan.getProductionPlanner().getInputItems().get(item);
                configuredInputAmount = Objects.requireNonNullElse(configuredInputAmount, BigDecimal.ZERO);
                v = v.add(configuredInputAmount);
                v = v.setScale(4, RoundingMode.HALF_UP);
                return new SimpleObjectProperty<>(v);
            });
        }

        tableView.setItems(FXCollections.observableList(new ArrayList<>(multiPlan.getMissingResources().entrySet())));

        tableView.setPrefHeight(150);

        return tableView;
    }

    private static TitledPane createTitledProductionPane(String title, ProductionPlan plan)
    {
        TitledPane titledPane = new TitledPane();
        titledPane.setText(title);
        titledPane.setContent(createProductionPlanTabPane(plan));
        titledPane.setMaxHeight(Double.MAX_VALUE);
        return titledPane;
    }

    private static Node createProductionPlanTabPane(ProductionPlan plan)
    {
        TabPane tabPane = new TabPane();

        {
            Tab graphTab = new Tab("Graph");
            graphTab.setClosable(false);
            tabPane.getTabs().add(graphTab);
            ScrollPane sp = new ZoomableScrollPane(GraphPane.createGraphPane(plan));
            sp.setPannable(true);
            graphTab.setContent(sp);
        }

        {
            Tab tableTab = new Tab("Table");
            tableTab.setClosable(false);
            tabPane.getTabs().add(tableTab);
            tableTab.setContent(createProductionPlanTableView(plan));
        }

        return tabPane;
    }

    private static Node createProductionPlanTableView(ProductionPlan plan)
    {
        TableView<Row> tableView = new TableView<>();

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
                .map(n -> n.toBigDecimal(4, RoundingMode.HALF_UP))
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
