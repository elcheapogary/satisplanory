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

import io.github.elcheapogary.satisplanory.gamedata.GameData;
import io.github.elcheapogary.satisplanory.gamedata.Item;
import io.github.elcheapogary.satisplanory.ui.jfx.component.ItemComponents;
import io.github.elcheapogary.satisplanory.ui.jfx.component.MathExpressionTextField;
import io.github.elcheapogary.satisplanory.util.MathExpression;
import java.util.List;
import java.util.Objects;
import javafx.collections.ListChangeListener;
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

class InputItemsPane
{
    private static void addRow(VBox container, ProdPlanModel.InputItem inputItem, List<ProdPlanModel.InputItem> inputItems, ObservableList<Item> allItems)
    {
        HBox hBox = new HBox(10);
        container.getChildren().add(container.getChildren().size() - 1, hBox);

        hBox.setAlignment(Pos.BASELINE_LEFT);

        ComboBox<Item> comboBox = ItemComponents.createItemComboBox(allItems);
        comboBox.setPrefWidth(150);
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.valueProperty().bindBidirectional(inputItem.itemProperty());
        HBox.setHgrow(comboBox, Priority.ALWAYS);
        hBox.getChildren().add(comboBox);

        {
            Label amountPerMinuteLabel = new Label("Amount per minute:");
            amountPerMinuteLabel.prefWidthProperty().bind(amountPerMinuteLabel.minWidthProperty());
            HBox.setHgrow(amountPerMinuteLabel, Priority.NEVER);
            hBox.getChildren().add(amountPerMinuteLabel);
        }

        TextField tf = new TextField();
        tf.setMaxWidth(80);
        MathExpressionTextField.setUp(tf, inputItem.getAmount(), e -> e.getValue().signum() >= 0, inputItem::setAmount);
        inputItem.amountProperty().addListener((observable, oldValue, newValue) -> tf.setText(Objects.requireNonNullElse(newValue, "0").toString()));
        HBox.setHgrow(tf, Priority.NEVER);
        hBox.getChildren().add(tf);

        Button removeButton = new Button("Remove");
        HBox.setHgrow(removeButton, Priority.NEVER);
        hBox.getChildren().add(removeButton);

        removeButton.onActionProperty().setValue(event -> {
            container.getChildren().remove(hBox);
            inputItems.remove(inputItem);
        });

        inputItem.itemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue != oldValue && newValue.getName().equals("Water")){
                tf.textProperty().set("999999999999");
                inputItem.setAmount(MathExpression.valueOf(999999999999L));
            }
        });

        tf.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (tf.isFocused()){
                tf.selectAll();
            }
        });
    }

    static TitledPane createInputItemsPane(ObservableList<ProdPlanModel.InputItem> inputItems, ObservableList<Item> allItems, GameData gameData)
    {
        TitledPane titledPane = new TitledPane();
        titledPane.setText("Input Items");
        titledPane.setCollapsible(true);

        VBox vbox = new VBox(10);
        BorderPane.setMargin(vbox, new Insets(10));
        ScrollPane scrollPane = new ScrollPane(new BorderPane(vbox));
        scrollPane.setFitToWidth(true);
        titledPane.setContent(scrollPane);

        {
            Label help = new Label("Add items that are available to be used as input for the factory. Using \"Add "
                    + "All Raw Resources\" adds all raw resources on the map.");
            help.setWrapText(true);
            vbox.getChildren().add(help);
        }

        VBox table = new VBox(10);
        vbox.getChildren().add(table);

        HBox buttons = new HBox(10);
        table.getChildren().add(buttons);

        Button addRowButton = new Button("Add Item");
        buttons.getChildren().add(addRowButton);
        addRowButton.onActionProperty().setValue(event -> {
            ProdPlanModel.InputItem inputItem = new ProdPlanModel.InputItem(allItems.get(0), MathExpression.valueOf(0));
            inputItems.add(inputItem);
        });

        Button addMapLimitsButton = new Button("Add All Raw Resources");
        buttons.getChildren().add(addMapLimitsButton);
        addMapLimitsButton.onActionProperty().set(event -> {
            for (Item item : gameData.getRawResources()){
                ProdPlanModel.InputItem inputItem = new ProdPlanModel.InputItem(item, MathExpression.valueOf(gameData.getRawResourceMaxExtractionRatePerMinute(item)));
                inputItems.add(inputItem);
            }
            gameData.getItemByName("Water")
                    .ifPresent(item -> {
                        ProdPlanModel.InputItem inputItem = new ProdPlanModel.InputItem(item, MathExpression.valueOf(999999999999L));
                        inputItems.add(inputItem);
                    });
        });

        Region grower = new Region();
        HBox.setHgrow(grower, Priority.ALWAYS);
        buttons.getChildren().add(grower);

        Button removeAllButton = new Button("Remove All");
        buttons.getChildren().add(removeAllButton);
        removeAllButton.onActionProperty().set(event -> {
            inputItems.clear();
            table.getChildren().remove(0, table.getChildren().size() - 1);
        });

        for (ProdPlanModel.InputItem inputItem : inputItems){
            addRow(table, inputItem, inputItems, allItems);
        }

        inputItems.addListener((ListChangeListener<ProdPlanModel.InputItem>)c -> {
            while (c.next()){
                if (c.wasAdded()){
                    for (ProdPlanModel.InputItem inputItem : c.getAddedSubList()){
                        addRow(table, inputItem, inputItems, allItems);
                    }
                }
            }
        });

        return titledPane;
    }
}
