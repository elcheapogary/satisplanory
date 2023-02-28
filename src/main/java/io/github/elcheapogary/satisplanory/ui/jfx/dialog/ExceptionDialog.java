/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.dialog;

import io.github.elcheapogary.satisplanory.ui.jfx.context.AppContext;
import io.github.elcheapogary.satisplanory.ui.jfx.style.Style;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;

public class ExceptionDialog
{
    private final List<? extends String> styleSheets;
    private String title;
    private String contextMessage;
    private String detailsMessage;
    private Exception exception;

    public ExceptionDialog(AppContext appContext)
    {
        this.styleSheets = Style.getStyleSheets(appContext);
    }

    public ExceptionDialog setContextMessage(String contextMessage)
    {
        this.contextMessage = contextMessage;
        return this;
    }

    public ExceptionDialog setDetailsMessage(String detailsMessage)
    {
        this.detailsMessage = detailsMessage;
        return this;
    }

    public ExceptionDialog setException(Exception exception)
    {
        this.exception = exception;
        return this;
    }

    public ExceptionDialog setTitle(String title)
    {
        this.title = title;
        return this;
    }

    public void showAndWait()
    {
        Dialog<Void> dialog = new Dialog<>();
        dialog.getDialogPane().getStylesheets().addAll(styleSheets);
        if (title != null){
            dialog.setTitle(title);
        }else{
            dialog.setTitle("Error");
        }

        if (contextMessage != null){
            dialog.setContentText(contextMessage);
        }

        if (exception != null || detailsMessage != null){
            TabPane tabPane = new TabPane();

            if (detailsMessage != null){
                TextArea textArea = new TextArea();
                textArea.setEditable(false);
                textArea.setText(detailsMessage);
                Tab tab = new Tab("Details", textArea);
                tab.setClosable(false);
                tabPane.getTabs().add(tab);
            }

            if (exception != null){
                TextArea textArea = new TextArea();
                textArea.setEditable(false);
                StringWriter sw = new StringWriter();
                exception.printStackTrace(new PrintWriter(sw));
                textArea.setText(sw.toString());
                Tab tab = new Tab("Exception", textArea);
                tab.setClosable(false);
                tabPane.getTabs().add(tab);
            }

            dialog.getDialogPane().setExpandableContent(tabPane);
        }

        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        dialog.showAndWait();
    }
}
