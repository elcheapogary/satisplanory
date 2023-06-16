/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.prodplan;

import io.github.elcheapogary.satisplanory.model.GameData;
import io.github.elcheapogary.satisplanory.model.Recipe;
import io.github.elcheapogary.satisplanory.ui.jfx.context.AppContext;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.ExceptionDialog;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.TaskProgressDialog;
import io.github.elcheapogary.satisplanory.ui.jfx.persist.PersistentProductionPlan;
import io.github.elcheapogary.satisplanory.ui.jfx.persist.UnsupportedVersionException;
import io.github.elcheapogary.satisplanory.ui.jfx.style.Style;
import io.github.elcheapogary.satisplanory.util.JsonUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import org.controlsfx.control.Notifications;

public class ProdPlanBrowser
{
    private ProdPlanBrowser()
    {
    }

    public static Tab create(AppContext appContext, GameData gameData)
    {
        Tab tab = new Tab("Production Plans");
        tab.setClosable(false);

        tab.setContent(createBody(appContext, gameData));

        return tab;
    }

    private static Region createBody(AppContext appContext, GameData gameData)
    {
        HBox hbox = new HBox();
        hbox.setFillHeight(true);

        TabPane tabPane = new TabPane();
        tabPane.setPrefWidth(0);

        Node list = createList(appContext, gameData, tabPane);
        HBox.setHgrow(list, Priority.NEVER);
        hbox.getChildren().add(list);

        HBox.setHgrow(tabPane, Priority.ALWAYS);
        hbox.getChildren().add(tabPane);

        return hbox;
    }

    private static Node createList(AppContext appContext, GameData gameData, TabPane tabPane)
    {
        interface PlanOpener
        {
            default void openPlan(PersistentProductionPlan plan)
            {
                openPlan(plan, null);
            }

            void openPlan(PersistentProductionPlan plan, Consumer<? super ProdPlanModel> modelConfigurator);
        }
        ObservableMap<PersistentProductionPlan, Tab> tabMap = FXCollections.observableMap(new HashMap<>());
        VBox vbox = new VBox(10);
        vbox.setStyle("-fx-border-width: 0px 1px 0px 0px; -fx-border-color: -fx-box-border;");
        vbox.setPadding(new Insets(10));

        vbox.setFillWidth(true);

        HBox buttons = new HBox(0);
        vbox.getChildren().add(buttons);

        Button newPlanButton = new Button("Create new");
        HBox.setHgrow(newPlanButton, Priority.NEVER);
        buttons.getChildren().add(newPlanButton);

        {
            Region grower = new Region();
            grower.setPrefHeight(0);
            grower.setPrefWidth(10);
            grower.setMinWidth(10);
            HBox.setHgrow(grower, Priority.ALWAYS);
            buttons.getChildren().add(grower);
        }

        Button duplicateButton = new Button("Duplicate");
        HBox.setHgrow(duplicateButton, Priority.NEVER);
        buttons.getChildren().add(duplicateButton);

        {
            Region grower = new Region();
            grower.setPrefHeight(0);
            grower.setPrefWidth(10);
            grower.setMinWidth(10);
            HBox.setHgrow(grower, Priority.ALWAYS);
            buttons.getChildren().add(grower);
        }

        Button deleteButton = new Button("Delete");
        HBox.setHgrow(deleteButton, Priority.NEVER);
        buttons.getChildren().add(deleteButton);

        appContext.getPersistentData().getProductionPlans().sort(Comparator.comparing(PersistentProductionPlan::getName));

        ListView<PersistentProductionPlan> list = new ListView<>(appContext.getPersistentData().getProductionPlans());
        VBox.setVgrow(list, Priority.ALWAYS);
        vbox.getChildren().add(list);

        list.setPrefWidth(250);

        PlanOpener planOpener = (plan, modelConfigurator) -> {
            Tab tab = tabMap.get(plan);
            if (tab == null){
                try{
                    tab = new TaskProgressDialog(appContext)
                            .setTitle("Loading production plan")
                            .setContentText("Loading production plan")
                            .setCancellable(false)
                            .runTask(taskContext -> {
                                ProdPlanModel model = ProdPlanModel.fromPersistent(gameData, plan);
                                FutureTask<Tab> future = new FutureTask<>(() -> {
                                    model.nameProperty().addListener((observable, oldValue, newValue) -> {
                                        appContext.getPersistentData().getProductionPlans().sort(Comparator.comparing(PersistentProductionPlan::getName));
                                        list.refresh();
                                    });
                                    if (modelConfigurator != null){
                                        modelConfigurator.accept(model);
                                    }
                                    return ProdPlanTab.create(appContext, model);
                                });
                                Platform.runLater(future);
                                return future.get();
                            }).get();
                    tab.onClosedProperty().set(e -> tabMap.remove(plan));
                    tabMap.put(plan, tab);
                }catch (Exception e){
                    new ExceptionDialog(appContext)
                            .setTitle("Error loading production plan")
                            .setContextMessage("An error occurred while loading the production plan")
                            .setException(e)
                            .showAndWait();
                    return;
                }
            }
            if (!tabPane.getTabs().contains(tab)){
                tabPane.getTabs().add(tab);
            }
            list.getSelectionModel().select(plan);
            tabPane.getSelectionModel().select(tab);
        };

        newPlanButton.onActionProperty().set(event -> {
            TextInputDialog dlg = new TextInputDialog();
            dlg.getDialogPane().getStylesheets().addAll(Style.getStyleSheets(appContext));
            dlg.setHeaderText("Please provide a name for the new factory");
            dlg.setTitle("New factory name");
            dlg.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, ev -> {
                if (dlg.getEditor().getText().isBlank()){
                    ev.consume();
                }
            });
            dlg.showAndWait().ifPresent(name -> {
                PersistentProductionPlan plan = new PersistentProductionPlan();
                plan.setName(name.trim());
                appContext.getPersistentData().getProductionPlans().add(plan);
                planOpener.openPlan(plan, model -> {
                    for (Recipe recipe : gameData.getRecipes()){
                        model.getEnabledRecipes().add(recipe);
                    }
                    model.getSettings().getOptimizationTargets().addAll(OptimizationTargetModel.MAXIMIZE_OUTPUT_ITEMS, OptimizationTargetModel.MINIMIZE_RESOURCE_SCARCITY);
                });
            });
        });

