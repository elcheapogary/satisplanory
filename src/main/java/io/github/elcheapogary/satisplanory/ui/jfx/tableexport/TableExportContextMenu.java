/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.tableexport;

import io.github.elcheapogary.satisplanory.ui.jfx.context.AppContext;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.ExceptionDialog;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;

public class TableExportContextMenu
{
    private TableExportContextMenu()
    {
    }

    public static ContextMenu forTable(AppContext appContext, TableView<?> tableView)
    {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setAutoHide(true);

        MenuItem menuItem = new MenuItem("Export CSV");
        contextMenu.getItems().add(menuItem);
        menuItem.onActionProperty().set(event -> {
            FileChooser fc = new FileChooser();
            if (appContext.getPersistentData().getPreferences().getLastImportExportDirectory() != null && appContext.getPersistentData().getPreferences().getLastImportExportDirectory().isDirectory()){
                fc.setInitialDirectory(appContext.getPersistentData().getPreferences().getLastImportExportDirectory());
            }
            fc.setTitle("Select output file");
            fc.setInitialFileName("data.csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(tableView.getScene().getWindow());
            if (f != null){
                appContext.getPersistentData().getPreferences().setLastImportExportDirectory(f.getAbsoluteFile().getParentFile());
                try {
                    try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
                        CsvExport.exportToCsv(w, tableView);
                    }
                }catch (IOException e){
                    new ExceptionDialog(appContext)
                            .setTitle("Error exporting data")
                            .setContextMessage("An error occurred while exporting the data to file")
                            .setException(e)
                            .showAndWait();
                }
            }
        });

        return contextMenu;
    }
}
