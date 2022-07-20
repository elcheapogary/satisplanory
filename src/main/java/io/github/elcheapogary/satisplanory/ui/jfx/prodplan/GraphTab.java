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
import io.github.elcheapogary.satisplanory.prodplan.graph.data.InputItemNodeData;
import io.github.elcheapogary.satisplanory.prodplan.graph.data.OutputItemNodeData;
import io.github.elcheapogary.satisplanory.prodplan.graph.data.ProdPlanEdgeData;
import io.github.elcheapogary.satisplanory.prodplan.graph.data.ProdPlanGraphML;
import io.github.elcheapogary.satisplanory.prodplan.graph.data.ProdPlanNodeData;
import io.github.elcheapogary.satisplanory.prodplan.graph.data.RecipeNodeData;
import io.github.elcheapogary.satisplanory.prodplan.graph.lib.Edge;
import io.github.elcheapogary.satisplanory.prodplan.graph.lib.Graph;
import io.github.elcheapogary.satisplanory.prodplan.graph.lib.Node;
import io.github.elcheapogary.satisplanory.ui.jfx.component.ZoomableScrollPane;
import io.github.elcheapogary.satisplanory.ui.jfx.context.AppContext;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.ExceptionDialog;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.TaskProgressDialog;
import io.github.elcheapogary.satisplanory.util.BigDecimalUtils;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurve;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import org.controlsfx.control.Notifications;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.graph.DirectedGraph;
import org.eclipse.draw2d.graph.DirectedGraphLayout;

class GraphTab
{
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private GraphTab()
    {
    }

    /**
     * Adds edge to layout graph. We need this method to suppress unchecked warnings. External code is unchecked, not
     * ours.
     *
     * @param layoutGraph The graph to add the edge to.
     * @param edge        The edge to add to the graph.
     */
    @SuppressWarnings("unchecked")
    private static void addEdgeToLayoutGraph(DirectedGraph layoutGraph, org.eclipse.draw2d.graph.Edge edge)
    {
        layoutGraph.edges.add(edge);
    }

