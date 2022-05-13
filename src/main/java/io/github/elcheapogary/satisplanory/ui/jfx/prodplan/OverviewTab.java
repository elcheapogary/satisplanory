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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class OverviewTab
{
    private OverviewTab()
    {
    }

    public static Node create(ProductionPlan plan)
    {
        VBox vBox = new VBox(10);
        vBox.setPadding(new Insets(10));

        Accordion accordion = new Accordion();
        VBox.setVgrow(accordion, Priority.ALWAYS);
        vBox.getChildren().add(accordion);

        accordion.getPanes().add(createResourcePane(plan));
        accordion.setExpandedPane(accordion.getPanes().get(0));
        accordion.getPanes().add(createMachinesPane(plan));

        return vBox;
    }

    private static TitledPane createResourcePane(ProductionPlan plan)
    {
        TableView<ResourceLine> tableView = new TableView<>();

        {
            TableColumn<ResourceLine, String> col = new TableColumn<>("Resource");
            tableView.getColumns().add(col);
            col.setStyle("-fx-alignment: CENTER_LEFT;");
            col.cellValueFactoryProperty().set(param -> new SimpleObjectProperty<>(param.getValue().name));
        }

        {
            TableColumn<ResourceLine, BigDecimal> col = new TableColumn<>("Max extract rate");
            tableView.getColumns().add(col);
            col.setStyle("-fx-alignment: CENTER_RIGHT;");
            col.cellValueFactoryProperty().set(param -> new SimpleObjectProperty<>(param.getValue().maxExtractRate));
        }

        {
            TableColumn<ResourceLine, BigDecimal> col = new TableColumn<>("Amount used");
            tableView.getColumns().add(col);
            col.setStyle("-fx-alignment: CENTER_RIGHT;");
            col.cellValueFactoryProperty().set(param -> new SimpleObjectProperty<>(param.getValue().amount));
        }

        {
            TableColumn<ResourceLine, BigDecimal> col = new TableColumn<>("% of max");
            tableView.getColumns().add(col);
            col.setStyle("-fx-alignment: CENTER_RIGHT;");
            col.cellValueFactoryProperty().set(param -> new SimpleObjectProperty<>(param.getValue().percentageOfMaxExtractRate));
            col.setCellFactory(param -> new TableCell<>()
            {
                @Override
                protected void updateItem(BigDecimal item, boolean empty)
                {
                    if (item == null || empty){
                        setText("");
                    }else{
                        setText(item.toString().concat("%"));
                    }
                }
            });
        }

        List<Item> sortedItems = new ArrayList<>(plan.getInputItems());

        sortedItems.sort(Comparator.comparing(Item::getName));

        BigFraction totalAmount = BigFraction.ZERO;
        BigFraction totalMaxExtractRate = BigFraction.ZERO;
        BigFraction totalPercentOfMaxExtractRate = BigFraction.ZERO;

        for (Item item : sortedItems){
            Long max = SatisfactoryData.getResourceExtractionLimits().get(item.getName());
            if (max != null || item.getName().equals("Water")){
                ResourceLine l = new ResourceLine();
                tableView.getItems().add(l);
                l.name = item.getName();
                l.amount = item.toDisplayAmount(plan.getInputItemsPerMinute(item)).toBigDecimal(4, RoundingMode.HALF_UP);
                totalAmount = totalAmount.add(item.toDisplayAmount(plan.getInputItemsPerMinute(item)));
                if (max == null){
                    l.maxExtractRate = null;
                    l.percentageOfMaxExtractRate = null;
                }else{
                    totalMaxExtractRate = totalMaxExtractRate.add(item.toDisplayAmount(BigFraction.valueOf(max)));
                    totalPercentOfMaxExtractRate = totalPercentOfMaxExtractRate.add(plan.getInputItemsPerMinute(item).divide(max));
                    l.maxExtractRate = item.toDisplayAmount(BigFraction.valueOf(max)).toBigDecimal(0, RoundingMode.DOWN);
                    l.percentageOfMaxExtractRate = plan.getInputItemsPerMinute(item).divide(max).multiply(100)
                            .toBigDecimal(6, RoundingMode.HALF_UP);
                }
            }
        }

        ResourceLine totals = new ResourceLine();
        totals.name = "Totals";
        totals.amount = totalAmount.toBigDecimal(4, RoundingMode.HALF_UP);
        totals.maxExtractRate = totalMaxExtractRate.toBigDecimal(0, RoundingMode.DOWN);
        totals.percentageOfMaxExtractRate = totalPercentOfMaxExtractRate.multiply(100).toBigDecimal(6, RoundingMode.HALF_UP);
        tableView.getItems().add(totals);

        tableView.setSortPolicy(param -> {
            Comparator<ResourceLine> comparator = Comparators.sortLast(resourceLine -> resourceLine.name.equals("Totals"));

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
            clockSpeedLabel.textProperty().bind(Bindings.createStringBinding(() -> "Clock speed: " + (int) slider.valueProperty().get() + "%", slider.valueProperty()));
        }

        Map<String, MachinesRow> machineRowMap = new TreeMap<>();

        for (Recipe r : plan.getRecipes()){
            MachinesRow row = machineRowMap.computeIfAbsent(r.getProducedInBuilding().getName(), s -> new MachinesRow());
            row.machineName = r.getProducedInBuilding().getName();

            LongBinding l = Bindings.createLongBinding(() -> plan.getNumberOfMachinesWithRecipe(r)
                    .divide(BigFraction.valueOf(BigDecimal.valueOf(slider.valueProperty().get())).max(BigFraction.ONE).divide(100))
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

    private static class ResourceLine
    {
        private String name;
        private BigDecimal amount;
        private BigDecimal maxExtractRate;
        private BigDecimal percentageOfMaxExtractRate;
    }

    private static class MachinesRow
    {
        private String machineName;
        private ObservableLongValue numberOfMachines;
    }
}
