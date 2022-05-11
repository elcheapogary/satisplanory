/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.prodplan;

import io.github.elcheapogary.satisplanory.model.GameData;
import io.github.elcheapogary.satisplanory.model.Item;
import io.github.elcheapogary.satisplanory.model.Recipe;
import io.github.elcheapogary.satisplanory.prodplan.MultiPlan;
import io.github.elcheapogary.satisplanory.prodplan.ProdPlanUtils;
import io.github.elcheapogary.satisplanory.prodplan.ProductionPlanNotFeatisbleException;
import io.github.elcheapogary.satisplanory.prodplan.ProductionPlanner;
import io.github.elcheapogary.satisplanory.ui.jfx.component.ItemComponents;
import io.github.elcheapogary.satisplanory.ui.jfx.component.ObservableRunnable;
import io.github.elcheapogary.satisplanory.ui.jfx.context.AppContext;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.ExceptionDialog;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.TaskProgressDialog;
import io.github.elcheapogary.satisplanory.ui.jfx.style.Style;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import io.github.elcheapogary.satisplanory.util.ResourceUtils;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.scene.web.WebView;

public class ProdPlanTab
{
    private ProdPlanTab()
    {
    }

    private static void addInputTab(AppContext appContext, GameData gameData, TabPane tabPane)
    {
        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.TOP_CENTER);
        hbox.setPadding(new Insets(10));

        Tab inputTab = new Tab("Input");
        inputTab.setClosable(false);
        inputTab.setContent(hbox);
        tabPane.getTabs().add(inputTab);

        ProdPlanData prodPlanData = new ProdPlanData();
        prodPlanData.getEnabledRecipes().addAll(gameData.getRecipes());

        ObservableList<Item> allItemsObservableList = FXCollections.observableList(new ArrayList<>(gameData.getItems()));

        Region n = createRecipesPane(gameData.getRecipes(), prodPlanData.getEnabledRecipes(), allItemsObservableList);
        HBox.setHgrow(n, Priority.NEVER);
        hbox.getChildren().add(n);

        VBox vbox = new VBox(10);
        vbox.setPrefWidth(0);
        HBox.setHgrow(vbox, Priority.ALWAYS);
        hbox.getChildren().add(vbox);

        Accordion accordion = new Accordion();
        VBox.setVgrow(accordion, Priority.ALWAYS);
        vbox.getChildren().add(accordion);

        ObservableList<ProdPlanData.InputItem> inputItems = FXCollections.observableList(prodPlanData.getInputItems());

        accordion.getPanes().add(InputItemsPane.createInputItemsPane(inputItems, allItemsObservableList, gameData));

        accordion.setExpandedPane(accordion.getPanes().get(0));

        ObservableValue<List<ProdPlanData.OutputItem>> outputItems;
        {
            ObservableList<ProdPlanData.OutputItem> tmp = FXCollections.observableList(prodPlanData.getOutputItems());

            ObservableRunnable r = new ObservableRunnable();

            accordion.getPanes().add(OutputItemsPane.createOutputRequirementsPane(tmp, allItemsObservableList, r));

            outputItems = Bindings.createObjectBinding(() -> tmp, tmp, r);
        }

        ObservableList<SettingsPane.OptimizationTarget> optimizationTargets = FXCollections.observableList(new ArrayList<>());
        optimizationTargets.add(SettingsPane.OptimizationTarget.BALANCE);
        optimizationTargets.add(SettingsPane.OptimizationTarget.MAXIMIZE_OUTPUT_ITEMS);

        accordion.getPanes().add(SettingsPane.createSettingsPane(prodPlanData.settings, outputItems, optimizationTargets));

        accordion.getPanes().add(createHelpPane(appContext));

        Button button = new Button("Calculate");
        button.setMaxWidth(Double.MAX_VALUE);
        vbox.getChildren().add(button);

        Tab resultTab = new Tab("Results");

