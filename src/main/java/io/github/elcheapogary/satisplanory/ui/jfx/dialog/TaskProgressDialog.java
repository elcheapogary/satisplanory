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
import java.util.List;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class TaskProgressDialog
{
    private final List<? extends String> styleSheets;
    private String title;
    private String contentText;
    private boolean cancellable;

    public TaskProgressDialog(AppContext appContext)
    {
        this.styleSheets = Style.getStyleSheets(appContext);
    }

    public <T> TaskResult<T> runTask(Task<? extends T> task)
    {
        Dialog<TaskResult<T>> dialog = new Dialog<>();
        dialog.getDialogPane().getStylesheets().addAll(styleSheets);
        dialog.setTitle(title);
        VBox content = new VBox();
        content.setPrefWidth(360);
        content.setSpacing(10);
        Label contentLabel = new Label(contentText);
        contentLabel.setMaxWidth(Double.MAX_VALUE);
        content.getChildren().add(contentLabel);
        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(-1);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        content.getChildren().add(progressBar);
        dialog.getDialogPane().setContent(content);
        TaskContextImpl context = new TaskContextImpl(progressBar);
        Thread thread = new Thread(() -> {
            try {
                T result = task.perform(context);
                Platform.runLater(() -> {
                    dialog.setResult(TaskResult.forResult(result));
                    dialog.close();
                });
            }catch (Exception e){
                Platform.runLater(() -> {
                    dialog.setResult(TaskResult.forException(e));
                    dialog.close();
                });
            }
        });
        thread.start();
        if (cancellable){
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            dialog.setResultConverter(param -> {
                if (param == ButtonType.CANCEL){
                    thread.interrupt();
                    return TaskResult.forException(new TaskCancelledException());
                }
                throw new IllegalArgumentException("Unsupported button type: " + param.getText());
            });
        }
        return dialog.showAndWait().orElse(null);
    }

    public TaskProgressDialog setCancellable(boolean cancellable)
    {
        this.cancellable = cancellable;
        return this;
    }

    public TaskProgressDialog setContentText(String contentText)
    {
        this.contentText = contentText;
        return this;
    }

    public TaskProgressDialog setTitle(String title)
    {
        this.title = title;
        return this;
    }

    public interface TaskContext
    {
        void setProgress(double progress);
    }

    @FunctionalInterface
    public interface Task<T>
    {
        T perform(TaskContext taskContext)
                throws Exception;
    }

    public interface TaskResult<T>
    {
        static <T> TaskResult<T> forException(Exception e)
        {
            return new TaskResult<T>()
            {
                @Override
                public T get()
                        throws Exception
                {
                    throw e;
                }

                @Override
                public boolean taskCancelled()
                {
                    return e instanceof TaskCancelledException;
                }

                @Override
                public boolean taskEndedSuccessfully()
                {
                    return false;
                }

                @Override
                public boolean taskFailed()
                {
                    return true;
                }
            };
        }

        static <T> TaskResult<T> forResult(T t)
        {
            return new TaskResult<T>()
            {
                @Override
                public T get()
                        throws Exception
                {
                    return t;
                }

                @Override
                public boolean taskCancelled()
                {
                    return false;
                }

                @Override
                public boolean taskEndedSuccessfully()
                {
                    return true;
                }

                @Override
                public boolean taskFailed()
                {
                    return false;
                }
            };
        }

        T get()
                throws Exception;

        boolean taskCancelled();

        boolean taskEndedSuccessfully();

        boolean taskFailed();
    }

    private static class TaskContextImpl
            implements TaskContext
    {
        private final ProgressBar progressBar;

        public TaskContextImpl(ProgressBar progressBar)
        {
            this.progressBar = progressBar;
        }

        @Override
        public void setProgress(double progress)
        {
            Platform.runLater(() -> progressBar.setProgress(progress));
        }
    }

    public static class TaskCancelledException
            extends Exception
    {
    }
}
