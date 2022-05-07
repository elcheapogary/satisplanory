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

import io.github.elcheapogary.satisplanory.model.Item;
import io.github.elcheapogary.satisplanory.ui.jfx.component.BigDecimalTextField;
import io.github.elcheapogary.satisplanory.ui.jfx.component.ItemComponents;
import java.math.BigDecimal;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

class OutputItemsPane
{
    private OutputItemsPane()
    {
    }

    static TitledPane createOutputRequirementsPane(List<ProdPlanData.OutputItem> outputItems, ObservableList<Item> allItems)
    {
        TitledPane titledPane = new TitledPane();
        titledPane.setText("Output Requirements");
        titledPane.setCollapsible(true);

        VBox vbox = new VBox(10);
        {
            BorderPane.setMargin(vbox, new Insets(10));
            ScrollPane scrollPane = new ScrollPane(new BorderPane(vbox));
            scrollPane.setFitToWidth(true);
            titledPane.setContent(scrollPane);
        }

        HBox buttons = new HBox(10);
        vbox.getChildren().add(buttons);

        Button addRowButton = new Button("Add Item");
        buttons.getChildren().add(addRowButton);

        addRowButton.onActionProperty().set(event -> {
            ProdPlanData.OutputItem outputItem = new ProdPlanData.OutputItem(allItems.get(0), BigDecimal.ONE, BigDecimal.ZERO);
            outputItems.add(outputItem);
            addRow(outputItem, vbox, outputItems, allItems);
        });

        Region grower = new Region();
        HBox.setHgrow(grower, Priority.ALWAYS);
        buttons.getChildren().add(grower);

        Button removeAllButton = new Button("Remove All");
        buttons.getChildren().add(removeAllButton);
        removeAllButton.onActionProperty().set(event -> {
            outputItems.clear();
            vbox.getChildren().remove(0, vbox.getChildren().size() - 1);
        });

        for (ProdPlanData.OutputItem outputItem : outputItems){
            addRow(outputItem, vbox, outputItems, allItems);
        }

        return titledPane;
    }

    private static void addRow(ProdPlanData.OutputItem outputItem, VBox vbox, List<ProdPlanData.OutputItem> outputItems, ObservableList<Item> allItems)
    {
        HBox hbox = new HBox(10);
        vbox.getChildren().add(vbox.getChildren().size() - 1, hbox);

        hbox.setAlignment(Pos.BASELINE_LEFT);

        {
            ComboBox<Item> comboBox = ItemComponents.createItemComboBox(allItems);
            comboBox.getSelectionModel().select(outputItem.getItem());
            comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                outputItem.setItem(newValue);
            });
            comboBox.setMaxWidth(Double.MAX_VALUE);
            comboBox.setPrefWidth(150);
            HBox.setHgrow(comboBox, Priority.ALWAYS);
            hbox.getChildren().add(comboBox);
        }

        hbox.getChildren().add(new Label("Min:"));

        {
            TextField minTextField = new TextField();
            minTextField.setMaxWidth(80);
            BigDecimalTextField.setUp(minTextField, outputItem.getMin(), outputItem::setMin);
            hbox.getChildren().add(minTextField);
        }

        {
            Label maximizeWeightLabel = new Label("Weight:");
            hbox.getChildren().add(maximizeWeightLabel);
        }

        {
            TextField weightTextField = new TextField();
            weightTextField.setMaxWidth(80);
            BigDecimalTextField.setUp(weightTextField, outputItem.getWeight(), outputItem::setWeight);
            hbox.getChildren().add(weightTextField);
        }

        {
            Button removeButton = new Button("Remove");
            hbox.getChildren().add(removeButton);

            removeButton.onActionProperty().set(event -> {
                outputItems.remove(outputItem);
                vbox.getChildren().remove(hbox);
            });
        }
    }
}
