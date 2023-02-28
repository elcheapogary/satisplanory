/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.satisdata;

import io.github.elcheapogary.satisplanory.satisfactory.SatisfactoryInstallation;
import io.github.elcheapogary.satisplanory.satisfactory.SatisfactoryInstallationDiscoveryMechanism;
import io.github.elcheapogary.satisplanory.ui.jfx.context.AppContext;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.ExceptionDialog;
import io.github.elcheapogary.satisplanory.ui.jfx.style.Style;
import java.io.File;
import java.util.Collection;
import java.util.Optional;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

public class SatisfactoryInstallationSelectorDialog
{
    private SatisfactoryInstallationSelectorDialog()
    {
    }

    private static SatisfactoryInstallation getCurrentlySelectedInstallation(AppContext appContext, Collection<? extends SatisfactoryInstallation> installations)
    {
        String path = appContext.getPersistentData().getSatisfactoryPath();

        if (path == null){
            return null;
        }

        for (SatisfactoryInstallation installation : installations){
            if (installation.getPath().getAbsolutePath().equals(path)){
                return installation;
            }
        }

        return null;
    }

    public static Optional<SatisfactoryInstallation> show(AppContext appContext, Collection<SatisfactoryInstallation> installations)
    {
        Dialog<SatisfactoryInstallation> dialog = new Dialog<>();

        dialog.setTitle("Select Satisfactory installation");
        dialog.getDialogPane().getStylesheets().addAll(Style.getStyleSheets(appContext));

        dialog.setGraphic(null);
        dialog.setHeaderText("Please select a Satisfactory installation");

        ObservableList<SatisfactoryInstallation> list = FXCollections.observableArrayList(installations);

        ListView<SatisfactoryInstallation> listView = new ListView<>(list);

        listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        listView.setCellFactory(param -> new ListCell<>()
        {
            @Override
            protected void updateItem(SatisfactoryInstallation item, boolean empty)
            {
                super.updateItem(item, empty);

                if (empty){
                    setText(null);
                    setGraphic(null);
                }else{
                    VBox vbox = new VBox();

                    String heading;

                    if (item.getBranch() == null){
                        heading = item.getGameStore().getDisplayName();
                    }else{
                        heading = item.getGameStore().getDisplayName() + " / " + item.getBranch().getDisplayName();
                    }

                    Label headingLabel = new Label(heading);
                    headingLabel.setStyle("-fx-font-weight: bold;");

                    vbox.getChildren().add(headingLabel);
                    vbox.getChildren().add(new Label(item.getPath().getAbsolutePath()));

                    setGraphic(vbox);
                    setText(null);
                }
            }
        });

        SatisfactoryInstallation currentInstallation = getCurrentlySelectedInstallation(appContext, installations);

        if (currentInstallation != null){
            listView.getSelectionModel().select(currentInstallation);
        }else if (appContext.getPersistentData().getSatisfactoryPath() != null && SatisfactoryInstallation.isValidSatisfactoryInstallation(new File(appContext.getPersistentData().getSatisfactoryPath()))){
            currentInstallation = new SatisfactoryInstallation(SatisfactoryInstallationDiscoveryMechanism.Manual, null, new File(appContext.getPersistentData().getSatisfactoryPath()));
            list.add(0, currentInstallation);
            listView.getSelectionModel().select(0);
        }

        VBox vbox = new VBox();
        vbox.setPrefWidth(600);
        vbox.setPadding(new Insets(10, 10, 0, 10));
        vbox.setSpacing(10);
        dialog.getDialogPane().setContent(vbox);

        vbox.getChildren().add(listView);

        Button browseButton = new Button("Browse");
        vbox.getChildren().add(browseButton);

        browseButton.setMaxWidth(Double.MAX_VALUE);

        browseButton.onActionProperty().set(event -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Satisfactory Directory");

            while (true){
                File f = dc.showDialog(browseButton.getScene().getWindow());

                if (f == null){
                    break;
                }else if (!SatisfactoryInstallation.isValidSatisfactoryInstallation(f)){
                    new ExceptionDialog(appContext)
                            .setTitle("Invalid Satisfactory Installation Selected")
                            .setContextMessage("The directory you selected is not recognised as a valid Satisfactory installation.")
                            .showAndWait();
                }else{
                    SatisfactoryInstallation browsedInstallation = null;

                    for (SatisfactoryInstallation installation : installations){
                        if (installation.getPath().equals(f)){
                            browsedInstallation = installation;
                            listView.getSelectionModel().select(browsedInstallation);
                            break;
                        }
                    }

                    if (browsedInstallation == null){
                        browsedInstallation = new SatisfactoryInstallation(SatisfactoryInstallationDiscoveryMechanism.Manual, null, f);
                        list.add(0, browsedInstallation);
                        listView.getSelectionModel().select(browsedInstallation);
                    }

                    break;
                }
            }
        });

        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(Bindings.createBooleanBinding(() -> listView.getSelectionModel().selectedItemProperty().get() == null, listView.getSelectionModel().selectedItemProperty()));

        dialog.setResultConverter(param -> {
            if (param == ButtonType.OK){
                return listView.getSelectionModel().getSelectedItem();
            }else{
                return null;
            }
        });

        return dialog.showAndWait();
    }
}
