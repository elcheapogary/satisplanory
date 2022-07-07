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
import io.github.elcheapogary.satisplanory.prodplan.ProductionPlan;
import io.github.elcheapogary.satisplanory.satisfactory.SatisfactoryData;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import io.github.elcheapogary.satisplanory.util.Comparators;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.LongBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableLongValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

class OverviewTab
{
    private OverviewTab()
    {
    }

    public static Tab create(GameData gameData, ProdPlanModel model)
    {
        Tab tab = new Tab();
        tab.setClosable(false);
        tab.setText("Overview");

        setContent(tab, gameData, model);

        model.planProperty().addListener((observable, oldValue, newValue) -> setContent(tab, gameData, model));

        tab.disableProperty().bind(Bindings.createBooleanBinding(() -> model.planProperty().getValue() == null, model.planProperty()));

        return tab;
    }

    private static <R> TableColumn<R, BigFraction> createBigFractionColumn(String name, Function<? super R, ? extends BigFraction> valueExtractor, Function<? super BigFraction, String> toStringConverter)
    {
        TableColumn<R, BigFraction> col = new TableColumn<>(name);
        col.setStyle("-fx-alignment: CENTER_RIGHT;");
        col.cellValueFactoryProperty().set(param -> new SimpleObjectProperty<>(valueExtractor.apply(param.getValue())));
        col.setCellFactory(param -> new TableCell<>()
        {
            @Override
            protected void updateItem(BigFraction item, boolean empty)
            {
                if (item == null || empty){
                    setText("");
                }else{
                    setText(toStringConverter.apply(item));
                }
            }
        });

        return col;
    }

    private static TitledPane createMachinesPane(ProductionPlan plan)
    {
        VBox vbox = new VBox(10);

        VBox clockSpeedArea = new VBox(5);
        VBox.setVgrow(clockSpeedArea, Priority.NEVER);
        vbox.getChildren().add(clockSpeedArea);

        Slider slider = new Slider();
        clockSpeedArea.getChildren().add(slider);

        slider.setMin(0);
        slider.setMax(250);
        slider.setShowTickLabels(true);
        slider.setValue(100);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(10);
        slider.setMinorTickCount(9);
        slider.setSnapToTicks(true);

        {

            Label clockSpeedLabel = new Label();
            clockSpeedArea.getChildren().add(clockSpeedLabel);

            clockSpeedLabel.setMaxWidth(Double.MAX_VALUE);
            clockSpeedLabel.setAlignment(Pos.CENTER);
            clockSpeedLabel.textProperty().bind(Bindings.createStringBinding(() -> "Clock speed: " + (int)slider.valueProperty().get() + "%", slider.valueProperty()));
        }

        Map<String, MachinesRow> machineRowMap = new TreeMap<>();

        for (Recipe r : plan.getRecipes()){
            MachinesRow row = machineRowMap.computeIfAbsent(r.getProducedInBuilding().getName(), s -> new MachinesRow());
            row.machineName = r.getProducedInBuilding().getName();

            LongBinding l = Bindings.createLongBinding(() -> plan.getNumberOfMachinesWithRecipe(r)
                    .divide(BigFraction.valueOf(BigDecimal.valueOf(slider.valueProperty().get())).max(BigFraction.one()).divide(100))
                    .toBigDecimal(0, RoundingMode.UP)
                    .toBigInteger().longValue(), slider.valueProperty());

            if (row.numberOfMachines == null){
                row.numberOfMachines = l;
            }else{
                ObservableLongValue o = row.numberOfMachines;
                row.numberOfMachines = Bindings.createLongBinding(() -> l.get() + o.get(), l, o);
            }
        }

        ObservableList<MachinesRow> rows = FXCollections.observableList(new ArrayList<>(machineRowMap.values()));

        MachinesRow total = new MachinesRow();
        total.machineName = "Totals";
        {
            LongBinding b = Bindings.createLongBinding(() -> 0L);
            for (MachinesRow r : rows){
                final LongBinding tmp = b;
                b = Bindings.createLongBinding(() -> tmp.get() + r.numberOfMachines.get(), tmp, r.numberOfMachines);
            }
            total.numberOfMachines = b;
        }
        rows.add(total);

        TableView<MachinesRow> tableView = new TableView<>(rows);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        vbox.getChildren().add(tableView);

        tableView.setPrefWidth(0);
        tableView.setPrefHeight(0);

        {
            TableColumn<MachinesRow, String> col = new TableColumn<>("Machine");
            tableView.getColumns().add(col);
            col.setStyle("-fx-alignment: CENTER_LEFT;");
            col.cellValueFactoryProperty().set(param -> new SimpleObjectProperty<>(param.getValue().machineName));
        }

        {
            TableColumn<MachinesRow, Long> col = new TableColumn<>("Number");
            tableView.getColumns().add(col);
            col.setStyle("-fx-alignment: CENTER_RIGHT;");
            col.cellValueFactoryProperty().set(param -> Bindings.createObjectBinding(() -> param.getValue().numberOfMachines.get(), param.getValue().numberOfMachines));
        }

        tableView.setSortPolicy(param -> {
            Comparator<MachinesRow> comparator = Comparators.sortLast(resourceLine -> resourceLine.machineName.equals("Totals"));

            if (param.getComparator() != null){
                comparator = comparator.thenComparing(param.getComparator());
            }

            FXCollections.sort(param.getItems(), comparator);
            return true;
        });

        TitledPane tp = new TitledPane();
        tp.setText("Machines");
        tp.setContent(vbox);
        return tp;
    }

