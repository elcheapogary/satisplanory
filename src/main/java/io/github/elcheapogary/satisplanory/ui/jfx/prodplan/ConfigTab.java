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

import io.github.elcheapogary.satisplanory.model.Item;
import io.github.elcheapogary.satisplanory.model.Recipe;
import io.github.elcheapogary.satisplanory.prodplan.MultiPlan;
import io.github.elcheapogary.satisplanory.prodplan.OptimizationTarget;
import io.github.elcheapogary.satisplanory.prodplan.ProdPlanUtils;
import io.github.elcheapogary.satisplanory.prodplan.ProductionPlan;
import io.github.elcheapogary.satisplanory.prodplan.ProductionPlanNotFeatisbleException;
import io.github.elcheapogary.satisplanory.prodplan.ProductionPlanner;
import io.github.elcheapogary.satisplanory.ui.jfx.component.ItemComponents;
import io.github.elcheapogary.satisplanory.ui.jfx.context.AppContext;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.ExceptionDialog;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.TaskProgressDialog;
import io.github.elcheapogary.satisplanory.ui.jfx.style.Style;
import io.github.elcheapogary.satisplanory.util.ResourceUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.scene.web.WebView;

class ConfigTab
{
    private ConfigTab()
    {
    }

    public static Tab create(AppContext appContext, ProdPlanModel model)
    {
        Tab tab = new Tab("Configuration");
        tab.setClosable(false);
        tab.setContent(createBody(appContext, model));
        return tab;
    }

    public static Node createBody(AppContext appContext, ProdPlanModel model)
    {
        HBox body = new HBox(10);
        body.setAlignment(Pos.TOP_CENTER);
        body.setPadding(new Insets(10));

        ObservableList<Item> allItemsObservableList = FXCollections.observableList(new ArrayList<>(appContext.getGameData().getItems()));

        Region n = createRecipesPane(appContext.getGameData().getRecipes(), model, allItemsObservableList);
        HBox.setHgrow(n, Priority.NEVER);
        body.getChildren().add(n);

        VBox vbox = new VBox(10);
        vbox.setPrefWidth(0);
        HBox.setHgrow(vbox, Priority.ALWAYS);
        body.getChildren().add(vbox);

        Accordion accordion = new Accordion();
        VBox.setVgrow(accordion, Priority.ALWAYS);
        vbox.getChildren().add(accordion);

        accordion.getPanes().add(InputItemsPane.createInputItemsPane(model.getInputItems(), allItemsObservableList, appContext.getGameData()));

        accordion.setExpandedPane(accordion.getPanes().get(0));

        accordion.getPanes().add(OutputItemsPane.createOutputRequirementsPane(FXCollections.observableList(model.getOutputItems()), allItemsObservableList, () -> {
        }));

        accordion.getPanes().add(SettingsPane.createSettingsPane(model.getSettings(), model.getOutputItems()));

        accordion.getPanes().add(createHelpPane(appContext));

        Button button = new Button("Calculate");
        button.setMaxWidth(Double.MAX_VALUE);
        vbox.getChildren().add(button);

        button.onActionProperty().set(event -> {
            ProductionPlanner.Builder b = new ProductionPlanner.Builder();

            b.addRecipes(model.getEnabledRecipes());

            for (OptimizationTargetModel otm : model.getSettings().getOptimizationTargets()){
                if (otm.getOptimizationTarget() == OptimizationTarget.MAX_SINK_POINTS){
                    b.setFilterRecipesByOutputItems(false);
                }
                b.addOptimizationTarget(otm.getOptimizationTarget());
            }

            for (ProdPlanModel.InputItem inputItem : model.getInputItems()){
                if (inputItem.getAmount().getValue().signum() > 0){
                    b.addInputItem(inputItem.getItem(), inputItem.getItem().fromDisplayAmount(inputItem.getAmount().getValue()));
                }
            }

            for (ProdPlanModel.OutputItem outputItem : model.getOutputItems()){
                b.addOutputItem(
                        outputItem.getItem(),
                        outputItem.getItem().fromDisplayAmount(outputItem.getMin().getValue()),
                        outputItem.getWeight().getValue()
                );
            }

            MultiPlan plan;

            try {
                plan = new TaskProgressDialog(appContext)
                        .setTitle("Calculating")
                        .setContentText("Calculating production plan")
                        .setCancellable(true)
                        .runTask(taskContext -> ProdPlanUtils.getMultiPlan(appContext.getGameData(), b.build()))
                        .get();
            }catch (TaskProgressDialog.TaskCancelledException e){
                return;
            }catch (ProductionPlanNotFeatisbleException e){
                new ExceptionDialog(appContext)
                        .setTitle("No feasible plan")
                        .setContextMessage("No production plan possible with the provided input")
                        .showAndWait();
                return;
            }catch (Exception e){
                new ExceptionDialog(appContext)
                        .setTitle("Error calculating production plan")
                        .setContextMessage("An unexpected error occurred while calculating the production plan")
                        .setDetailsMessage("""
                                This should not happen. It's probably a bug. We would like to hear about this.
                                Please consider sending the developers (of Satisplanory, not Satisfactory) the EXACT\s
                                input settings that caused this error."""
                        )
                        .showAndWait();
                return;
            }

            if (plan.isUnmodifiedPlanFeasible()){
                ProductionPlan p = plan.getUnmodifiedPlan();
                if (p.getOutputItems().isEmpty()){
                    new ExceptionDialog(appContext)
                            .setContextMessage("This configuration generated an empty plan - it does nothing. Try adding minimum output.")
                            .setTitle("Empty plan")
                            .setDetailsMessage("Having at least one output item with a minimum number of items per minute will help.")
                            .showAndWait();
                }else{
                    model.setPlan(p);
                    model.setMultiPlan(null);
                }
            }else{
                model.setPlan(null);
                model.setMultiPlan(plan);
            }
        });

        return body;
    }