    private static void addLine(Region from, Region to, Region lineDescription, Pane pane, BooleanBinding selectedBinding, boolean curved)
    {
        Path arrow = new Path();
        arrow.getElements().add(new MoveTo(0, 0));
        arrow.getElements().add(new LineTo(-4, 10));
        arrow.getElements().add(new LineTo(4, 10));
        arrow.getElements().add(new LineTo(0, 0));
        arrow.setFill(Color.BLACK);
        arrow.setStroke(Color.BLACK);

        DoubleBinding fromCenterXBinding = from.layoutXProperty().add(from.translateXProperty()).add(from.widthProperty().divide(2.0));
        DoubleBinding fromCenterYBinding = from.layoutYProperty().add(from.translateYProperty()).add(from.heightProperty().divide(2.0));
        DoubleBinding toCenterXBinding = to.layoutXProperty().add(to.translateXProperty()).add(to.widthProperty().divide(2.0));
        DoubleBinding toCenterYBinding = to.layoutYProperty().add(to.translateYProperty()).add(to.heightProperty().divide(2.0));

        if (curved){
            ObjectBinding<Point2D> directFromPointBinding = Bindings.createObjectBinding(() -> getRectangleEdge(fromCenterXBinding.get(), fromCenterYBinding.get(), from.widthProperty().get(), from.heightProperty().get(), toCenterXBinding.get(), toCenterYBinding.get()), fromCenterXBinding, fromCenterYBinding, toCenterXBinding, toCenterYBinding, from.widthProperty(), from.heightProperty());
            ObjectBinding<Point2D> directToPointBinding = Bindings.createObjectBinding(() -> getRectangleEdge(toCenterXBinding.get(), toCenterYBinding.get(), to.widthProperty().get(), to.heightProperty().get(), fromCenterXBinding.get(), fromCenterYBinding.get()), fromCenterXBinding, fromCenterYBinding, toCenterXBinding, toCenterYBinding, to.widthProperty(), to.heightProperty());

            ObjectBinding<Point2D> controlPointBinding = Bindings.createObjectBinding(() -> {
                double distance = directFromPointBinding.get().distance(directToPointBinding.get());
                double xdiff = directToPointBinding.get().getX() - directFromPointBinding.get().getX();
                double ydiff = directToPointBinding.get().getY() - directFromPointBinding.get().getY();

                double x = directFromPointBinding.get().getX() + (xdiff / 2.0);
                double y = directFromPointBinding.get().getY() + (ydiff / 2.0);

                y -= (xdiff / distance) * 40.0;
                x += (ydiff / distance) * 40.0;

                return new Point2D(x, y);
            }, directToPointBinding, directFromPointBinding);
            ObjectBinding<Point2D> fromPointBinding = Bindings.createObjectBinding(() -> getRectangleEdge(fromCenterXBinding.get(), fromCenterYBinding.get(), from.widthProperty().get(), from.heightProperty().get(), controlPointBinding.get().getX(), controlPointBinding.get().getY()), fromCenterXBinding, fromCenterYBinding, controlPointBinding, from.widthProperty(), from.heightProperty());
            ObjectBinding<Point2D> toPointBinding = Bindings.createObjectBinding(() -> getRectangleEdge(toCenterXBinding.get(), toCenterYBinding.get(), to.widthProperty().get(), to.heightProperty().get(), controlPointBinding.get().getX(), controlPointBinding.get().getY()), controlPointBinding, toCenterXBinding, toCenterYBinding, to.widthProperty(), to.heightProperty());

            QuadCurve line = new QuadCurve();
            line.startXProperty().bind(Bindings.createDoubleBinding(() -> fromPointBinding.get().getX(), fromPointBinding));
            line.startYProperty().bind(Bindings.createDoubleBinding(() -> fromPointBinding.get().getY(), fromPointBinding));
            line.endXProperty().bind(Bindings.createDoubleBinding(() -> toPointBinding.get().getX(), toPointBinding));
            line.endYProperty().bind(Bindings.createDoubleBinding(() -> toPointBinding.get().getY(), toPointBinding));
            line.controlXProperty().bind(Bindings.createDoubleBinding(() -> controlPointBinding.get().getX(), controlPointBinding));
            line.controlYProperty().bind(Bindings.createDoubleBinding(() -> controlPointBinding.get().getY(), controlPointBinding));

            line.strokeWidthProperty().bind(Bindings.createDoubleBinding(() -> {
                if (selectedBinding.get()){
                    return 2.0;
                }else{
                    return 1.0;
                }
            }, selectedBinding));

            line.setFill(null);
            line.setStroke(Color.BLACK);

            lineDescription.layoutXProperty().bind(Bindings.createDoubleBinding(() -> controlPointBinding.get().getX() - (lineDescription.widthProperty().get() / 2.0), controlPointBinding, lineDescription.widthProperty()));
            lineDescription.layoutYProperty().bind(Bindings.createDoubleBinding(() -> controlPointBinding.get().getY() - (lineDescription.heightProperty().get() / 2.0), controlPointBinding, lineDescription.heightProperty()));

            arrow.layoutXProperty().bind(Bindings.createDoubleBinding(() -> toPointBinding.get().getX(), toPointBinding));
            arrow.layoutYProperty().bind(Bindings.createDoubleBinding(() -> toPointBinding.get().getY(), toPointBinding));

            {
                Rotate rotate = new Rotate();
                rotate.setPivotX(0);
                rotate.setPivotY(0);
                rotate.angleProperty().bind(Bindings.createDoubleBinding(() -> {
                    Point2D nv = toPointBinding.get().subtract(controlPointBinding.get());
                    double angle = new Point2D(0, -1).angle(nv);
                    if (nv.getX() < 0){
                        return 0 - angle;
                    }else{
                        return angle;
                    }
                }, controlPointBinding, toPointBinding));
                arrow.getTransforms().add(rotate);
            }

            pane.getChildren().addAll(arrow, line, lineDescription);
        }else{
            Line line = new Line();

            line.strokeWidthProperty().bind(Bindings.createDoubleBinding(() -> {
                if (selectedBinding.get()){
                    return 2.0;
                }else{
                    return 1.0;
                }
            }, selectedBinding));

            ObjectBinding<Point2D> fromPointBinding = Bindings.createObjectBinding(() -> getRectangleEdge(fromCenterXBinding.get(), fromCenterYBinding.get(), from.widthProperty().get(), from.heightProperty().get(), toCenterXBinding.get(), toCenterYBinding.get()), fromCenterXBinding, fromCenterYBinding, toCenterXBinding, toCenterYBinding, from.widthProperty(), from.heightProperty());
            ObjectBinding<Point2D> toPointBinding = Bindings.createObjectBinding(() -> getRectangleEdge(toCenterXBinding.get(), toCenterYBinding.get(), to.widthProperty().get(), to.heightProperty().get(), fromCenterXBinding.get(), fromCenterYBinding.get()), fromCenterXBinding, fromCenterYBinding, toCenterXBinding, toCenterYBinding, to.widthProperty(), to.heightProperty());

            line.startXProperty().bind(Bindings.createDoubleBinding(() -> fromPointBinding.get().getX(), fromPointBinding));
            line.startYProperty().bind(Bindings.createDoubleBinding(() -> fromPointBinding.get().getY(), fromPointBinding));
            line.endXProperty().bind(Bindings.createDoubleBinding(() -> toPointBinding.get().getX(), toPointBinding));
            line.endYProperty().bind(Bindings.createDoubleBinding(() -> toPointBinding.get().getY(), toPointBinding));

            lineDescription.layoutXProperty().bind(
                    from.layoutXProperty().add(from.widthProperty().divide(2.0))
                            .add(to.layoutXProperty().add(to.widthProperty().divide(2.0)))
                            .divide(2.0)
                            .subtract(lineDescription.widthProperty().divide(2.0))
            );
            lineDescription.layoutYProperty().bind(
                    from.layoutYProperty().add(from.heightProperty().divide(2.0))
                            .add(to.layoutYProperty().add(to.heightProperty().divide(2.0)))
                            .divide(2.0)
                            .subtract(lineDescription.heightProperty().divide(2.0))
            );
            lineDescription.translateXProperty().bind(
                    from.layoutXProperty().add(from.translateXProperty())
                            .add(from.widthProperty().divide(2.0))
                            .add(to.layoutXProperty().add(to.translateXProperty()))
                            .add(to.widthProperty().divide(2.0))
                            .divide(2.0)
                            .subtract(lineDescription.widthProperty().divide(2.0))
                            .subtract(lineDescription.layoutXProperty())
            );
            lineDescription.translateYProperty().bind(
                    from.layoutYProperty().add(from.translateYProperty())
                            .add(from.heightProperty().divide(2.0))
                            .add(to.layoutYProperty().add(to.translateYProperty()))
                            .add(to.heightProperty().divide(2.0))
                            .divide(2.0)
                            .subtract(lineDescription.heightProperty().divide(2.0))
                            .subtract(lineDescription.layoutYProperty())
            );

            arrow.layoutXProperty().bind(Bindings.createDoubleBinding(() -> toPointBinding.get().getX(), toPointBinding));
            arrow.layoutYProperty().bind(Bindings.createDoubleBinding(() -> toPointBinding.get().getY(), toPointBinding));

            {
                Rotate rotate = new Rotate();
                rotate.setPivotX(0);
                rotate.setPivotY(0);
                rotate.angleProperty().bind(Bindings.createDoubleBinding(() -> {
                    Point2D nv = toPointBinding.get().subtract(fromPointBinding.get());
                    double angle = new Point2D(0, -1).angle(nv);
                    if (nv.getX() < 0){
                        return 0 - angle;
                    }else{
                        return angle;
                    }
                }, fromPointBinding, toPointBinding));
                arrow.getTransforms().add(rotate);
            }
            pane.getChildren().addAll(arrow, line, lineDescription);
        }
    }

