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

import io.github.elcheapogary.satisplanory.model.GameData;
import io.github.elcheapogary.satisplanory.ui.jfx.context.AppContext;
import io.github.elcheapogary.satisplanory.ui.jfx.persist.PersistentProductionPlan;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;

public class ProdPlanTab
{
    private ProdPlanTab(){}

    public static Tab create(AppContext appContext, GameData gameData, PersistentProductionPlan persistentPlan)
    {
        Tab retv = new Tab();
        retv.setClosable(true);

        StringProperty planNameProperty = persistentPlan.nameProperty();

        Label label = new Label();
        label.textProperty().bind(planNameProperty);
        retv.setGraphic(label);

        TextField tf = new TextField();
        tf.onActionProperty().set(event -> {
            String name = tf.getText();
            if (!name.isBlank()){
                planNameProperty.set(name);
            }
            retv.setGraphic(label);
        });
        tf.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (!focused){
                retv.setGraphic(label);
            }
        });

        label.onMouseClickedProperty().set(event -> {
            if (event.getClickCount() == 2){
                tf.setText(planNameProperty.getValue());
                retv.setGraphic(tf);
                tf.selectAll();
                tf.requestFocus();
            }
        });

        TabPane subTabPane = new TabPane();
        InputTab.addInputTab(appContext, gameData, subTabPane, new ProdPlanData(gameData, persistentPlan));
        retv.setContent(subTabPane);

        return retv;
    }
}