    private static TitledPane createHelpPane(AppContext appContext)
    {
        TitledPane titledPane = new TitledPane();
        titledPane.setText("Help");
        titledPane.setCollapsible(true);

        WebView webView = new WebView();
        Style.configureWebView(appContext, webView);
        webView.setMaxWidth(Double.MAX_VALUE);
        webView.setMaxHeight(Double.MAX_VALUE);
        titledPane.setContent(webView);

        try {
            webView.getEngine().loadContent(ResourceUtils.getResourceAsString(ConfigTab.class, "help.html"), "text/html");
        }catch (IOException e){
            e.printStackTrace(System.err);
        }

        return titledPane;
    }

    private static TitledPane createRecipeSelectionList(String title, Predicate<Recipe> predicate, Collection<? extends Recipe> recipes, ObservableSet<Recipe> enabledRecipes, Function<Recipe, ObservableValue<Boolean>> searchFunction)
    {
        TitledPane t = new TitledPane();
        t.setText(title);
        t.setCollapsible(false);

        List<CheckBox> checkboxes = new LinkedList<>();

        VBox vbox = new VBox(2);
        t.setContent(vbox);

        {
            HBox hBox = new HBox(10);
            VBox.setMargin(hBox, new Insets(0, 0, 10, 0));
            vbox.getChildren().add(hBox);

            {
                Button button = new Button("Select All");
                button.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(button, Priority.ALWAYS);
                hBox.getChildren().add(button);
                button.onActionProperty().set(event -> {
                    for (CheckBox cb : checkboxes){
                        if (cb.isVisible()){
                            cb.setSelected(true);
                        }
                    }
                });
            }

            {
                Button button = new Button("Select None");
                button.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(button, Priority.ALWAYS);
                hBox.getChildren().add(button);
                button.onActionProperty().set(event -> {
                    for (CheckBox cb : checkboxes){
                        if (cb.isVisible()){
                            cb.setSelected(false);
                        }
                    }
                });
            }
        }

        for (Recipe recipe : recipes){
            if (predicate.test(recipe)){
                String recipeName = recipe.getName();
                if (recipeName.startsWith("Alternate: ")){
                    recipeName = recipeName.substring("Alternate: ".length());
                }
                CheckBox cb = new CheckBox(recipeName);
                ObservableValue<Boolean> showValue = searchFunction.apply(recipe);
                cb.visibleProperty().bind(showValue);
                cb.managedProperty().bind(showValue);
                checkboxes.add(cb);
                if (enabledRecipes.contains(recipe)){
                    cb.setSelected(true);
                }
                enabledRecipes.addListener((SetChangeListener<Recipe>)change -> {
                    if (change.wasRemoved() && change.getElementRemoved() == recipe){
                        cb.setSelected(false);
                    }else if (change.wasAdded() && change.getElementAdded() == recipe){
                        cb.setSelected(true);
                    }
                });
                vbox.getChildren().add(cb);
                checkboxes.add(cb);
                cb.selectedProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue){
                        enabledRecipes.add(recipe);
                    }else{
                        enabledRecipes.remove(recipe);
                    }
                });
            }
        }

        return t;
    }

    private static TitledPane createRecipesPane(Collection<? extends Recipe> recipes, ProdPlanModel model, ObservableList<Item> allItems)
    {
        TitledPane titledPane = new TitledPane();
        titledPane.setCollapsible(false);
        titledPane.setText("Recipes");

        VBox vBox = new VBox(10);
        vBox.setPadding(new Insets(10, 25, 10, 10));

        {
            Button useCurrentRecipesButton = new Button("Current plan recipes only");
            vBox.getChildren().add(useCurrentRecipesButton);

            useCurrentRecipesButton.setMaxWidth(Double.MAX_VALUE);

            useCurrentRecipesButton.disableProperty().bind(Bindings.createBooleanBinding(() -> model.planProperty().getValue() == null, model.planProperty()));

            useCurrentRecipesButton.onActionProperty().set(event -> {
                model.getEnabledRecipes().clear();
                model.getEnabledRecipes().addAll(model.getPlan().getRecipes());
            });
        }

        ObservableValue<String> searchStringValue;
        ObservableValue<Item> producesItem;

        {
            GridPane searchGridPane = new GridPane();
            vBox.getChildren().add(searchGridPane);

            {
                Label label = new Label("Search:");
                searchGridPane.add(label, 0, 0);
            }

            TextField searchField = new TextField();
            searchField.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(searchField, Priority.ALWAYS);
            GridPane.setMargin(searchField, new Insets(0, 0, 0, 10));
            searchGridPane.add(searchField, 1, 0);
            searchStringValue = searchField.textProperty();

            {
                Label label = new Label("Produces:");
                GridPane.setMargin(label, new Insets(10, 0, 0, 0));
                searchGridPane.add(label, 0, 1);
            }

            ComboBox<Item> producesComboBox = ItemComponents.createItemComboBox(allItems);
            producesComboBox.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(producesComboBox, Priority.ALWAYS);
            GridPane.setMargin(producesComboBox, new Insets(10, 0, 0, 10));
            searchGridPane.add(producesComboBox, 1, 1);
            producesItem = producesComboBox.valueProperty();

            Button clearButton = new Button("Clear\nSearch");
            clearButton.setTextAlignment(TextAlignment.CENTER);
            clearButton.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(clearButton, Priority.ALWAYS);
            GridPane.setRowSpan(clearButton, 2);
            GridPane.setHalignment(clearButton, HPos.RIGHT);
            GridPane.setMargin(clearButton, new Insets(0, 0, 0, 10));
            searchGridPane.add(clearButton, 2, 0);
            clearButton.onActionProperty().set(event -> {
                searchField.setText("");
                producesComboBox.setValue(null);
            });
        }

        Function<Recipe, ObservableValue<Boolean>> displayValueFactory = recipe -> Bindings.createBooleanBinding(() -> {
            String searchString = searchStringValue.getValue().trim().toLowerCase(Locale.ENGLISH);
            if (!searchString.isEmpty()){
                return recipe.getName().toLowerCase(Locale.ENGLISH).contains(searchString);
            }
            if (producesItem.getValue() != null){
                return recipe.producesItem(producesItem.getValue());
            }
            return true;
        }, searchStringValue, producesItem);

        {
            HBox hBox = new HBox(10);
            vBox.getChildren().add(hBox);
            hBox.setFillHeight(true);

            Node defaultRecipes = createRecipeSelectionList("Default Recipes", recipe -> !recipe.isAlternateRecipe(), recipes, model.getEnabledRecipes(), displayValueFactory);
            HBox.setHgrow(defaultRecipes, Priority.ALWAYS);
            hBox.getChildren().add(defaultRecipes);

            Node alternateRecipes = createRecipeSelectionList("Alternate Recipes", Recipe::isAlternateRecipe, recipes, model.getEnabledRecipes(), displayValueFactory);
            HBox.setHgrow(alternateRecipes, Priority.ALWAYS);
            hBox.getChildren().add(alternateRecipes);
        }

        ScrollPane scrollPane = new ScrollPane(vBox);
        titledPane.setContent(scrollPane);

        return titledPane;
    }


}