    private static Node createOverview(GameData gameData, ProdPlanModel model)
    {
        VBox vBox = new VBox(10);
        vBox.setPadding(new Insets(10));

        Accordion accordion = new Accordion();
        VBox.setVgrow(accordion, Priority.ALWAYS);
        vBox.getChildren().add(accordion);

        accordion.getPanes().add(createResourcePane(gameData, model));
        accordion.setExpandedPane(accordion.getPanes().get(0));
        accordion.getPanes().add(createMachinesPane(model.getPlan()));

        return vBox;
    }

    private static TitledPane createResourcePane(GameData gameData, ProdPlanModel model)
    {
        ProductionPlan plan = model.getPlan();

        TableView<ResourceLine> tableView = new TableView<>();

        {
            TableColumn<ResourceLine, String> col = new TableColumn<>("Resource");
            tableView.getColumns().add(col);
            col.setStyle("-fx-alignment: CENTER_LEFT;");
            col.cellValueFactoryProperty().set(param -> new SimpleObjectProperty<>(param.getValue().name));
        }

        tableView.getColumns().add(createBigFractionColumn(
                "Amount available",
                resourceLine -> resourceLine.amountAvailable,
                bigFraction -> bigFraction.toBigDecimal(4, RoundingMode.HALF_UP).toString()
        ));

        tableView.getColumns().add(createBigFractionColumn(
                "Max extract rate",
                resourceLine -> resourceLine.maxExtractRate,
                bigFraction -> bigFraction.toBigInteger().toString()
        ));

        tableView.getColumns().add(createBigFractionColumn(
                "Amount used",
                resourceLine -> resourceLine.amountUsed,
                bigFraction -> bigFraction.toBigDecimal(4, RoundingMode.HALF_UP).toString()
        ));

        tableView.getColumns().add(createBigFractionColumn(
                "% of available",
                resourceLine -> resourceLine.percentageOfAvailable,
                bigFraction -> bigFraction.toBigDecimal(4, RoundingMode.HALF_UP).toString().concat("%")
        ));

        tableView.getColumns().add(createBigFractionColumn(
                "% of max",
                resourceLine -> resourceLine.percentageOfMaxExtractRate,
                bigFraction -> bigFraction.toBigDecimal(4, RoundingMode.HALF_UP).toString().concat("%")
        ));

        List<Item> sortedItems = new ArrayList<>(plan.getInputItems());

        sortedItems.sort(Comparator.comparing(Item::getName));

        BigFraction totalAmountUsed = BigFraction.zero();
        BigFraction totalAmountAvailable = BigFraction.zero();
        BigFraction totalMaxExtractRate = BigFraction.zero();

        for (Item item : sortedItems){
            Long max = SatisfactoryData.getResourceExtractionLimits().get(item.getName());
            if (max != null || item.getName().equals("Water")){
                ResourceLine l = new ResourceLine();
                tableView.getItems().add(l);
                l.name = item.getName();
                l.amountUsed = item.toDisplayAmount(plan.getInputItemsPerMinute(item));
                l.amountAvailable = model.getInputItemsPerMinute(item);
                l.percentageOfAvailable = l.amountUsed.divide(l.amountAvailable).multiply(100);
                if (max == null){
                    l.maxExtractRate = null;
                    l.percentageOfMaxExtractRate = null;
                }else{
                    l.maxExtractRate = item.toDisplayAmount(BigFraction.valueOf(max));
                    l.percentageOfMaxExtractRate = l.amountUsed.divide(l.maxExtractRate).multiply(100);
                    totalAmountUsed = totalAmountUsed.add(l.amountUsed);
                    totalAmountAvailable = totalAmountAvailable.add(l.amountAvailable);
                    totalMaxExtractRate = totalMaxExtractRate.add(l.maxExtractRate);
                }
            }
        }

        ResourceLine l = new ResourceLine();
        l.name = "Total (Resources Used)";
        l.totalIndex = 0;
        l.amountAvailable = totalAmountAvailable;
        l.amountUsed = totalAmountUsed;
        l.maxExtractRate = totalMaxExtractRate;
        if (l.amountAvailable.signum() > 0){
            l.percentageOfAvailable = l.amountUsed.divide(l.amountAvailable).multiply(100);
        }
        if (l.maxExtractRate.signum() > 0){
            l.percentageOfMaxExtractRate = l.amountUsed.divide(l.maxExtractRate).multiply(100);
        }
        tableView.getItems().add(l);

        totalMaxExtractRate = BigFraction.zero();

        for (var entry : SatisfactoryData.getResourceExtractionLimits().entrySet()){
            Item item = gameData.getItemByName(entry.getKey()).orElse(null);

            if (item != null){
                totalMaxExtractRate = totalMaxExtractRate.add(item.toDisplayAmount(BigFraction.valueOf(entry.getValue())));
            }
        }

        l = new ResourceLine();
        l.name = "Total (All Resources)";
        l.totalIndex = 1;
        l.amountAvailable = totalAmountAvailable;
        l.amountUsed = totalAmountUsed;
        l.maxExtractRate = totalMaxExtractRate;
        if (l.amountAvailable.signum() > 0){
            l.percentageOfAvailable = l.amountUsed.divide(l.amountAvailable).multiply(100);
        }
        if (l.maxExtractRate.signum() > 0){
            l.percentageOfMaxExtractRate = l.amountUsed.divide(l.maxExtractRate).multiply(100);
        }
        tableView.getItems().add(l);

        tableView.setSortPolicy(param -> {
            Comparator<ResourceLine> comparator = Comparator.comparingInt(value -> value.totalIndex);

            if (param.getComparator() != null){
                comparator = comparator.thenComparing(param.getComparator());
            }

            FXCollections.sort(param.getItems(), comparator);
            return true;
        });

        tableView.setPrefWidth(0);

        TitledPane tp = new TitledPane();
        tp.setText("Resources");
        tp.setContent(tableView);
        return tp;
    }

    private static void setContent(Tab tab, GameData gameData, ProdPlanModel model)
    {
        if (model.getPlan() == null){
            tab.setContent(new Pane());
        }else{
            tab.setContent(createOverview(gameData, model));
        }
    }

    private static class MachinesRow
    {
        private String machineName;
        private ObservableLongValue numberOfMachines;
    }

    private static class ResourceLine
    {
        private int totalIndex = -1;
        private String name;
        private BigFraction amountUsed;
        private BigFraction amountAvailable;
        private BigFraction maxExtractRate;
        private BigFraction percentageOfAvailable;
        private BigFraction percentageOfMaxExtractRate;
    }
}