    /**
     * Adds node to layout graph. We need this method to suppress unchecked warnings. External code is unchecked, not
     * ours.
     *
     * @param layoutGraph The graph to add the node to.
     * @param node        The node to add to the graph.
     */
    @SuppressWarnings("unchecked")
    private static void addNodeToLayoutGraph(DirectedGraph layoutGraph, org.eclipse.draw2d.graph.Node node)
    {
        layoutGraph.nodes.add(node);
    }

    private static void configureGraphContextMenu(AppContext appContext, ScrollPane scrollPane, Pane graphPane, Graph<ProdPlanNodeData, ProdPlanEdgeData> graph)
    {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setAutoHide(true);

        {
            MenuItem menuItem = new MenuItem("Copy image");
            contextMenu.getItems().add(menuItem);

            menuItem.onActionProperty().set(event -> {
                SnapshotParameters parameters = new SnapshotParameters();
                parameters.setFill(Color.TRANSPARENT);

                WritableImage img = graphPane.snapshot(parameters, null);

                Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.IMAGE, img));
            });
        }

        {
            MenuItem menuItem = new MenuItem("Export image");
            contextMenu.getItems().add(menuItem);

            menuItem.onActionProperty().set(event -> {
                FileChooser fc = new FileChooser();
                if (appContext.getPersistentData().getPreferences().getLastImportExportDirectory() != null && appContext.getPersistentData().getPreferences().getLastImportExportDirectory().isDirectory()){
                    fc.setInitialDirectory(appContext.getPersistentData().getPreferences().getLastImportExportDirectory());
                }
                fc.setTitle("Select output file");
                fc.setInitialFileName("graph.png");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Files", "*.png"));
                File f = fc.showSaveDialog(graphPane.getScene().getWindow());
                if (f != null){
                    appContext.getPersistentData().getPreferences().setLastImportExportDirectory(f.getAbsoluteFile().getParentFile());

                    SnapshotParameters parameters = new SnapshotParameters();
                    parameters.setFill(Color.TRANSPARENT);

                    WritableImage img = graphPane.snapshot(parameters, null);

                    new TaskProgressDialog(appContext)
                            .setTitle("Saving image")
                            .setContentText("Saving image")
                            .runTask(taskContext -> {
                                try {
                                    ImageIO.write(SwingFXUtils.fromFXImage(img, null), "PNG", f);
                                }catch (IOException e){
                                    Platform.runLater(() -> new ExceptionDialog(appContext)
                                            .setTitle("Error saving image")
                                            .setContextMessage("An error occurred while saving the image")
                                            .setException(e)
                                            .showAndWait());
                                }

                                return null;
                            });
                }
            });
        }

        {
            MenuItem menuItem = new MenuItem("Export GraphML");
            contextMenu.getItems().add(menuItem);

            menuItem.onActionProperty().set(event -> {
                FileChooser fc = new FileChooser();
                if (appContext.getPersistentData().getPreferences().getLastImportExportDirectory() != null && appContext.getPersistentData().getPreferences().getLastImportExportDirectory().isDirectory()){
                    fc.setInitialDirectory(appContext.getPersistentData().getPreferences().getLastImportExportDirectory());
                }
                fc.setTitle("Select output file");
                fc.setInitialFileName("graph.graphml");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("GraphML Files", "*.graphml"));
                File f = fc.showSaveDialog(graphPane.getScene().getWindow());
                if (f != null){
                    appContext.getPersistentData().getPreferences().setLastImportExportDirectory(f.getAbsoluteFile().getParentFile());

                    new TaskProgressDialog(appContext)
                            .setTitle("Exporting GraphML")
                            .setContentText("Exporting GraphML")
                            .runTask(taskContext -> {
                                try {
                                    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(f))) {
                                        ProdPlanGraphML.export(out, graph);
                                    }
                                }catch (IOException | RuntimeException e){
                                    Platform.runLater(() -> new ExceptionDialog(appContext)
                                            .setTitle("Error exporting image")
                                            .setContextMessage("An error occurred while exporting the data")
                                            .setException(e)
                                            .showAndWait());
                                }

                                return null;
                            });
                }
            });
        }

        javafx.scene.Node contextMenuParent = scrollPane.getContent();

        contextMenuParent.setOnContextMenuRequested(event -> {
            contextMenu.show(contextMenuParent, event.getScreenX(), event.getScreenY());
            event.consume();
        });
    }

    private static <N, E> void configureMouseEvents(Node<N, E> node, Region region, Pane parent, ObjectProperty<? super Node<N, E>> selectedNodeProperty, PaneState paneState)
    {
        final DragInfo dragInfo = new DragInfo();

        region.onMousePressedProperty().set(event -> {
            selectedNodeProperty.set(node);
            dragInfo.startSceneX = event.getSceneX();
            dragInfo.startSceneY = event.getSceneY();
            dragInfo.dragged = false;
            event.consume();
        });
        region.onMouseDraggedProperty().set(event -> {
            dragInfo.dragged = true;
            paneState.hadAnyDragging = true;
            region.translateXProperty().set((event.getSceneX() - dragInfo.startSceneX) / parent.getScaleX());
            region.translateYProperty().set((event.getSceneY() - dragInfo.startSceneY) / parent.getScaleY());
            event.consume();
        });
        region.onMouseDragReleasedProperty().set(event -> {
            region.layoutXProperty().set(region.layoutXProperty().doubleValue() + region.translateXProperty().doubleValue());
            region.layoutYProperty().set(region.layoutYProperty().doubleValue() + region.translateYProperty().doubleValue());
            region.translateXProperty().set(0);
            region.translateYProperty().set(0);
        });
        region.onMouseReleasedProperty().set(event -> {
            if (dragInfo.dragged){
                region.layoutXProperty().set(region.layoutXProperty().doubleValue() + region.translateXProperty().doubleValue());
                region.layoutYProperty().set(region.layoutYProperty().doubleValue() + region.translateYProperty().doubleValue());
                region.translateXProperty().set(0);
                region.translateYProperty().set(0);
            }
        });
    }

    public static Tab create(AppContext appContext, ProdPlanModel model)
    {
        Tab tab = new Tab();
        tab.setClosable(false);
        tab.setText("Graph");

        setContent(tab, appContext, model.getPlan());

        model.planProperty().addListener((observable, oldValue, newValue) -> setContent(tab, appContext, newValue));

        tab.disableProperty().bind(Bindings.createBooleanBinding(() -> model.planProperty().getValue() == null, model.planProperty()));

        return tab;
    }

    private static Region createEdgeComponent(ProdPlanEdgeData edgeData, BooleanBinding selectedBinding)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (var entry : edgeData.getItemAmountsMap().entrySet()){
            Item item = entry.getKey();
            BigFraction amount = entry.getValue();
            if (first){
                first = false;
            }else{
                sb.append("\n");
            }
            sb.append(item.getName());
            sb.append("\n");
            sb.append(BigDecimalUtils.normalize(item.toDisplayAmount(amount).toBigDecimal(4, RoundingMode.HALF_UP)));
            sb.append(" / min");
        }

        Label l = new Label(sb.toString());
        l.setWrapText(true);
        l.setTextAlignment(TextAlignment.CENTER);
        l.setPadding(new javafx.geometry.Insets(5));

        l.backgroundProperty().bind(Bindings.createObjectBinding(() -> {
            if (selectedBinding.get()){
                return Background.fill(Color.gray(0.0, 0.8));
            }else{
                return null;
            }
        }, selectedBinding));

        l.styleProperty().bind(Bindings.createStringBinding(() -> {
            if (selectedBinding.get()){
                return "-fx-text-fill: white;";
            }else{
                return null;
            }
        }, selectedBinding));

        l.viewOrderProperty().bind(Bindings.createObjectBinding(() -> {
            if (selectedBinding.get()){
                return -100;
            }else{
                return null;
            }
        }, selectedBinding));

        return l;
    }

    private static Region createGraphPane(AppContext appContext, ProductionPlan plan)
    {
        BorderPane bp = new BorderPane();
        ObjectProperty<Node<ProdPlanNodeData, ProdPlanEdgeData>> selectedNodeProperty = new SimpleObjectProperty<>();

        selectedNodeProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue == null){
                bp.setRight(null);
            }else{
                bp.setRight(createNodeDetailPane(newValue.getData()));
            }
        });

        var graph = plan.toGraph();

        Pane pane = createGraphPane(graph, GraphTab::createNodeComponent, GraphTab::createEdgeComponent, selectedNodeProperty);

        ScrollPane sp = ZoomableScrollPane.create(pane, 0.1, 5.0);
        sp.setPannable(true);
        bp.setCenter(sp);

        configureGraphContextMenu(appContext, sp, pane, graph);

        return bp;
    }

    private static <N, E> Pane createGraphPane(Graph<N, E> graph, BiFunction<? super N, ? super BooleanBinding, ? extends Region> nodeComponentFactory, BiFunction<? super E, ? super BooleanBinding, ? extends Region> edgeComponentFactory, ObjectProperty<Node<N, E>> selectedNodeProperty)
    {
        Map<Node<N, E>, Region> nodeComponentMap = new TreeMap<>(Comparator.comparing(Node::getName));

        final PaneState paneState = new PaneState();

        final Pane pane = new Pane()
        {
            @Override
            protected double computeMinHeight(double width)
            {
                return super.computeMinHeight(width) + 40.0;
            }

            @Override
            protected double computeMinWidth(double height)
            {
                return super.computeMinWidth(height) + 40.0;
            }

            @Override
            protected double computePrefHeight(double width)
            {
                return super.computePrefHeight(width) + 40.0;
            }

            @Override
            protected double computePrefWidth(double height)
            {
                return super.computePrefWidth(height) + 40.0;
            }

            @Override
            protected void layoutChildren()
            {
                super.layoutChildren();
                if (!paneState.hadAnyDragging){
                    Platform.runLater(() -> doGraphLayout(getWidth(), getHeight(), nodeComponentMap));
                }
            }
        };

        for (Node<N, E> n : graph.getNodes()){
            Region component = nodeComponentFactory.apply(n.getData(), Bindings.createBooleanBinding(() -> selectedNodeProperty.getValue() == n, selectedNodeProperty));
            configureMouseEvents(n, component, pane, selectedNodeProperty, paneState);
            nodeComponentMap.put(n, component);
        }

        for (Node<N, E> n : graph.getNodes()){
            for (var entry : n.getOutgoingEdges().entrySet()){
                Node<N, E> c = entry.getKey();
                Edge<N, E> e = entry.getValue();

                BooleanBinding selectedBinding = Bindings.createBooleanBinding(() -> selectedNodeProperty.getValue() == n || selectedNodeProperty.getValue() == c, selectedNodeProperty);

                addLine(nodeComponentMap.get(n), nodeComponentMap.get(c), edgeComponentFactory.apply(e.getData(), selectedBinding), pane, selectedBinding, c.getEdgeTo(n) != null);
            }
        }

        for (Region r : nodeComponentMap.values()){
            pane.getChildren().add(r);
        }

        return pane;
    }

    private static Region createInputItemNodeDetailPane(InputItemNodeData nodeData)
    {
        VBox vbox = new VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(10));

        vbox.getChildren().add(new Label("Amount: " + BigDecimalUtils.normalize(nodeData.getAmount().toBigDecimal(4, RoundingMode.HALF_UP)) + " / min"));

        TitledPane titledPane = new TitledPane();
        titledPane.setCollapsible(false);
        titledPane.setAlignment(Pos.CENTER);
        titledPane.setMaxHeight(Double.MAX_VALUE);
        titledPane.getStyleClass().add("stpnr-tenbold");
        titledPane.setText("Input: " + nodeData.getItem().getName());
        titledPane.setContent(vbox);
        return titledPane;
    }

    private static Region createNodeComponent(ProdPlanNodeData nodeData, BooleanBinding selectedBinding)
    {
        VBox vbox = new VBox();
        vbox.getStyleClass().add("stpnr-graph-node");

        String line1;
        String line2;
        if (nodeData instanceof RecipeNodeData recipeNodeData){
            vbox.getStyleClass().add("stpnr-graph-node-recipe");
            line1 = recipeNodeData.getRecipe().getName();
            line2 = BigDecimalUtils.normalize(recipeNodeData.getAmount().toBigDecimal(6, RoundingMode.HALF_UP)) + " " + recipeNodeData.getRecipe().getProducedInBuilding().getName();
        }else if (nodeData instanceof InputItemNodeData inputItemNodeData){
            vbox.getStyleClass().add("stpnr-graph-node-input");
            line1 = "Input: " + inputItemNodeData.getItem().getName();
            line2 = BigDecimalUtils.normalize(inputItemNodeData.getItem().toDisplayAmount(inputItemNodeData.getAmount()).toBigDecimal(4, RoundingMode.HALF_UP)) + " / min";
        }else if (nodeData instanceof OutputItemNodeData outputItemNodeData){
            vbox.getStyleClass().add("stpnr-graph-node-output");
            line1 = "Output: " + outputItemNodeData.getItem().getName();
            line2 = BigDecimalUtils.normalize(outputItemNodeData.getItem().toDisplayAmount(outputItemNodeData.getAmount()).toBigDecimal(4, RoundingMode.HALF_UP)) + " / min";
        }else{
            throw new AssertionError();
        }

        vbox.setFillWidth(true);
        vbox.setPadding(new javafx.geometry.Insets(10));

        selectedBinding.addListener((observable, oldValue, newValue) -> vbox.pseudoClassStateChanged(SELECTED, newValue));

        Label l1 = new Label(line1);
        l1.setMaxWidth(Double.MAX_VALUE);
        l1.setStyle("-fx-font-weight: bold;");
        l1.setTextAlignment(TextAlignment.CENTER);
        l1.setAlignment(Pos.BASELINE_CENTER);
        l1.setTextFill(Color.BLACK);
        vbox.getChildren().add(l1);

        Label l2 = new Label(line2);
        l2.setMaxWidth(Double.MAX_VALUE);
        l2.setTextAlignment(TextAlignment.CENTER);
        l2.setAlignment(Pos.BASELINE_CENTER);
        l2.setTextFill(Color.BLACK);
        vbox.getChildren().add(l2);

        HBox hbox = new HBox();
        hbox.getChildren().add(vbox);

        hbox.viewOrderProperty().bind(Bindings.createDoubleBinding(() -> {
            if (selectedBinding.get()){
                return -200.0;
            }else{
                return 0.0;
            }
        }, selectedBinding));

        hbox.setBackground(Background.fill(Color.BLACK));
        hbox.setPadding(new javafx.geometry.Insets(0.5));

        return hbox;
    }

    private static Region createNodeDetailPane(ProdPlanNodeData data)
    {
        if (data instanceof InputItemNodeData inputItemNodeData){
            return createInputItemNodeDetailPane(inputItemNodeData);
        }else if (data instanceof OutputItemNodeData outputItemNodeData){
            return createOutputItemNodeDetailPane(outputItemNodeData);
        }else if (data instanceof RecipeNodeData recipeNodeData){
            return createRecipeNodeDetailPane(recipeNodeData);
        }else{
            return new Pane();
        }
    }

    private static Region createOutputItemNodeDetailPane(OutputItemNodeData nodeData)
    {
        VBox vbox = new VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(10));

        vbox.getChildren().add(new Label("Amount: " + BigDecimalUtils.normalize(nodeData.getAmount().toBigDecimal(4, RoundingMode.HALF_UP)) + " / min"));

        TitledPane titledPane = new TitledPane();
        titledPane.setCollapsible(false);
        titledPane.setAlignment(Pos.CENTER);
        titledPane.setMaxHeight(Double.MAX_VALUE);
        titledPane.getStyleClass().add("stpnr-tenbold");
        titledPane.setText("Output: " + nodeData.getItem().getName());
        titledPane.setContent(vbox);
        return titledPane;
    }

    private static TitledPane createRecipeItemsDetailPane(String heading, BigFraction amount, Collection<? extends Recipe.RecipeItem> recipeItems)
    {
        TitledPane titledPane = new TitledPane();

        titledPane.setText(heading);
        titledPane.setCollapsible(true);

        VBox lines = new VBox(3);
        lines.setPadding(new javafx.geometry.Insets(10));
        titledPane.setContent(lines);

        List<Recipe.RecipeItem> sortedRecipeItems = new ArrayList<>(recipeItems);
        sortedRecipeItems.sort(Comparator.comparing(recipeItem -> recipeItem.getItem().getName()));

        for (Recipe.RecipeItem ri : sortedRecipeItems){
            lines.getChildren().add(new Label(ri.getItem().getName() + ": " + BigDecimalUtils.normalize(ri.getItem().toDisplayAmount(ri.getAmount().getAmountPerMinuteFraction().multiply(amount)).toBigDecimal(4, RoundingMode.HALF_UP)) + " / min"));
        }

        return titledPane;
    }

    private static Region createRecipeNodeDetailPane(RecipeNodeData nodeData)
    {
        VBox vbox = new VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(10));

        {
            TitledPane titledPane = new TitledPane();
            vbox.getChildren().add(titledPane);

            titledPane.setText("Buildings");
            titledPane.setCollapsible(true);

            VBox container = new VBox(10);
            titledPane.setContent(container);
            container.setPadding(new javafx.geometry.Insets(10));

            VBox lines = new VBox(3);
            container.getChildren().add(lines);

            BigDecimal n = nodeData.getAmount().toBigDecimal(6, RoundingMode.HALF_UP);
            BigDecimal i = n.setScale(0, RoundingMode.DOWN);
            n = n.subtract(i);

            if (i.signum() != 0){
                lines.getChildren().add(new Label(i + " × " + nodeData.getRecipe().getProducedInBuilding().getName() + " at 100%"));
            }

            if (n.signum() != 0){
                final BigDecimal underclockedClockSpeedPercent = BigDecimalUtils.normalize(n.movePointRight(2));
                Label l = new Label("1 × " + nodeData.getRecipe().getProducedInBuilding().getName() + " at " + underclockedClockSpeedPercent + "%");
                lines.getChildren().add(l);

                l.onMouseClickedProperty().set(event -> {
                    ClipboardContent clipboardContent = new ClipboardContent();
                    clipboardContent.putString(underclockedClockSpeedPercent.toString());
                    Clipboard.getSystemClipboard().setContent(clipboardContent);
                });

                MenuButton menuButton = new MenuButton("Copy Underclocked");
                container.getChildren().add(menuButton);

                MenuItem clockSpeed = new MenuItem("Clock speed");
                menuButton.getItems().add(clockSpeed);
                clockSpeed.onActionProperty().set(event -> {
                    ClipboardContent clipboardContent = new ClipboardContent();
                    clipboardContent.putString(underclockedClockSpeedPercent.toString().concat("%"));
                    Clipboard.getSystemClipboard().setContent(clipboardContent);

                    Notifications.create()
                            .title("Clock speed copied")
                            .text("Clock speed copied to clipboard: " + underclockedClockSpeedPercent + "%")
                            .hideAfter(Duration.seconds(3))
                            .position(Pos.TOP_CENTER)
                            .show();
                });

                MenuItem itemsPerMin = new MenuItem("Output items / min");
                menuButton.getItems().add(itemsPerMin);
                itemsPerMin.onActionProperty().set(event -> {
                    String copyText = BigDecimalUtils.normalize(nodeData.getRecipe().getPrimaryProductAmount()
                            .getAmountPerMinuteFraction()
                            .multiply(nodeData.getAmount().subtract(BigFraction.valueOf(i)))
                            .toBigDecimal(4, RoundingMode.HALF_UP)
                    ).toString();
                    ClipboardContent clipboardContent = new ClipboardContent();
                    clipboardContent.putString(copyText);
                    Clipboard.getSystemClipboard().setContent(clipboardContent);

                    Notifications.create()
                            .title("Output items / min copied")
                            .text("Output items / min copies to clipboard: " + copyText)
                            .hideAfter(Duration.seconds(3))
                            .position(Pos.TOP_CENTER)
                            .show();
                });
            }
        }

        vbox.getChildren().add(createRecipeItemsDetailPane("Consumes", nodeData.getAmount(), nodeData.getRecipe().getIngredients()));
        vbox.getChildren().add(createRecipeItemsDetailPane("Produces", nodeData.getAmount(), nodeData.getRecipe().getProducts()));

        {
            Region grower = new Pane();
            grower.setPrefHeight(0);
            grower.setPrefWidth(0);
            VBox.setVgrow(grower, Priority.ALWAYS);
            vbox.getChildren().add(grower);
        }

        TitledPane titledPane = new TitledPane();
        titledPane.setCollapsible(false);
        titledPane.setAlignment(Pos.CENTER);
        titledPane.setMaxHeight(Double.MAX_VALUE);
        titledPane.getStyleClass().add("stpnr-tenbold");
        titledPane.setText(nodeData.getRecipe().getName());
        titledPane.setContent(vbox);
        return titledPane;
    }

    private static <N, E> void doGraphLayout(double width, double height, Map<Node<N, E>, Region> componentMap)
    {
        Map<Node<N, E>, org.eclipse.draw2d.graph.Node> layoutNodeMap = new TreeMap<>(Comparator.comparing(Node::getName));

        final DirectedGraph layoutGraph = new DirectedGraph();
        layoutGraph.setMargin(new Insets(20));
        layoutGraph.setDefaultPadding(new Insets(50));
        layoutGraph.setDirection(PositionConstants.SOUTH);

        for (var entry : componentMap.entrySet()){
            var n = entry.getKey();
            Region r = entry.getValue();

            org.eclipse.draw2d.graph.Node layoutNode = new org.eclipse.draw2d.graph.Node();
            layoutNode.width = (int)r.getWidth();
            layoutNode.height = (int)r.getHeight();
            layoutNodeMap.put(n, layoutNode);
            addNodeToLayoutGraph(layoutGraph, layoutNode);
        }

        for (Node<N, E> n : componentMap.keySet()){
            for (Node<N, E> c : n.getOutgoingEdges().keySet()){
                org.eclipse.draw2d.graph.Edge layoutEdge = new org.eclipse.draw2d.graph.Edge(layoutNodeMap.get(n), layoutNodeMap.get(c));
                addEdgeToLayoutGraph(layoutGraph, layoutEdge);
            }
        }

        new DirectedGraphLayout().visit(layoutGraph);

        double xOffset = 0;
        double yOffset = 0;

        if (layoutGraph.getLayoutSize().width < width){
            xOffset = (width - (double)layoutGraph.getLayoutSize().width) / 2.0;
        }

        if (layoutGraph.getLayoutSize().height < height){
            yOffset = (height - (double)layoutGraph.getLayoutSize().height) / 2.0;
        }

        for (var entry : componentMap.entrySet()){
            var key = entry.getKey();
            Region r = entry.getValue();

            org.eclipse.draw2d.graph.Node layoutNode = layoutNodeMap.get(key);

            r.layoutXProperty().set(layoutNode.x + xOffset);
            r.layoutYProperty().set(layoutNode.y + yOffset);
        }
    }

    static Point2D getRectangleEdge(double x, double y, double w, double h, double ox, double oy)
    {
        if (w <= 0.0 || h <= 0.0){
            return new Point2D(x, y);
        }

        double ydiff = oy - y;
        double xdiff = ox - x;

        if (ydiff == 0.0){
            double rx = x + ((w / 2.0) * Math.signum(xdiff));
            return new Point2D(rx, y);
        }else if (xdiff == 0.0){
            double ry = y + ((h / 2.0) * Math.signum(ydiff));
            return new Point2D(x, ry);
        }else{
            double angle = xdiff / ydiff;

            if (Math.abs(angle) < w / h){
                double ry = y + ((h / 2.0) * Math.signum(ydiff));
                double rx = x + (((ry - y) / ydiff) * xdiff);
                return new Point2D(rx, ry);
            }else{
                double rx = x + ((w / 2.0) * Math.signum(xdiff));
                double ry = y + (((rx - x) / xdiff) * ydiff);
                return new Point2D(rx, ry);
            }
        }
    }

    private static void setContent(Tab tab, AppContext appContext, ProductionPlan plan)
    {
        if (plan == null){
            tab.setContent(new Pane());
        }else{
            tab.setContent(createGraphPane(appContext, plan));
        }
    }

    private static class DragInfo
    {
        public double startSceneX;
        public double startSceneY;
        public boolean dragged = false;
    }

    private static class PaneState
    {
        public boolean hadAnyDragging = false;
    }
}
