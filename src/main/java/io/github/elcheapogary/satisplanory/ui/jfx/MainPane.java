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

import io.github.elcheapogary.satisplanory.Satisplanory;
import io.github.elcheapogary.satisplanory.satisfactory.SatisfactoryInstallation;
import io.github.elcheapogary.satisplanory.ui.jfx.context.AppContext;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.ExceptionDialog;
import io.github.elcheapogary.satisplanory.ui.jfx.persist.SatisplanoryPersistence;
import io.github.elcheapogary.satisplanory.ui.jfx.prodplan.ProdPlanBrowser;
import io.github.elcheapogary.satisplanory.ui.jfx.satisdata.SatisfactoryDataLoaderUi;
import io.github.elcheapogary.satisplanory.ui.jfx.satisdata.SatisfactoryInstallationSelectorDialog;
import java.io.IOException;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
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

    private static Menu createFileMenu(Application application, Stage stage, AppContext appContext)
    {
        Menu fileMenu = new Menu("File");

        MenuItem selectSatisfactoryInstallationMenuItem = new MenuItem("Select Satisfactory Installation");
        fileMenu.getItems().add(selectSatisfactoryInstallationMenuItem);

        selectSatisfactoryInstallationMenuItem.onActionProperty().set(event -> {
            SatisfactoryInstallationSelectorDialog.show(appContext, SatisfactoryInstallation.findSatisfactoryInstallations())
                    .ifPresent(satisfactoryInstallation -> {
                        SatisfactoryDataLoaderUi.loadSatisfactoryData(appContext, satisfactoryInstallation.getPath());
                    });
        });

        fileMenu.getItems().add(new SeparatorMenuItem());

        MenuItem saveMenuItem = new MenuItem("Save Data");
        fileMenu.getItems().add(saveMenuItem);

        saveMenuItem.onActionProperty().set(event -> {
            SatisplanoryPersistence.save(appContext, appContext.getPersistentData());
        });

        fileMenu.getItems().add(new SeparatorMenuItem());

        MenuItem exitMenuItem = new MenuItem("Exit");
        fileMenu.getItems().add(exitMenuItem);

        exitMenuItem.onActionProperty().setValue(event -> stage.close());

        return fileMenu;
    }

    private static Menu createHelpMenu(Application application, Stage stage, AppContext appContext, TabPane mainTabPane)
    {
        Menu helpMenu = new Menu("Help");

        MenuItem licensesMenuItem = new MenuItem("About");
        helpMenu.getItems().add(licensesMenuItem);

        licensesMenuItem.onActionProperty().setValue(actionEvent -> {
            try {
                Tab tab = new Tab("About " + Satisplanory.getApplicationName());
                tab.setClosable(true);
                tab.setContent(AboutPane.create(application, appContext));
                mainTabPane.getTabs().add(tab);
                mainTabPane.getSelectionModel().selectLast();
            }catch (IOException e){
                new ExceptionDialog(appContext)
                        .setTitle("Error loading data")
                        .setContextMessage("An error occurred while loading data")
                        .setException(e)
                        .showAndWait();
            }
        });

        return helpMenu;
    }

    public static Pane createMainPane(Application application, Stage stage, AppContext appContext)
    {
        BorderPane borderPane = new BorderPane();

        TabPane mainTabPage = createTabPane(appContext);

        borderPane.setTop(createMenuBar(application, appContext, stage, mainTabPage));

        borderPane.setCenter(mainTabPage);

        return borderPane;
    }

    private static MenuBar createMenuBar(Application application, AppContext appContext, Stage stage, TabPane mainTabPane)
    {
        MenuBar mainMenuBar = new MenuBar();

        mainMenuBar.getMenus().add(createFileMenu(application, stage, appContext));
        mainMenuBar.getMenus().add(createOptionsMenu(appContext));
        mainMenuBar.getMenus().add(createHelpMenu(application, stage, appContext, mainTabPane));

        return mainMenuBar;
    }

    private static Menu createOptionsMenu(AppContext appContext)
    {
        Menu menu = new Menu("Options");

        CheckMenuItem darkModeMenuItem = new CheckMenuItem();
        menu.getItems().add(darkModeMenuItem);
        darkModeMenuItem.setText("Dark mode");
        BooleanProperty darkModeProperty = appContext.getPersistentData().getPreferences().getUiPreferences().darkModeEnabledProperty();
        darkModeMenuItem.selectedProperty().bindBidirectional(darkModeProperty);

        return menu;
    }

    private static TabPane createTabPane(AppContext appContext)
    {
        TabPane tabPane = new TabPane();

        final Tab homeTab = HomeTab.create(appContext);
        tabPane.getTabs().add(homeTab);

        appContext.gameDataProperty().addListener((observable, oldValue, gameData) -> {
            if (gameData != null){
                tabPane.getTabs().clear();
                tabPane.getTabs().add(CodexPane.createTab(gameData));
                tabPane.getTabs().add(ProdPlanBrowser.create(appContext, gameData));
            }
        });

        return tabPane;
    }
}
