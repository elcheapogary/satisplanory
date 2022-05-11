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

import io.github.elcheapogary.satisplanory.prodplan.OptimizationTarget;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

class SettingsPane
{
    private SettingsPane()
    {
    }

    public static TitledPane createSettingsPane(ProdPlanData.Settings settings)
    {
        GridPane gp = new GridPane();
        gp.setPadding(new Insets(10));

        {
            ColumnConstraints c = new ColumnConstraints();
            c.setHgrow(Priority.NEVER);
            gp.getColumnConstraints().add(c);
        }

        {
            ColumnConstraints c = new ColumnConstraints();
            c.setHgrow(Priority.ALWAYS);
            c.setFillWidth(true);
            gp.getColumnConstraints().add(c);
        }

        int row = 0;

        {
            gp.add(new Label("Optimize for: "), 0, row);
            ComboBox<OptimizationTarget> comboBox = new ComboBox<>();
            comboBox.setEditable(false);
            comboBox.getItems().addAll(OptimizationTarget.values());
            comboBox.getSelectionModel().select(OptimizationTarget.MAX_OUTPUT_ITEMS);
            settings.optimizationTargetProperty().bind(comboBox.getSelectionModel().selectedItemProperty());
            GridPane.setMargin(comboBox, new Insets(0, 0, 0, 10));
            gp.add(comboBox, 1, row);
            row++;
            Label l = new Label();
            l.setWrapText(true);
            l.textProperty().bind(Bindings.createStringBinding(() -> {
                OptimizationTarget t = settings.getOptimizationTarget();
                if (t == null){
                    return "";
                }else{
                    return t.getDescription();
                }
            }, settings.optimizationTargetProperty()));
            GridPane.setMargin(l, new Insets(10, 0, 0, 10));
            gp.add(l, 1, row);
        }

        row++;

        TitledPane titledPane = new TitledPane();
        titledPane.setCollapsible(true);
        ScrollPane scrollPane = new ScrollPane(gp);
        scrollPane.setFitToWidth(true);
        titledPane.setContent(scrollPane);
        titledPane.setText("Settings");
        return titledPane;
    }

}
