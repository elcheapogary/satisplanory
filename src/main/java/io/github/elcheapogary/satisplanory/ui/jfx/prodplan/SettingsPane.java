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

import io.github.elcheapogary.satisplanory.prodplan.ProductionPlanner;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
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

    private static void addItem(VBox items, OptimizationTarget target, ObservableBooleanValue hasMaximizeOutput, ObservableList<OptimizationTarget> enabledTargets, ObservableList<OptimizationTarget> disabledTargets)
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

            for (Warning warning : target.createWarnings(checkbox.selectedProperty(), hasMaximizeOutput, enabledTargets)){
                TitledPane titledPane = new TitledPane();
                body.getChildren().add(titledPane);

                BooleanBinding condition = Bindings.createBooleanBinding(warning.condition::getAsBoolean, checkbox.selectedProperty(), hasMaximizeOutput, enabledTargets);

                titledPane.visibleProperty().bind(condition);
                titledPane.managedProperty().bind(condition);

                titledPane.setCollapsible(false);
                titledPane.setText("Warning: " + warning.title);

                GridPane gp = new GridPane();
                titledPane.setContent(gp);

                gp.setPadding(new Insets(10));

                Label label = new Label(warning.description);
                label.setWrapText(true);
                gp.add(label, 0, 0);
            }
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

    private static void addItems(VBox items, ObservableValue<List<ProdPlanData.OutputItem>> outputItems, ObservableList<OptimizationTarget> enabledTargets)
    {
        BooleanBinding hasMaximizeOutput = Bindings.createBooleanBinding(() -> {
            for (ProdPlanData.OutputItem outputItem : outputItems.getValue()){
                if (outputItem.getWeight().signum() > 0){
                    return true;
                }
            }
            return false;
        }, outputItems);

        ObservableList<OptimizationTarget> disabledTargets = FXCollections.observableList(new ArrayList<>());

        for (OptimizationTarget target : enabledTargets){
            addItem(items, target, hasMaximizeOutput, enabledTargets, disabledTargets);
        }

        for (OptimizationTarget target : OptimizationTarget.values()){
            if (!enabledTargets.contains(target)){
                disabledTargets.add(target);
                addItem(items, target, hasMaximizeOutput, enabledTargets, disabledTargets);
            }
        }
    }

    public static TitledPane createSettingsPane(ProdPlanData.Settings settings, ObservableValue<List<ProdPlanData.OutputItem>> outputItems, ObservableList<OptimizationTarget> enabledTargets)
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

        addItems(items, outputItems, enabledTargets);

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
        titledPane.setText("Recipe selection");
        return titledPane;
    }

    private static boolean isMoreImportant(OptimizationTarget a, OptimizationTarget b, List<OptimizationTarget> l)
    {
        return !l.contains(b) || l.indexOf(a) < l.indexOf(b);
    }

    public enum OptimizationTarget
    {
        BALANCE(
                "maximizeBalance",
                "Maximize balance",
                "When multiple output items have weights, try make sure the ratio between the amount "
                        + "produced of these items matches the ratio between their weights."
        ){
            @Override
            public void setWeight(ProductionPlanner.Builder pb, BigFraction weight)
            {
                pb.setBalanceWeight(weight);
            }

            @Override
            public Warning[] createWarnings(ObservableBooleanValue targetEnabled, ObservableBooleanValue hasMaximizeOutputItem, ObservableList<OptimizationTarget> order)
            {
                return new Warning[]{

                };
            }
        },
        MAXIMIZE_OUTPUT_ITEMS(
                "maximizeOutputItems",
                "Maximize output items",
                "Try create as much as possible of all output items with a weight > 0."
        ){
            @Override
            public void setWeight(ProductionPlanner.Builder pb, BigFraction weight)
            {
                pb.setMaximizeOutputItemWeight(weight);
            }

            @Override
            public Warning[] createWarnings(ObservableBooleanValue targetEnabled, ObservableBooleanValue hasMaximizeOutputItem, ObservableList<OptimizationTarget> order)
            {
                return new Warning[]{

                };
            }
        },
        MINIMIZE_INPUT_ITEMS(
                "minimizeInputItems",
                "Minimize input items",
                "Use as little as possible of the input items."
        ){
            @Override
            public void setWeight(ProductionPlanner.Builder pb, BigFraction weight)
            {
                pb.setMinimizeInputItemWeight(weight);
            }

            @Override
            public Warning[] createWarnings(ObservableBooleanValue targetEnabled, ObservableBooleanValue hasMaximizeOutputItem, ObservableList<OptimizationTarget> order)
            {
                return new Warning[]{

                };
            }
        },
        MAXIMIZE_INPUT_ITEMS(
                "maximizeInputItems",
                "Maximize input items",
                "Use as much as possible of the input items."
        ){
            @Override
            public void setWeight(ProductionPlanner.Builder pb, BigFraction weight)
            {
                pb.setMaximizeInputItemsWeight(weight);
            }

            @Override
            public Warning[] createWarnings(ObservableBooleanValue targetEnabled, ObservableBooleanValue hasMaximizeOutputItem, ObservableList<OptimizationTarget> order)
            {
                return new Warning[]{

                };
            }
        },
        MINIMIZE_SURPLUS(
                "minimizeSurplus",
                "Minimize surplus",
                "Try create as little as possible of items that are not maximized output items (that do not "
                        + "have a weight configured in \"Output requirements\"). If you have output items with a "
                        + "configured minimum output per minute, the calculated plan will still produce that "
                        + "minimum amount.\n"
                        + "\n"
                        + "Use with \"" + MAXIMIZE_INPUT_ITEMS.title + "\" to ensure that only final products are "
                        + "produced, not intermediate items.\n"
                        + "\n"
                        + "Can also be used to avoid recipes that produce by-products."
        ){
            @Override
            public void setWeight(ProductionPlanner.Builder pb, BigFraction weight)
            {
                pb.setMinimizeSurplusWeight(weight);
            }

            @Override
            public Warning[] createWarnings(ObservableBooleanValue targetEnabled, ObservableBooleanValue hasMaximizeOutputItem, ObservableList<OptimizationTarget> order)
            {
                return new Warning[]{

                };
            }
        },
        MINIMIZE_POWER_CONSUMPTION(
                "minimizePowerConsumption",
                "Minimize power consumption",
                "Try and use as little power as possible."
        ){
            @Override
            public void setWeight(ProductionPlanner.Builder pb, BigFraction weight)
            {
                pb.setPowerWeight(weight);
            }

            @Override
            public Warning[] createWarnings(ObservableBooleanValue targetEnabled, ObservableBooleanValue hasMaximizeOutputItem, ObservableList<OptimizationTarget> order)
            {
                return new Warning[]{
                        new Warning(
                                "Will probably have no effect",
                                "\"" + MAXIMIZE_OUTPUT_ITEMS.title + "\" tries to create as much of the output items as "
                                        + "possible. There is usually only one combination of recipes that results in "
                                        + "the highest number of output items. Because \"" + MAXIMIZE_OUTPUT_ITEMS.title + "\" is "
                                        + "set as more important than this, the calculator will always pick the "
                                        + "combination of recipes that results in the maximum number of output items, "
                                        + "and will not consider power consumption unless there are multiple "
                                        + "combinations of recipes that all result in the exact same maximum number of "
                                        + "items.\n"
                                        + "\n"
                                        + "Try set this as more important than \"" + MAXIMIZE_OUTPUT_ITEMS.title + "\".",
                                () -> targetEnabled.get()
                                        && hasMaximizeOutputItem.get()
                                        && !isMoreImportant(MINIMIZE_POWER_CONSUMPTION, MAXIMIZE_OUTPUT_ITEMS, order)
                        ),
                        new Warning(
                                "Will probably result in a minimal plan",
                                "The easiest way to reduce power consumption is to produce nothing at all. "
                                        + "Having power as the most important factor will result in a production plan "
                                        + "that uses as little power as possible - by producing as little as possible."
                                        + "\n\n"
                                        + "If you set \"" + MAXIMIZE_INPUT_ITEMS.title + "\" as more important than minimizing "
                                        + "power, then that would force the calculator to use up as much of the input "
                                        + "items as possible, so not an minimal plan. You probably want to set "
                                        + "\"" + MINIMIZE_SURPLUS.title + "\" as more important than minimizing power usage too, to "
                                        + "ensure you get the correct output items.",
                                () -> targetEnabled.get()
                                        && hasMaximizeOutputItem.get()
                                        && !isMoreImportant(MAXIMIZE_INPUT_ITEMS, MINIMIZE_POWER_CONSUMPTION, order)
                        ),
                        new Warning(
                                "May result in unprocessed by-products",
                                "The easiest way to reduce power consumption is to produce nothing at all. "
                                        + "Having power as the most important factor will result in a production plan "
                                        + "that uses as little power as possible - by producing as little as possible."
                                        + "\n\n"
                                        + "If you set \"" + MAXIMIZE_INPUT_ITEMS.title + "\" as more important than minimizing "
                                        + "power, then that would force the calculator to use up as much of the input "
                                        + "items as possible, so not an minimal plan. You probably want to set "
                                        + "\"" + MINIMIZE_SURPLUS.title + "\" as more important than minimizing power usage too, to "
                                        + "ensure you get the correct output items.",
                                () -> targetEnabled.get()
                                        && hasMaximizeOutputItem.get()
                                        && !isMoreImportant(MAXIMIZE_OUTPUT_ITEMS, MINIMIZE_POWER_CONSUMPTION, order)
                                        && !isMoreImportant(MAXIMIZE_INPUT_ITEMS, MINIMIZE_POWER_CONSUMPTION, order)
                        ),
                };
            }
        };

        private final String saveCode;
        private final String title;
        private final String description;

        OptimizationTarget(String saveCode, String title, String description)
        {
            this.saveCode = saveCode;
            this.title = title;
            this.description = description;
        }

        public static OptimizationTarget forSaveCode(String saveCode)
        {
            for (OptimizationTarget target : values()){
                if (target.saveCode.equals(saveCode)){
                    return target;
                }
            }
            return null;
        }

        abstract Warning[] createWarnings(ObservableBooleanValue targetEnabled, ObservableBooleanValue hasMaximizeOutputItem, ObservableList<OptimizationTarget> order);

        public String getDescription()
        {
            return description;
        }

        public String getSaveCode()
        {
            return saveCode;
        }

        public String getTitle()
        {
            return title;
        }

        public abstract void setWeight(ProductionPlanner.Builder pb, BigFraction weight);
    }

    private static class Warning
    {
        public final String title;
        public final String description;
        public final BooleanSupplier condition;

        public Warning(String title, String description, BooleanSupplier condition)
        {
            this.title = title;
            this.description = description;
            this.condition = condition;
        }
    }
}