        list.onMouseClickedProperty().set(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2){
                PersistentProductionPlan plan = list.getSelectionModel().getSelectedItem();

                if (plan != null){
                    planOpener.openPlan(plan);
                }
            }
        });

        list.setCellFactory(param -> new TextFieldListCell<>(new StringConverter<>()
        {
            @Override
            public PersistentProductionPlan fromString(String string)
            {
                return null;
            }

            @Override
            public String toString(PersistentProductionPlan object)
            {
                return object.getName();
            }
        }));

        deleteButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            PersistentProductionPlan p = list.getSelectionModel().selectedItemProperty().get();
            if (p == null){
                return true;
            }
            return tabMap.containsKey(p);
        }, list.getSelectionModel().selectedItemProperty(), tabMap));

        deleteButton.onActionProperty().set(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            for (String styleSheet : Style.getStyleSheets(appContext)){
                alert.getDialogPane().getStylesheets().add(styleSheet);
            }
            alert.setTitle("Confirm delete");
            alert.setContentText("Are you sure you want to delete this production plan?");
            alert.getButtonTypes().clear();
            alert.getButtonTypes().add(ButtonType.YES);
            alert.getButtonTypes().add(ButtonType.NO);

            if (alert.showAndWait().orElse(null) == ButtonType.YES){
                list.getItems().remove(list.getSelectionModel().getSelectedItem());
            }
        });

        duplicateButton.disableProperty().bind(Bindings.createBooleanBinding(() -> list.getSelectionModel().selectedItemProperty().get() == null, list.getSelectionModel().selectedItemProperty()));

        duplicateButton.onActionProperty().set(event -> {
            PersistentProductionPlan p = list.getSelectionModel().getSelectedItem();

            TextInputDialog dlg = new TextInputDialog();
            dlg.getEditor().setText(p.getName() + " (Copy)");
            dlg.getDialogPane().getStylesheets().addAll(Style.getStyleSheets(appContext));
            dlg.setHeaderText("Please provide a name for the new factory");
            dlg.setTitle("New factory name");
            dlg.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, ev -> {
                if (dlg.getEditor().getText().isBlank()){
                    ev.consume();
                }
            });
            dlg.showAndWait().ifPresent(name -> {
                PersistentProductionPlan n = new PersistentProductionPlan();
                try{
                    n.loadJson(p.toJson());
                    n.setName(name);
                    appContext.getPersistentData().getProductionPlans().add(n);
                    planOpener.openPlan(n);
                }catch (UnsupportedVersionException e){
                    new ExceptionDialog(appContext)
                            .setTitle("Error duplicating plan")
                            .setContextMessage("This should never happen")
                            .setException(e)
                            .showAndWait();
                }
            });
        });

        HBox importExportButtons = new HBox(10);
        vbox.getChildren().add(importExportButtons);

        MenuButton importButton = new MenuButton("Import");
        importButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(importButton, Priority.ALWAYS);
        importExportButtons.getChildren().add(importButton);

        MenuButton exportButton = new MenuButton("Export");
        exportButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(exportButton, Priority.ALWAYS);
        importExportButtons.getChildren().add(exportButton);

        exportButton.disableProperty().bind(Bindings.createBooleanBinding(() -> list.getSelectionModel().selectedItemProperty().get() == null, list.getSelectionModel().selectedItemProperty()));

        MenuItem importFromLinkMenuItem = new MenuItem("From link");
        importButton.getItems().add(importFromLinkMenuItem);

        importFromLinkMenuItem.onActionProperty().set(event -> {
            PersistentProductionPlan plan = importFromLink(appContext);
            if (plan != null){
                appContext.getPersistentData().getProductionPlans().add(plan);
                planOpener.openPlan(plan);
            }
        });

        MenuItem importFromFileMenuItem = new MenuItem("From file");
        importButton.getItems().add(importFromFileMenuItem);

        importFromFileMenuItem.onActionProperty().set(event -> {
            PersistentProductionPlan plan = importPlanFromFile(appContext, importButton.getScene().getWindow());
            if (plan != null){
                appContext.getPersistentData().getProductionPlans().add(plan);
                planOpener.openPlan(plan);
            }
        });

        MenuItem exportToLinkMenuItem = new MenuItem("To link");
        exportButton.getItems().add(exportToLinkMenuItem);

        exportToLinkMenuItem.onActionProperty().set(event -> exportToLink(appContext, list.getSelectionModel().getSelectedItem()));

        MenuItem exportToFileMenuItem = new MenuItem("To file");
        exportButton.getItems().add(exportToFileMenuItem);

        exportToFileMenuItem.onActionProperty().set(event -> exportToFile(exportButton.getScene().getWindow(), appContext, list.getSelectionModel().getSelectedItem()));

        return vbox;
    }

    private static PersistentProductionPlan importFromLink(AppContext appContext)
    {
        TextInputDialog dialog = new TextInputDialog();

        dialog.setHeaderText("");
        dialog.getDialogPane().getStylesheets().addAll(Style.getStyleSheets(appContext));

        dialog.setTitle("Input URL");
        dialog.setHeaderText("Please enter the link/URL below");
        dialog.setContentText("URL:");

        String url = dialog.showAndWait().orElse(null);

        if (url != null){
            try{
                return new TaskProgressDialog(appContext)
                        .setTitle("Importing link")
                        .setContentText("Downloading and importing link")
                        .setCancellable(true)
                        .runTask(taskContext -> {
                            HttpRequest request = HttpRequest.newBuilder(new URI(url))
                                    .GET()
                                    .build();

                            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

                            if (response.statusCode() != 200){
                                throw new IOException("Response code: " + response.statusCode());
                            }

                            JsonObject json;

                            try (JsonReader r = Json.createReader(new StringReader(response.body()))){
                                json = r.readObject();
                            }

                            PersistentProductionPlan plan = new PersistentProductionPlan();
                            plan.loadJson(json);
                            return plan;
                        }).get();
            }catch (Exception e){
                new ExceptionDialog(appContext)
                        .setTitle("Error importing from link")
                        .setContextMessage("An error occurred while importing from link")
                        .setException(e)
                        .showAndWait();
            }
        }

        return null;
    }

    private static void exportToFile(Window parentWindow, AppContext appContext, PersistentProductionPlan plan)
    {
        FileChooser fc = new FileChooser();
        if (appContext.getPersistentData().getPreferences().getLastImportExportDirectory() != null && appContext.getPersistentData().getPreferences().getLastImportExportDirectory().isDirectory()){
            fc.setInitialDirectory(appContext.getPersistentData().getPreferences().getLastImportExportDirectory());
        }
        fc.setTitle("Select output file");
        fc.setInitialFileName(plan.getName() + ".json");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File f = fc.showSaveDialog(parentWindow);
        if (f != null){
            appContext.getPersistentData().getPreferences().setLastImportExportDirectory(f.getAbsoluteFile().getParentFile());
            try{
                try (JsonWriter w = JsonUtils.createWriter(f)){
                    w.writeObject(plan.toJson());
                }
                Notifications.create()
                        .position(Pos.TOP_CENTER)
                        .title("Production plan exported")
                        .text("The production plan was exported successfully")
                        .show();
            }catch (IOException e){
                new ExceptionDialog(appContext)
                        .setTitle("Error exporting plan")
                        .setContextMessage("An error occurred while exporting the plan to file")
                        .setException(e)
                        .showAndWait();
            }
        }
    }

    private static void exportToLink(AppContext appContext, PersistentProductionPlan plan)
    {
        try{
            String url = new TaskProgressDialog(appContext)
                    .setTitle("Exporting to link")
                    .setContentText("Busy uploading the production plan to the Internet")
                    .setCancellable(true)
                    .runTask(taskContext -> {

                        String content;
                        {
                            StringWriter sw = new StringWriter();
                            try (JsonWriter w = JsonUtils.createWriter(sw)){
                                w.writeObject(plan.toJson());
                            }
                            content = sw.toString();
                        }

                        String requestBody;
                        {
                            JsonObject json = Json.createObjectBuilder()
                                    .add("sections", Json.createArrayBuilder(
                                                    Collections.singleton(
                                                            Json.createObjectBuilder()
                                                                    .add("contents", content)
                                                                    .build()
                                                    )
                                            ).build()
                                    ).build();
                            StringWriter sw = new StringWriter();
                            try (JsonWriter w = JsonUtils.createWriter(sw)){
                                w.writeObject(json);
                            }
                            requestBody = sw.toString();
                        }

                        HttpRequest request = HttpRequest.newBuilder(new URI("https://api.paste.ee/v1/pastes"))
                                .header("Content-Type", "application/json")
                                .header("X-Auth-Token", "uwr65sq3CxBFuAerU5i5IAd0xw0RyPd98CwpodBSb")
                                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                                .build();

                        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() != 200 && response.statusCode() != 201){
                            throw new IOException("Returned status code: " + response.statusCode());
                        }

                        String s = response.body();

                        JsonObject json;
                        try (JsonReader r = Json.createReader(new StringReader(s))){
                            json = r.readObject();
                        }

                        if (!json.containsKey("id")){
                            throw new IOException("Response does not have id: " + s);
                        }

                        return "https://paste.ee/r/" + json.getString("id") + "/0";
                    })
                    .get();
            ClipboardContent content = new ClipboardContent();
            content.putString(url);
            Clipboard.getSystemClipboard().setContent(content);
            Notifications.create()
                    .position(Pos.TOP_CENTER)
                    .title("Link copied to clipboard")
                    .text("The link has been copied to your clipboard")
                    .show();
        }catch (Exception e){
            new ExceptionDialog(appContext)
                    .setTitle("Error exporting to link")
                    .setContextMessage("Error exporting to link")
                    .setException(e)
                    .showAndWait();
        }
    }

    private static PersistentProductionPlan importPlanFromFile(AppContext appContext, Window parentWindow)
    {
        FileChooser fc = new FileChooser();
        if (appContext.getPersistentData().getPreferences().getLastImportExportDirectory() != null && appContext.getPersistentData().getPreferences().getLastImportExportDirectory().isDirectory()){
            fc.setInitialDirectory(appContext.getPersistentData().getPreferences().getLastImportExportDirectory());
        }
        fc.setTitle("Select file to import");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File f = fc.showOpenDialog(parentWindow);
        if (f != null){
            appContext.getPersistentData().getPreferences().setLastImportExportDirectory(f.getAbsoluteFile().getParentFile());

            try{
                try (JsonReader r = Json.createReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(f)), StandardCharsets.UTF_8))){
                    PersistentProductionPlan plan = new PersistentProductionPlan();
                    plan.loadJson(r.readObject());
                    return plan;
                }
            }catch (IOException | RuntimeException e){
                new ExceptionDialog(appContext)
                        .setTitle("Error importing production plan")
                        .setContextMessage("An error occurred while importing the production plan")
                        .setException(e)
                        .showAndWait();
            }catch (UnsupportedVersionException e){
                new ExceptionDialog(appContext)
                        .setTitle("Requires newer version of Satisplanory")
                        .setContextMessage("Unsupported production plan format, please upgrade Satisplanory")
                        .setDetailsMessage("""
                                The production plan you are trying to import was created with a newer version of Satisplanory.

                                This version of Satisplanory does not support the newer file format.

                                Please upgrade Satisplanory to allow importing this plan. See Help -> About for links.""")
                        .setException(e)
                        .showAndWait();
            }
        }

        return null;
    }
}
