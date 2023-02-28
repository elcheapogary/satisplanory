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

import io.github.elcheapogary.satisplanory.model.Item;
import io.github.elcheapogary.satisplanory.model.Recipe;
import io.github.elcheapogary.satisplanory.prodplan.MultiPlan;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import io.github.elcheapogary.satisplanory.util.MathExpression;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

class ErrorTab
{
    private ErrorTab()
    {
    }

    public static Tab create(ProdPlanModel model)
    {
        Tab tab = new Tab();
        tab.setClosable(true);
        tab.setText("Plan not possible");

        setContent(tab, model);

        model.multiPlanProperty().addListener((observable, oldValue, newValue) -> setContent(tab, model));

        return tab;
    }

    public static Region createContent(ProdPlanModel model)
    {
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

        if (model.getMultiPlan().canCreatePlanByAddingResources()){
            vbox.getChildren().add(createWordWrapLabel("A production plan is possible with the configured recipes, but requires the addition of the following input items:"));
            vbox.getChildren().add(createMissingResourcesTable(model.getMultiPlan()));
            Button button = new Button("Use this plan");
            vbox.getChildren().add(button);

            button.onActionProperty().set(event -> {
                for (var entry : model.getMultiPlan().getMissingResources().entrySet()){
                    if (entry.getValue().signum() > 0){
                        model.getInputItems().add(new ProdPlanModel.InputItem(entry.getKey(), MathExpression.valueOf(entry.getValue().toBigDecimal(4, RoundingMode.HALF_UP))));
                    }
                }
                model.setPlan(model.getMultiPlan().getPlanWithAllItems());
            });
        }

        if (model.getMultiPlan().canCreatePlanByAddingRecipes()){
            vbox.getChildren().add(createWordWrapLabel("A production plan is possible with the configured input items, but requires the following recipes:"));
            vbox.getChildren().add(createMissingRecipesList(model.getMultiPlan()));
            Button button = new Button("Use this plan");
            vbox.getChildren().add(button);

            button.onActionProperty().set(event -> {
                model.getEnabledRecipes().addAll(model.getMultiPlan().getMissingRecipes());
                model.setPlan(model.getMultiPlan().getPlanWithAllRecipes());
            });
        }

        if (model.getMultiPlan().canCreatePlanByAddingResourcesAndRecipes()){
            vbox.getChildren().add(createWordWrapLabel("A production plan is possible with the addition of the following recipes and input items:"));
            HBox hbox = new HBox(10);
            vbox.getChildren().add(hbox);
            hbox.getChildren().add(createMissingRecipesList(model.getMultiPlan()));
            {
                Node n = createMissingResourcesTable(model.getMultiPlan());
                HBox.setHgrow(n, Priority.ALWAYS);
                hbox.getChildren().add(n);
            }
            Button button = new Button("Use this plan");
            vbox.getChildren().add(button);

            button.onActionProperty().set(event -> {
                for (var entry : model.getMultiPlan().getMissingResources().entrySet()){
                    if (entry.getValue().signum() > 0){
                        model.getInputItems().add(new ProdPlanModel.InputItem(entry.getKey(), MathExpression.valueOf(entry.getValue().toBigDecimal(4, RoundingMode.HALF_UP))));
                    }
                }
                model.getEnabledRecipes().addAll(model.getMultiPlan().getMissingRecipes());
                model.setPlan(model.getMultiPlan().getPlanWithAllItemsAndRecipes());
            });
        }

        return bp;
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

    private static Node createMissingResourcesTable(MultiPlan multiPlan)
    {
        TableView<Map.Entry<Item, BigFraction>> tableView = new TableView<>();

        {
            TableColumn<Map.Entry<Item, BigFraction>, String> col = new TableColumn<>("Item");
            tableView.getColumns().add(col);
            col.cellValueFactoryProperty().set(param -> new SimpleStringProperty(param.getValue().getKey().getName()));
        }

        {
            TableColumn<Map.Entry<Item, BigFraction>, BigDecimal> col = new TableColumn<>("Configured items/min");
            tableView.getColumns().add(col);
            col.setStyle("-fx-alignment: CENTER_RIGHT;");
            col.cellValueFactoryProperty().set(param -> {
                Item item = param.getValue().getKey();
                BigFraction configuredInputAmount = multiPlan.getProductionPlanner().getInputItems().get(item);
                configuredInputAmount = Objects.requireNonNullElse(configuredInputAmount, BigFraction.zero());
                return new SimpleObjectProperty<>(configuredInputAmount.toBigDecimal(4, RoundingMode.HALF_UP));
            });
        }

        {
            TableColumn<Map.Entry<Item, BigFraction>, BigDecimal> col = new TableColumn<>("Additional items/min");
            tableView.getColumns().add(col);
            col.setStyle("-fx-alignment: CENTER_RIGHT;");
            col.cellValueFactoryProperty().set(param -> new SimpleObjectProperty<>(param.getValue().getValue().toBigDecimal(4, RoundingMode.HALF_UP)));
        }

        {
            TableColumn<Map.Entry<Item, BigFraction>, BigDecimal> col = new TableColumn<>("Total items/min");
            tableView.getColumns().add(col);
            col.setStyle("-fx-alignment: CENTER_RIGHT;");
            col.cellValueFactoryProperty().set(param -> {
                BigFraction v = param.getValue().getValue();
                Item item = param.getValue().getKey();
                BigFraction configuredInputAmount = multiPlan.getProductionPlanner().getInputItems().get(item);
                configuredInputAmount = Objects.requireNonNullElse(configuredInputAmount, BigFraction.zero());
                v = v.add(configuredInputAmount);
                return new SimpleObjectProperty<>(v.toBigDecimal(4, RoundingMode.HALF_UP));
            });
        }

        tableView.setItems(FXCollections.observableList(new ArrayList<>(multiPlan.getMissingResources().entrySet())));

        tableView.setPrefHeight(150);

        return tableView;
    }

    private static Label createWordWrapLabel(String text)
    {
        Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }

    private static void setContent(Tab tab, ProdPlanModel model)
    {
        if (model.getMultiPlan() == null){
            tab.setContent(new Pane());
        }else{
            tab.setContent(createContent(model));
        }
    }


}