        button.onActionProperty().set(event -> {
            ProductionPlanner.Builder b = new ProductionPlanner.Builder();

            b.addRecipes(prodPlanData.getEnabledRecipes());

            {
                BigFraction weight = BigFraction.ONE;
                for (SettingsPane.OptimizationTarget target : optimizationTargets){
                    target.setWeight(b, weight);
                    weight = weight.movePointLeft(6);
                }

                for (SettingsPane.OptimizationTarget target : SettingsPane.OptimizationTarget.values()){
                    if (!optimizationTargets.contains(target)){
                        target.setWeight(b, BigFraction.ZERO);
                    }
                }
            }

            for (ProdPlanData.InputItem inputItem : prodPlanData.getInputItems()){
                if (inputItem.getAmount().signum() > 0){
                    b.addInputItem(inputItem.getItem(), inputItem.getItem().fromDisplayAmount(inputItem.getAmount()));
                }
            }

            boolean hasOutputRequirement = false;
            for (ProdPlanData.OutputItem outputItem : prodPlanData.getOutputItems()){
                BigDecimal min = outputItem.getMin();

                if (min.signum() > 0){
                    hasOutputRequirement = true;
                    b.requireOutputItemsPerMinute(outputItem.getItem(), outputItem.getItem().fromDisplayAmount(min));
                }

                BigDecimal weight = outputItem.getWeight();

                if (weight.signum() > 0){
                    hasOutputRequirement = true;
                    b.maximizeOutputItem(outputItem.getItem(), weight);
                }
            }

            if (!hasOutputRequirement){
                new ExceptionDialog(appContext)
                        .setContextMessage("No output requirements provided. Please provide output requirements.")
                        .setTitle("Error: No output requirements provided")
                        .setDetailsMessage("There needs to be at least one item listed under Output Requirements that has a value\ngreater than zero for either Min or Weight.")
                        .showAndWait();
                return;
            }

            MultiPlan plan;

            try {
                plan = new TaskProgressDialog(appContext)
                        .setTitle("Calculating")
                        .setContentText("Calculating production plan")
                        .setCancellable(true)
                        .runTask(taskContext -> ProdPlanUtils.getMultiPlan(gameData, b.build()))
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
                        .setDetailsMessage("This should not happen. It's probably a bug. We would like to hear about this.\n"
                                + "Please consider sending the developers (of Satisplanory, not Satisfactory) the EXACT \n"
                                + "input settings that caused this error.")
                        .showAndWait();
                return;
            }

            resultTab.setClosable(false);
            resultTab.setContent(ResultsPane.createResultsTabContent(plan));
            if (!tabPane.getTabs().contains(resultTab)){
                tabPane.getTabs().add(resultTab);
            }
            tabPane.getSelectionModel().select(resultTab);
        });
    }

    public static Tab create(AppContext appContext, GameData gameData)
    {
        TabPane subTabPane = new TabPane();
        addInputTab(appContext, gameData, subTabPane);
        Tab retv = new Tab("Production Plans", subTabPane);
        retv.setClosable(false);
        return retv;
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
            webView.getEngine().loadContent(ResourceUtils.getResourceAsString(ProdPlanTab.class, "help.html"), "text/html");
        }catch (IOException e){
            e.printStackTrace(System.err);
        }

        return titledPane;
    }

    private static TitledPane createRecipeSelectionList(String title, Predicate<Recipe> predicate, Collection<? extends Recipe> recipes, Set<Recipe> enabledRecipes, Function<Recipe, ObservableValue<Boolean>> searchFunction)
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

    private static TitledPane createRecipesPane(Collection<? extends Recipe> recipes, Set<Recipe> enabledRecipes, ObservableList<Item> allItems)
    {
        TitledPane titledPane = new TitledPane();
        titledPane.setCollapsible(false);
        titledPane.setText("Recipes");

        VBox vBox = new VBox(10);
        vBox.setPadding(new Insets(10, 25, 10, 10));

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

            Node defaultRecipes = createRecipeSelectionList("Default Recipes", recipe -> !recipe.isAlternateRecipe(), recipes, enabledRecipes, displayValueFactory);
            HBox.setHgrow(defaultRecipes, Priority.ALWAYS);
            hBox.getChildren().add(defaultRecipes);

            Node alternateRecipes = createRecipeSelectionList("Alternate Recipes", Recipe::isAlternateRecipe, recipes, enabledRecipes, displayValueFactory);
            HBox.setHgrow(alternateRecipes, Priority.ALWAYS);
            hBox.getChildren().add(alternateRecipes);
        }

        ScrollPane scrollPane = new ScrollPane(vBox);
        titledPane.setContent(scrollPane);

        return titledPane;
    }


}
