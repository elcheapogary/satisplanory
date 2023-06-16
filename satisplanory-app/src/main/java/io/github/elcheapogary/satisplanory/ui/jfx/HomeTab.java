/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx;

import io.github.elcheapogary.satisplanory.satisfactory.SatisfactoryInstallation;
import io.github.elcheapogary.satisplanory.ui.jfx.context.AppContext;
import io.github.elcheapogary.satisplanory.ui.jfx.satisdata.SatisfactoryDataLoaderUi;
import io.github.elcheapogary.satisplanory.ui.jfx.satisdata.SatisfactoryInstallationSelectorDialog;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class HomeTab
{
    private HomeTab()
    {
    }

    public static Tab create(AppContext appContext)
    {
        Tab retv = new Tab("Home", createPane(appContext));
        retv.setClosable(false);
        return retv;
    }

    private static Node createLoadSatisfactoryDataPane(AppContext appContext)
    {
        VBox vbox = new VBox();
        vbox.setSpacing(10);
        vbox.setPadding(new Insets(10));

        Label messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setText("""
                Satisplanory needs to load data from your Satisfactory installation in order to work.

                Don't worry, Satisplanory only reads the data - it makes no changes to your Satisfactory installation at all.

                Please click the button below to browse to your Satisfactory installation. You should only need to do this the first time you use Satisplanory."""
        );

        vbox.getChildren().add(messageLabel);

        Button button = new Button("Select Satisfactory installation");

        button.setOnAction(event -> {
            SatisfactoryInstallationSelectorDialog.show(appContext, SatisfactoryInstallation.findSatisfactoryInstallations())
                    .ifPresent(satisfactoryInstallation -> SatisfactoryDataLoaderUi.loadSatisfactoryData(appContext, satisfactoryInstallation.getPath()));
        });

        vbox.getChildren().add(button);

        TitledPane tp = new TitledPane();
        tp.setContent(vbox);
        tp.setCollapsible(false);
        tp.setText("Satisfactory data required");

        tp.visibleProperty().bind(appContext.gameDataProperty().isNull());

        return tp;
    }

    private static Pane createPane(AppContext appContext)
    {
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(10));

        vbox.setMaxWidth(Double.MAX_VALUE);

        vbox.getChildren().add(createLoadSatisfactoryDataPane(appContext));

        return vbox;
    }
}
