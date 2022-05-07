/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx;

import io.github.elcheapogary.satisplanory.model.GameData;
import io.github.elcheapogary.satisplanory.ui.SatisfactoryDataLoader;
import io.github.elcheapogary.satisplanory.ui.jfx.data.AppData;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.ExceptionDialog;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.TaskProgressDialog;
import io.github.elcheapogary.satisplanory.ui.jfx.persist.SatisplanoryPersistence;
import java.io.File;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

public class HomeTab
{
    private HomeTab()
    {
    }

    public static Tab create(AppData appData)
    {
        Tab retv = new Tab("Home", createPane(appData));
        retv.setClosable(false);
        return retv;
    }

    private static Node createLoadSatisfactoryDataPane(AppData appData)
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
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Satisfactory Directory");

            while (true){
                File f = dc.showDialog(button.getScene().getWindow());

                if (f == null){
                    break;
                }else{
                    try {
                        new TaskProgressDialog()
                                .setTitle("Loading Satisfactory data")
                                .setContentText("Loading Satisfactory data")
                                .setCancellable(false)
                                .runTask(taskContext -> {
                                    GameData gameData = SatisfactoryDataLoader.loadSatisfactoryData(f).build();
                                    Platform.runLater(() -> {
                                        appData.setGameData(gameData);
                                        appData.getPersistentData().setSatisfactoryPath(f.getPath());
                                        SatisplanoryPersistence.save(appData.getPersistentData());
                                    });
                                    return null;
                                })
                                .get();
                    }catch (Exception e){
                        new ExceptionDialog()
                                .setTitle("Error loading Satisfactory data")
                                .setContextMessage("Error loading Satisfactory data")
                                .setException(e)
                                .showAndWait();
                        continue;
                    }
                    break;
                }
            }
        });

        vbox.getChildren().add(button);

        TitledPane tp = new TitledPane();
        tp.setContent(vbox);
        tp.setCollapsible(false);
        tp.setText("Satisfactory data required");

        tp.visibleProperty().bind(appData.gameDataProperty().isNull());

        return tp;
    }

    private static Pane createPane(AppData appData)
    {
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(10));

        vbox.setMaxWidth(Double.MAX_VALUE);

        vbox.getChildren().add(createLoadSatisfactoryDataPane(appData));

        return vbox;
    }
}
