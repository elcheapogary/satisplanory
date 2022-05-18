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

import java.util.ArrayList;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFontRegistry;

class SettingsPane
{
    private SettingsPane()
    {
    }

    private static void addItem(VBox items, OptimizationTargetModel target, ObservableList<OptimizationTargetModel> enabledTargets, ObservableList<OptimizationTargetModel> disabledTargets, ObservableList<ProdPlanModel.OutputItem> outputItems)
    {
        IntegerBinding position = Bindings.createIntegerBinding(() -> {
            int idx = enabledTargets.indexOf(target);
            if (idx >= 0){
                return idx;
            }
            return enabledTargets.size() + disabledTargets.indexOf(target);
        }, enabledTargets, disabledTargets);

        HBox targetNode = new HBox(10);
        items.getChildren().add(targetNode);

        targetNode.setPadding(new Insets(10));
        targetNode.styleProperty().bind(Bindings.createStringBinding(() -> {
            if (position.get() != 0){
                return "-fx-border-width: 1 0 0 0; -fx-border-color: -fx-box-border;";
            }else{
                return "";
            }
        }, position));
        targetNode.viewOrderProperty().bind(Bindings.createIntegerBinding(() -> 100 - position.get(), position));

        BooleanProperty enabledProperty;
        {
            VBox vbox = new VBox(10);
            HBox.setHgrow(vbox, Priority.ALWAYS);
            targetNode.getChildren().add(vbox);

            CheckBox checkbox = new CheckBox();
            enabledProperty = checkbox.selectedProperty();
            checkbox.selectedProperty().set(enabledTargets.contains(target));
            checkbox.textProperty().bind(Bindings.createStringBinding(() -> {
                if (!checkbox.selectedProperty().get()){
                    return "Disabled: " + target.getTitle();
                }else{
                    return "Priority " + (position.get() + 1) + ": " + target.getTitle();
                }
            }, position, checkbox.selectedProperty()));
            checkbox.setStyle("-fx-font-weight: bold;");
            vbox.getChildren().add(checkbox);

            checkbox.selectedProperty().addListener((observableValue, oldValue, newValue) -> {
                items.getChildren().remove(targetNode);
                disabledTargets.remove(target);
                enabledTargets.remove(target);
                items.getChildren().add(enabledTargets.size(), targetNode);
                if (newValue){
                    enabledTargets.add(target);
                }else{
                    disabledTargets.add(0, target);
                }
            });

            VBox body = new VBox(10);
            vbox.getChildren().add(body);

            body.setPadding(new Insets(0, 0, 0, 25));

            Label descriptionLabel = new Label(target.getDescription());
            descriptionLabel.setWrapText(true);
            body.getChildren().add(descriptionLabel);
        }

        {
            VBox buttons = new VBox(0);
            HBox.setHgrow(buttons, Priority.NEVER);
            targetNode.getChildren().add(buttons);

            Button upArrowButton = new Button("", GlyphFontRegistry.font("FontAwesome").create(FontAwesome.Glyph.ARROW_UP));
            Button downArrowButton = new Button("", GlyphFontRegistry.font("FontAwesome").create(FontAwesome.Glyph.ARROW_DOWN));

            Region grower = new Region();
            grower.setPrefHeight(10);
            grower.setMinHeight(10);
            grower.setMaxHeight(Double.MAX_VALUE);

            VBox.setVgrow(upArrowButton, Priority.NEVER);
            buttons.getChildren().add(upArrowButton);
            VBox.setVgrow(grower, Priority.ALWAYS);
            buttons.getChildren().add(grower);
            VBox.setVgrow(downArrowButton, Priority.NEVER);
            buttons.getChildren().add(downArrowButton);

            upArrowButton.onActionProperty().set(actionEvent -> {
                int idx = enabledTargets.indexOf(target);
                enabledTargets.remove(target);
                enabledTargets.add(idx - 1, target);
                items.getChildren().remove(targetNode);
                items.getChildren().add(idx - 1, targetNode);
            });
            upArrowButton.disableProperty().bind(Bindings.createBooleanBinding(() -> !(enabledProperty.get() && position.get() > 0), enabledProperty, position));
            downArrowButton.onActionProperty().set(actionEvent -> {
                int idx = enabledTargets.indexOf(target);
                enabledTargets.remove(target);
                enabledTargets.add(idx + 1, target);
                items.getChildren().remove(targetNode);
                items.getChildren().add(idx + 1, targetNode);
            });
            downArrowButton.disableProperty().bind(Bindings.createBooleanBinding(() -> !(enabledProperty.get() && position.get() < enabledTargets.size() - 1), enabledProperty, position, enabledTargets));
        }
    }

    private static void addItems(VBox items, ObservableList<ProdPlanModel.OutputItem> outputItems, ObservableList<OptimizationTargetModel> enabledTargets)
    {
        ObservableList<OptimizationTargetModel> disabledTargets = FXCollections.observableList(new ArrayList<>());

        for (OptimizationTargetModel target : enabledTargets){
            addItem(items, target, enabledTargets, disabledTargets, outputItems);
        }

        for (OptimizationTargetModel target : OptimizationTargetModel.values()){
            if (!enabledTargets.contains(target)){
                disabledTargets.add(target);
                addItem(items, target, enabledTargets, disabledTargets, outputItems);
            }
        }
    }

    public static TitledPane createSettingsPane(ProdPlanModel.Settings settings, ObservableList<ProdPlanModel.OutputItem> outputItems)
    {
        VBox vbox = new VBox(10);
        vbox.setFillWidth(true);
        vbox.setPadding(new Insets(10));

        {
            Label l = new Label("Arrange the following settings from most important to least important to set the calculator to generate a result based on what is important to you.");
            l.setWrapText(true);
            vbox.getChildren().add(l);
        }

        HBox hbox = new HBox();
        VBox.setVgrow(hbox, Priority.NEVER);
        vbox.getChildren().add(hbox);

        hbox.setFillHeight(true);
        hbox.setPadding(new Insets(5, 40, 5, 40));

        VBox items = new VBox(0);
        HBox.setHgrow(items, Priority.ALWAYS);
        hbox.getChildren().add(items);

        items.setStyle("-fx-border-width: 1px; -fx-border-color: -fx-box-border;");

        addItems(items, outputItems, settings.getOptimizationTargets());

        {
            Region grower = new Region();
            grower.setPrefHeight(0);
            VBox.setVgrow(grower, Priority.ALWAYS);
            vbox.getChildren().add(grower);
        }

        TitledPane titledPane = new TitledPane();
        titledPane.setCollapsible(true);
        ScrollPane scrollPane = new ScrollPane(vbox);
        scrollPane.setFitToWidth(true);
        titledPane.setContent(scrollPane);
        titledPane.setText("Optimization Targets");
        return titledPane;
    }
}
