/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.satisdata;

import io.github.elcheapogary.satisplanory.ui.SatisfactoryDataLoader;
import io.github.elcheapogary.satisplanory.ui.jfx.context.AppContext;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.ExceptionDialog;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.TaskProgressDialog;
import java.io.File;
import javafx.application.Platform;

public class SatisfactoryDataLoaderUi
{
    private SatisfactoryDataLoaderUi()
    {
    }

    public static void loadSatisfactoryData(AppContext appContext, File satisfactoryInstallationPath)
    {
        try {
            new TaskProgressDialog(appContext)
                    .setTitle("Loading Satisfactory data")
                    .setContentText("Loading Satisfactory data")
                    .setCancellable(false)
                    .runTask(taskContext -> {
                        var gameData = SatisfactoryDataLoader.loadSatisfactoryData(satisfactoryInstallationPath).build();
                        appContext.getPersistentData().setSatisfactoryPath(satisfactoryInstallationPath.getAbsolutePath());
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
