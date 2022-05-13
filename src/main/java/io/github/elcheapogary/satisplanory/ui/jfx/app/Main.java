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
import io.github.elcheapogary.satisplanory.satisfactory.SatisfactoryInstallation;
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
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;

public class Main
        extends Application
{
    private AppContext appContext = new AppContext();

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
        try {
            new TaskProgressDialog(appContext)
                    .setTitle("Loading Satisfactory data")
                    .setContentText("Loading Satisfactory data")
                    .setCancellable(false)
                    .runTask(taskContext -> {
                        File satisfactoryInstallation = null;
                        if (appContext.getPersistentData().getSatisfactoryPath() != null){
                            satisfactoryInstallation = new File(appContext.getPersistentData().getSatisfactoryPath());
                            if (!SatisfactoryInstallation.isValidSatisfactoryInstallation(satisfactoryInstallation)){
                                satisfactoryInstallation = null;
                            }
                        }
                        if (satisfactoryInstallation == null){
                            satisfactoryInstallation = SatisfactoryInstallation.findSatisfactoryInstallation();
                        }
                        if (satisfactoryInstallation != null){
                            var gameData = SatisfactoryDataLoader.loadSatisfactoryData(satisfactoryInstallation).build();
                            appContext.getPersistentData().setSatisfactoryPath(satisfactoryInstallation.getAbsolutePath());
                            Platform.runLater(() -> appContext.setGameData(gameData));
                        }
                        return null;
                    })
                    .get();
            Notifications.create()
                    .position(Pos.TOP_CENTER)
                    .title("Remember to check for updates")
                    .text("See Help -> About for links")
                    .show();
        }catch (Exception e){
            new ExceptionDialog(appContext)
                    .setTitle("Error loading Satisfactory data")
                    .setContextMessage("Error loading Satisfactory data")
                    .setException(e)
                    .showAndWait();
        }
    }

    @Override
    public void stop()
            throws Exception
    {
        SatisplanoryPersistence.save(appContext, appContext.getPersistentData());
    }
}
