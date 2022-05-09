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
import io.github.elcheapogary.satisplanory.ui.jfx.context.AppContext;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.ExceptionDialog;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.TaskProgressDialog;
import io.github.elcheapogary.satisplanory.ui.jfx.persist.PersistentData;
import io.github.elcheapogary.satisplanory.ui.jfx.persist.SatisplanoryPersistence;
import io.github.elcheapogary.satisplanory.ui.jfx.style.Style;
import java.io.File;
import java.io.IOException;
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
        PersistentData persistentData = new PersistentData();
        try {
            persistentData = SatisplanoryPersistence.load();
        }catch (IOException e){
            // do nothing for now
        }
        Style.init();
        final AppContext appContext = new AppContext();
        appContext.setPersistentData(persistentData);
        stage.getIcons().clear();
        stage.getIcons().add(new Image(MainPane.class.getResource("icon/sp-16.png").toString()));
        stage.getIcons().add(new Image(MainPane.class.getResource("icon/sp-32.png").toString()));
        stage.getIcons().add(new Image(MainPane.class.getResource("icon/sp-64.png").toString()));
        stage.getIcons().add(new Image(MainPane.class.getResource("icon/sp-128.png").toString()));
        stage.getIcons().add(new Image(MainPane.class.getResource("icon/sp-256.png").toString()));
        stage.setMaximized(true);
        stage.setTitle(Satisplanory.getApplicationName());
        Scene scene = new Scene(MainPane.createMainPane(this, stage, appContext));
        scene.getStylesheets().add(Style.getCustomStylesheet());
        if (appContext.getPersistentData().getPreferences().getUiPreferences().isDarkModeEnabled()){
            scene.getStylesheets().add(Style.getDarkModeStylesheet());
        }
        appContext.getPersistentData().getPreferences().getUiPreferences()
                .darkModeEnabledProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue){
                        scene.getStylesheets().add(Style.getDarkModeStylesheet());
                    }else{
                        scene.getStylesheets().remove(Style.getDarkModeStylesheet());
                    }
                });

        stage.setScene(scene);
        stage.show();
        Platform.runLater(() -> stage.setMaximized(true));
        if (appContext.getPersistentData().getSatisfactoryPath() != null){
            try {
                new TaskProgressDialog(appContext)
                        .setTitle("Loading Satisfactory data")
                        .setContentText("Loading Satisfactory data")
                        .setCancellable(false)
                        .runTask(taskContext -> {
                            var gameData = SatisfactoryDataLoader.loadSatisfactoryData(new File(appContext.getPersistentData().getSatisfactoryPath())).build();
                            Platform.runLater(() -> appContext.setGameData(gameData));
                            return null;
                        })
                        .get();
            }catch (Exception e){
                new ExceptionDialog(appContext)
                        .setTitle("Error loading Satisfactory data")
                        .setContextMessage("Error loading Satisfactory data")
                        .setException(e)
                        .showAndWait();
            }
        }
    }
}
