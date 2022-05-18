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
import io.github.elcheapogary.satisplanory.ui.jfx.persist.UnsupportedVersionException;
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
    private final AppContext appContext = new AppContext();
    private boolean saveOnExit = false;

    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage stage)
    {
        Style.init();
        stage.getIcons().clear();
        stage.getIcons().add(new Image(MainPane.class.getResource("icon/sp-16.png").toString()));
        stage.getIcons().add(new Image(MainPane.class.getResource("icon/sp-32.png").toString()));
        stage.getIcons().add(new Image(MainPane.class.getResource("icon/sp-64.png").toString()));
        stage.getIcons().add(new Image(MainPane.class.getResource("icon/sp-128.png").toString()));
        stage.getIcons().add(new Image(MainPane.class.getResource("icon/sp-256.png").toString()));
        stage.setMaximized(true);
        stage.setTitle(Satisplanory.getApplicationName());
        appContext.setPersistentData(new PersistentData());
        try {
            appContext.setPersistentData(SatisplanoryPersistence.load());
        }catch (IOException e){
            new ExceptionDialog(appContext)
                    .setTitle("Error loading Satisplanory data")
                    .setContextMessage("An error occurred while attempting to load the Satisplanory data")
                    .setDetailsMessage("Satisplanory was not able to load this file: " + SatisplanoryPersistence.getJsonFile().getAbsolutePath())
                    .setException(e)
                    .showAndWait();
            return;
        }catch (UnsupportedVersionException e){
            new ExceptionDialog(appContext)
                    .setTitle("Data is from newer version of Satisplanory")
                    .setContextMessage("Your Satisplanory data is from a newer version - please use that version")
                    .setDetailsMessage("Your data stored in:\n\n    " + SatisplanoryPersistence.getJsonFile().getAbsolutePath()
                            + "\n\n"
                            + "is from a newer version of Satisplanory. Using an older version may result in data\n"
                            + "loss. You must have used a newer version at some point. Please continue using\n"
                            + "that version.\n\n"
                            + "You can always download the latest version at:\n\n"
                            + "    https://github.com/elcheapogary/satisplanory/")
                    .setException(e)
                    .showAndWait();
            return;
        }
        saveOnExit = true;
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
    {
        if (saveOnExit){
            SatisplanoryPersistence.save(appContext, appContext.getPersistentData());
        }
    }
}
