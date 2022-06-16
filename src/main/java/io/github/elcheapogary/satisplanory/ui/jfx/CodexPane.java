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

import io.github.elcheapogary.satisplanory.model.GameData;
import io.github.elcheapogary.satisplanory.model.Item;
import io.github.elcheapogary.satisplanory.model.Recipe;
import io.github.elcheapogary.satisplanory.util.BigDecimalUtils;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class CodexPane
{
    private CodexPane()
    {
    }

    public static Tab createTab(GameData gameData)
    {
        Tab tab = new Tab("Codex");
        tab.setClosable(false);
        tab.setContent(createTabContent(gameData));
        return tab;
    }

    private static Node createTabContent(GameData gameData)
    {
        HBox hbox = new HBox(10);
        hbox.setPadding(new Insets(10));
        hbox.setFillHeight(true);

        VBox itemsListVbox = new VBox(10);
        HBox.setHgrow(itemsListVbox, Priority.NEVER);
        hbox.getChildren().add(itemsListVbox);

        itemsListVbox.setFillWidth(true);
        itemsListVbox.setPrefWidth(250);

        TextField searchTextField = new TextField();
        itemsListVbox.getChildren().add(searchTextField);

        searchTextField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE){
                searchTextField.setText("");
                event.consume();
            }
        });

        ListView<Item> itemsList = new ListView<>();
        VBox.setVgrow(itemsList, Priority.ALWAYS);
        itemsListVbox.getChildren().add(itemsList);

        itemsList.setCellFactory(TextFieldListCell.forListView(new StringConverter<>()
        {
            @Override
            public Item fromString(String string)
            {
                return gameData.getItemByName(string).orElse(null);
            }

            @Override
            public String toString(Item object)
            {
                return object.getName();
            }
        }));

        FilteredList<Item> list = new FilteredList<>(FXCollections.observableArrayList(gameData.getItems()));
        itemsList.setItems(list);

        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null){
                list.setPredicate(item -> true);
            }else{
                newValue = newValue.trim();

                if (newValue.isEmpty()){
                    list.setPredicate(item -> true);
                }else{
                    final String searchString = newValue;
                    list.setPredicate(item -> item.getName().toLowerCase(Locale.ENGLISH).contains(searchString.toLowerCase(Locale.ENGLISH)));
                }
            }
        });

        BorderPane displayContainer = new BorderPane();
        HBox.setHgrow(displayContainer, Priority.ALWAYS);
        hbox.getChildren().add(displayContainer);

        itemsList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null){
                displayContainer.setCenter(null);
            }else{
                displayContainer.setCenter(createDisplayComponent(newValue, gameData));
            }
        });

        return hbox;
    }

    private static Node createDisplayComponent(Item item, GameData gameData)
    {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.setFillWidth(true);

        Label mainHeading = new Label(item.getName());
        mainHeading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        VBox.setVgrow(mainHeading, Priority.NEVER);
        vbox.getChildren().add(mainHeading);

        Label description = new Label(item.getDescription());
        description.setWrapText(true);
        VBox.setVgrow(description, Priority.NEVER);
        vbox.getChildren().add(description);

        vbox.getChildren().add(createCraftingPane(item, gameData));
        vbox.getChildren().add(createUsesPane(item, gameData));

        Region grower = new Region();
        grower.setPrefHeight(0);
        VBox.setVgrow(grower, Priority.ALWAYS);
        vbox.getChildren().add(grower);

        ScrollPane sp = new ScrollPane(vbox);
        sp.setFitToWidth(true);
        return sp;
    }

    private static Node createCraftingPane(Item item, GameData gameData)
    {
        List<Recipe> defaultRecipes = new LinkedList<>();
        List<Recipe> alternateRecipes = new LinkedList<>();

        for (Recipe r : gameData.getRecipes()){
            if (r.producesItem(item)){
                if (r.isAlternateRecipe()){
                    alternateRecipes.add(r);
                }else{
                    defaultRecipes.add(r);
                }
            }
        }

        if (defaultRecipes.isEmpty() && alternateRecipes.isEmpty()){
            Region r = new Region();
            r.setMaxWidth(0);
            r.setMaxHeight(0);
            r.setPrefWidth(0);
            r.setPrefHeight(0);
            return r;
        }

        defaultRecipes = new ArrayList<>(defaultRecipes);
        alternateRecipes = new ArrayList<>(alternateRecipes);

        defaultRecipes.sort(Comparator.comparing(Recipe::getName));
        alternateRecipes.sort(Comparator.comparing(Recipe::getName));

        VBox vbox = new VBox(10);

        vbox.getChildren().add(new Label("These are the recipes that can be used to create: " + item.getName()));

        for (Recipe recipe : defaultRecipes){
            Node n = createRecipeNode(recipe, item);
            VBox.setVgrow(n, Priority.NEVER);
            vbox.getChildren().add(n);
        }

        for (Recipe recipe : alternateRecipes){
            Node n = createRecipeNode(recipe, item);
            VBox.setVgrow(n, Priority.NEVER);
            vbox.getChildren().add(n);
        }

        TitledPane craftingPane = new TitledPane();
        craftingPane.setText("Crafting");
        craftingPane.setCollapsible(true);
        craftingPane.setExpanded(true);
        craftingPane.setContent(vbox);
        return craftingPane;
    }

    private static Node createUsesPane(Item item, GameData gameData)
    {
        List<Recipe> recipes = new LinkedList<>();

        for (Recipe r : gameData.getRecipes()){
            if (r.consumesItem(item)){
                recipes.add(r);
            }
        }

        if (recipes.isEmpty()){
            Region r = new Region();
            r.setMaxWidth(0);
            r.setMaxHeight(0);
            r.setPrefWidth(0);
            r.setPrefHeight(0);
            return r;
        }

        VBox vbox = new VBox(10);

        vbox.getChildren().add(new Label("These are the recipes use: " + item.getName()));

        recipes = new ArrayList<>(recipes);
        recipes.sort(Comparator.comparing(Recipe::getName));

        for (Recipe recipe : recipes){
            Node n = createRecipeNode(recipe, item);
            VBox.setVgrow(n, Priority.NEVER);
            vbox.getChildren().add(n);
        }

        TitledPane craftingPane = new TitledPane();
        craftingPane.setText("Uses");
        craftingPane.setCollapsible(true);
        craftingPane.setExpanded(true);
        craftingPane.setContent(vbox);
        return craftingPane;
    }

    private static Node createRecipeNode(Recipe recipe, Item primaryItem)
    {
        VBox vbox = new VBox(10);

        {
            Label recipeNameLabel = new Label(recipe.getName());
            recipeNameLabel.setStyle("-fx-font-weight: bold;");
            VBox.setVgrow(recipeNameLabel, Priority.NEVER);
            vbox.getChildren().add(recipeNameLabel);
        }

        {
            HBox hBox = new HBox(10);
            hBox.setFillHeight(false);
            VBox.setVgrow(hBox, Priority.NEVER);
            VBox.setMargin(hBox, new Insets(0, 30, 0, 30));
            vbox.getChildren().add(hBox);

            hBox.getChildren().add(createRecipeItemsTable("Ingredients", recipe.getIngredients()));

            {
                GridPane gridPane = new GridPane();
                gridPane.getColumnConstraints().add(new ColumnConstraints());
                gridPane.getColumnConstraints().add(new ColumnConstraints(120));
                hBox.getChildren().add(gridPane);

                {
                    Label l = new Label("Produced in");
                    l.setMaxWidth(Double.MAX_VALUE);
                    l.setStyle("-fx-font-weight: bold;");
                    setBorder(l, 1, 1, 1, 1);
                    l.setPadding(new Insets(5));
                    GridPane.setFillWidth(l, true);
                    gridPane.add(l, 0, 0);
                }

                {
                    Label l = new Label(recipe.getProducedInBuilding().getName());
                    l.setMaxWidth(Double.MAX_VALUE);
                    setBorder(l, 1, 1, 1, 0);
                    l.setPadding(new Insets(5));
                    GridPane.setFillWidth(l, true);
                    gridPane.add(l, 1, 0);
                }

                {
                    Label l = new Label("Cycle time");
                    l.setMaxWidth(Double.MAX_VALUE);
                    l.setStyle("-fx-font-weight: bold;");
                    setBorder(l, 0, 1, 1, 1);
                    l.setPadding(new Insets(5));
                    GridPane.setFillWidth(l, true);
                    gridPane.add(l, 0, 1);
                }

                {
                    Label l = new Label(BigDecimalUtils.normalize(recipe.getCycleTimeSeconds().setScale(2, RoundingMode.HALF_UP)).toString().concat(" secs"));
                    l.setMaxWidth(Double.MAX_VALUE);
                    setBorder(l, 0, 1, 1, 0);
                    l.setPadding(new Insets(5));
                    GridPane.setFillWidth(l, true);
                    gridPane.add(l, 1, 1);
                }

                {
                    Label l = new Label("Power usage");
                    l.setMaxWidth(Double.MAX_VALUE);
                    l.setStyle("-fx-font-weight: bold;");
                    setBorder(l, 0, 1, 1, 1);
                    l.setPadding(new Insets(5));
                    GridPane.setFillWidth(l, true);
                    gridPane.add(l, 0, 2);
                }

                {
                    Label l = new Label(BigDecimalUtils.normalize(recipe.getPowerConsumption().setScale(2, RoundingMode.HALF_UP)).toString().concat(" MW"));
                    l.setMaxWidth(Double.MAX_VALUE);
                    setBorder(l, 0, 1, 1, 0);
                    l.setPadding(new Insets(5));
                    GridPane.setFillWidth(l, true);
                    gridPane.add(l, 1, 2);
                }
            }

            hBox.getChildren().add(createRecipeItemsTable("Products", recipe.getProducts()));
        }

        return vbox;
    }

    private static Node createRecipeItemsTable(String heading, Collection<? extends Recipe.RecipeItem> items)
    {
        GridPane gp = new GridPane();

        {
            ColumnConstraints c = new ColumnConstraints(220);
            c.setHgrow(Priority.ALWAYS);
            gp.getColumnConstraints().add(c);
            gp.getColumnConstraints().add(new ColumnConstraints());
            gp.getColumnConstraints().add(new ColumnConstraints(80));
        }

        {
            Label tableHeading = new Label(heading);
            tableHeading.setAlignment(Pos.CENTER);
            tableHeading.setMaxWidth(Double.MAX_VALUE);
            tableHeading.setStyle("-fx-font-weight: bold;");
            setBorder(tableHeading, 1, 1, 0, 1);
            tableHeading.setPadding(new Insets(5));
            GridPane.setFillWidth(tableHeading, true);
            GridPane.setColumnSpan(tableHeading, 3);
            gp.add(tableHeading, 0, 0);
        }

        {
            Label itemHeader = new Label("Item");
            itemHeader.setMaxWidth(Double.MAX_VALUE);
            itemHeader.setStyle("-fx-font-weight: bold;");
            setBorder(itemHeader, 1, 1, 1, 1);
            itemHeader.setPadding(new Insets(5));
            GridPane.setFillWidth(itemHeader, true);
            gp.add(itemHeader, 0, 1);
        }

        {
            Label perCycleHeader = new Label("/ cycle");
            perCycleHeader.setAlignment(Pos.BASELINE_RIGHT);
            perCycleHeader.setMaxWidth(Double.MAX_VALUE);
            perCycleHeader.setStyle("-fx-font-weight: bold;");
            setBorder(perCycleHeader, 1, 1, 1, 0);
            perCycleHeader.setPadding(new Insets(5));
            GridPane.setFillWidth(perCycleHeader, true);
            gp.add(perCycleHeader, 1, 1);
        }

        {
            Label perMinHeader = new Label("/ min");
            perMinHeader.setAlignment(Pos.BASELINE_RIGHT);
            perMinHeader.setMaxWidth(Double.MAX_VALUE);
            perMinHeader.setStyle("-fx-font-weight: bold;");
            setBorder(perMinHeader, 1, 1, 1, 0);
            perMinHeader.setPadding(new Insets(5));
            GridPane.setFillWidth(perMinHeader, true);
            gp.add(perMinHeader, 2, 1);
        }

        List<Recipe.RecipeItem> sortedList = new ArrayList<>(items);
        sortedList.sort(Comparator.<Recipe.RecipeItem, String>comparing(recipeItem -> recipeItem.getItem().getName()));

        int row = 2;
        for (Recipe.RecipeItem ri : sortedList){
            Label itemName = new Label(ri.getItem().getName());
            itemName.setPadding(new Insets(5));
            itemName.setMaxWidth(Double.MAX_VALUE);
            setBorder(itemName, 0, 1, 1, 1);
            GridPane.setFillWidth(itemName, true);
            gp.add(itemName, 0, row);
            Label perCycle = new Label(ri.getItem().toDisplayAmount(ri.getAmount().getAmountPerCycle()).setScale(1, RoundingMode.HALF_UP).toString());
            perCycle.setPadding(new Insets(5));
            perCycle.setMaxWidth(Double.MAX_VALUE);
            perCycle.setAlignment(Pos.BASELINE_RIGHT);
            setBorder(perCycle, 0, 1, 1, 0);
            GridPane.setFillWidth(perCycle, true);
            gp.add(perCycle, 1, row);
            Label perMin = new Label(ri.getItem().toDisplayAmount(ri.getAmount().getAmountPerMinute()).setScale(4, RoundingMode.HALF_UP).toString());
            perMin.setPadding(new Insets(5));
            perMin.setMaxWidth(Double.MAX_VALUE);
            perMin.setAlignment(Pos.BASELINE_RIGHT);
            setBorder(perMin, 0, 1, 1, 0);
            GridPane.setFillWidth(perMin, true);
            gp.add(perMin, 2, row);
            row++;
        }

        return gp;
    }

    private static void setBorder(Region n, int top, int right, int bottom, int left)
    {
        n.setStyle(Objects.requireNonNullElse(n.getStyle(), "") + "-fx-border-color: -fx-box-border; -fx-border-width: " + top + "px " + right + "px " + bottom + "px " + left + "px;");
    }
}
