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

import java.math.BigDecimal;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

class SettingsPane
{
    private SettingsPane()
    {
    }

    public static TitledPane createSettingsPane(ProdPlanData.Settings settings)
    {
        TitledPane titledPane = new TitledPane();
        titledPane.setText("Recipe Selection");
        titledPane.setCollapsible(true);

        VBox vbox = new VBox(10);
        vbox.setFillWidth(true);
        titledPane.setContent(new ScrollPane(vbox));

        vbox.setPadding(new Insets(10));

        vbox.getChildren().add(new Label("Tweak the calculator by configuring the importance of each of these:"));

        addImportanceSlider(
                vbox, "Maintain balance between maximized output items:",
                () -> settings.weights.balance,
                value -> settings.weights.balance = value
        );

        addImportanceSlider(
                vbox, "Maximize output items:",
                () -> settings.weights.maximizeOutputItems,
                value -> settings.weights.maximizeOutputItems = value
        );

        addImportanceSlider(
                vbox, "Minimize input items:",
                () -> settings.weights.minimizeInputItems,
                value -> settings.weights.minimizeInputItems = value
        );

        addImportanceSlider(
                vbox, "Maximize input items:",
                () -> settings.weights.maximizeInputItems,
                value -> settings.weights.maximizeInputItems = value
        );

        addImportanceSlider(
                vbox, "Minimize power usage:",
                () -> settings.weights.power,
                value -> settings.weights.power = value
        );

        addImportanceSlider(
                vbox, "Minimize by-products:",
                () -> settings.weights.minimizeByProducts,
                value -> settings.weights.minimizeByProducts = value
        );

        return titledPane;
    }

    private static void addImportanceSlider(VBox vbox, String name, IntSupplier valueGetter, IntConsumer valueSetter)
    {
        vbox.getChildren().add(new Label(name));
        Slider slider = new Slider();
        slider.setMin(0);
        slider.setMax(15);
        slider.setMajorTickUnit(1);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(false);
        slider.setSnapToTicks(true);
        slider.setMinorTickCount(0);
        slider.setValue(valueGetter.getAsInt());
        slider.valueProperty().addListener((observable, oldValue, newValue) -> valueSetter.accept((int) newValue.doubleValue()));
        VBox.setMargin(slider, new Insets(0, 0, 0, 40));
        vbox.getChildren().add(slider);
    }

    public static BigDecimal importanceIntToWeight(int importance)
    {
        if (importance <= 0){
            return BigDecimal.ZERO;
        }

        return BigDecimal.ONE.movePointLeft((8 - importance) * 3);
    }
}
