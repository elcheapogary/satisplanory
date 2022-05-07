/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.app;

import io.github.elcheapogary.satisplanory.Satisplanory;
import io.github.elcheapogary.satisplanory.ui.SatisfactoryDataLoader;
import io.github.elcheapogary.satisplanory.ui.jfx.MainPane;
import io.github.elcheapogary.satisplanory.ui.jfx.data.AppData;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.ExceptionDialog;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.TaskProgressDialog;
import io.github.elcheapogary.satisplanory.ui.jfx.persist.PersistentData;
import io.github.elcheapogary.satisplanory.ui.jfx.persist.SatisplanoryPersistence;
import java.io.File;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main
        extends Application
{
    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage stage)
    {
        final AppData appData = new AppData();
        stage.getIcons().clear();
        stage.getIcons().add(new Image(MainPane.class.getResource("icon/sp-16.png").toString()));
        stage.getIcons().add(new Image(MainPane.class.getResource("icon/sp-32.png").toString()));
        stage.getIcons().add(new Image(MainPane.class.getResource("icon/sp-64.png").toString()));
        stage.getIcons().add(new Image(MainPane.class.getResource("icon/sp-128.png").toString()));
        stage.getIcons().add(new Image(MainPane.class.getResource("icon/sp-256.png").toString()));
        stage.setMaximized(true);
        stage.setTitle(Satisplanory.getApplicationName());
        stage.setScene(new Scene(MainPane.createMainPane(this, stage, appData)));
        stage.show();
        Platform.runLater(() -> stage.setMaximized(true));
        try {
            PersistentData data = new TaskProgressDialog()
                    .setTitle("Loading")
                    .setContentText("Loading persistent data")
                    .setCancellable(false)
                    .runTask(
                            taskContext -> SatisplanoryPersistence.load()
                    )
                    .get();
            appData.setPersistentData(data);
        }catch (Exception e){
            new ExceptionDialog()
                    .setTitle("Error loading data")
                    .setContextMessage("Error loading Satisplanory persistent data")
                    .setException(e)
                    .showAndWait();
            return;
        }
        if (appData.getPersistentData().getSatisfactoryPath() != null){
            try {
                new TaskProgressDialog()
                        .setTitle("Loading Satisfactory data")
                        .setContentText("Loading Satisfactory data")
                        .setCancellable(false)
                        .runTask(taskContext -> {
                            var gameData = SatisfactoryDataLoader.loadSatisfactoryData(new File(appData.getPersistentData().getSatisfactoryPath())).build();
                            Platform.runLater(() -> appData.setGameData(gameData));
                            return null;
                        })
                        .get();
            }catch (Exception e){
                new ExceptionDialog()
                        .setTitle("Error loading Satisfactory data")
                        .setContextMessage("Error loading Satisfactory data")
                        .setException(e)
                        .showAndWait();
            }
        }
    }
}
