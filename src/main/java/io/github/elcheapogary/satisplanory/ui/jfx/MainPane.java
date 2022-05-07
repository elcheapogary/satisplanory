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

import io.github.elcheapogary.satisplanory.Satisplanory;
import io.github.elcheapogary.satisplanory.ui.jfx.data.AppData;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.ExceptionDialog;
import io.github.elcheapogary.satisplanory.ui.jfx.prodplan.ProdPlanTab;
import java.io.IOException;
import javafx.application.Application;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class MainPane
{
    private MainPane()
    {
    }

    private static Menu createFileMenu(Application application, Stage stage)
    {
        Menu fileMenu = new Menu("File");

        MenuItem exitMenuItem = new MenuItem("Exit");
        fileMenu.getItems().add(exitMenuItem);

        exitMenuItem.onActionProperty().setValue(event -> stage.close());

        return fileMenu;
    }

    private static Menu createHelpMenu(Application application, Stage stage, TabPane mainTabPane)
    {
        Menu helpMenu = new Menu("Help");

        MenuItem licensesMenuItem = new MenuItem("About");
        helpMenu.getItems().add(licensesMenuItem);

        licensesMenuItem.onActionProperty().setValue(actionEvent -> {
            try {
                Tab tab = new Tab("About " + Satisplanory.getApplicationName());
                tab.setClosable(true);
                tab.setContent(AboutPane.create(application));
                mainTabPane.getTabs().add(tab);
                mainTabPane.getSelectionModel().selectLast();
            }catch (IOException e){
                new ExceptionDialog()
                        .setTitle("Error loading data")
                        .setContextMessage("An error occurred while loading data")
                        .setException(e)
                        .showAndWait();
            }
        });

        return helpMenu;
    }

    public static Pane createMainPane(Application application, Stage stage, AppData appData)
    {
        BorderPane borderPane = new BorderPane();

        TabPane mainTabPage = createTabPane(appData);

        borderPane.setTop(createMenuBar(application, stage, mainTabPage));

        borderPane.setCenter(mainTabPage);

        return borderPane;
    }

    private static MenuBar createMenuBar(Application application, Stage stage, TabPane mainTabPane)
    {
        MenuBar mainMenuBar = new MenuBar();

        mainMenuBar.getMenus().add(createFileMenu(application, stage));
        mainMenuBar.getMenus().add(createHelpMenu(application, stage, mainTabPane));

        return mainMenuBar;
    }

    private static TabPane createTabPane(AppData appData)
    {
        TabPane tabPane = new TabPane();

        final Tab homeTab = HomeTab.create(appData);
        tabPane.getTabs().add(homeTab);

        appData.gameDataProperty().addListener((observable, oldValue, gameData) -> {
            if (gameData != null){
                tabPane.getTabs().remove(homeTab);
                tabPane.getTabs().add(CodexPane.createTab(gameData));
                tabPane.getTabs().add(ProdPlanTab.create(gameData));
            }
        });

        return tabPane;
    }
}
