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

import io.github.elcheapogary.satisplanory.ui.jfx.context.AppContext;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;

class ProdPlanTab
{
    private ProdPlanTab()
    {
    }

    private static void addRemoveTabs(ProdPlanModel model, TabPane tabPane, Tab errorTab, Tab overviewTab, Tab graphTab, Tab tableTab)
    {
        if (model.getPlan() == null){
            tabPane.getTabs().remove(overviewTab);
            tabPane.getTabs().remove(graphTab);
            tabPane.getTabs().remove(tableTab);

            if (model.getMultiPlan() != null){
                if (!tabPane.getTabs().contains(errorTab)){
                    /*
                     * There is a bug somewhere in JavaFX that causes an IndexOutOfBoundsException if we don't add at
                     * the right index here. After removing the previous tabs, somewhere in the bowels, it still
                     * attempts to add at index 4, not 1, and that causes an issue.
                     *
                     * Inserting at index 1 fixes that.
                     */
                    tabPane.getTabs().add(1, errorTab);
                }
                tabPane.getSelectionModel().select(errorTab);
            }else{
                tabPane.getTabs().remove(errorTab);
            }
        }else{
            tabPane.getTabs().remove(errorTab);
            /*
             * Have to add at index here to avoid bug described above.
             */
            tabPane.getTabs().add(1, overviewTab);
            tabPane.getTabs().add(2, graphTab);
            tabPane.getTabs().add(3, tableTab);
            tabPane.getSelectionModel().select(graphTab);
        }
    }

    public static Tab create(AppContext appContext, ProdPlanModel model)
    {
        Tab tab = new Tab();
        tab.setClosable(true);

        setUpTabHeading(tab, model);

        TabPane tabPane = new TabPane();
        tab.setContent(tabPane);

        tabPane.getTabs().add(ConfigTab.create(appContext, model));

        final Tab errorTab = ErrorTab.create(model);
        final Tab overviewTab = OverviewTab.create(appContext, model);
        final Tab graphTab = GraphTab.create(appContext, model);
        final Tab tableTab = TableTab.create(appContext, model);

        addRemoveTabs(model, tabPane, errorTab, overviewTab, graphTab, tableTab);

        model.planProperty().addListener((observable, oldValue, newValue) -> addRemoveTabs(model, tabPane, errorTab, overviewTab, graphTab, tableTab));

        model.multiPlanProperty().addListener((observable, oldValue, newValue) -> addRemoveTabs(model, tabPane, errorTab, overviewTab, graphTab, tableTab));

        return tab;
    }

    private static void setUpTabHeading(Tab tab, ProdPlanModel model)
    {
        Label label = new Label();
        label.textProperty().bind(model.nameProperty());
        tab.setGraphic(label);

        TextField tf = new TextField();

        tf.onActionProperty().set(event -> {
            String name = tf.getText();
            if (!name.isBlank()){
                model.setName(name);
            }
            tab.setGraphic(label);
        });

        tf.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (!focused){
                tab.setGraphic(label);
            }
        });

        label.onMouseClickedProperty().set(event -> {
            if (event.getClickCount() == 2){
                tf.setText(model.getName());
                tab.setGraphic(tf);
                tf.selectAll();
                tf.requestFocus();
            }
        });
    }
}
