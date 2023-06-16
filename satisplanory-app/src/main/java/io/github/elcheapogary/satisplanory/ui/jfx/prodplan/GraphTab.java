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

import io.github.elcheapogary.satisplanory.graphlayout.draw2d.PositionConstants;
import io.github.elcheapogary.satisplanory.graphlayout.draw2d.geometry.Insets;
import io.github.elcheapogary.satisplanory.graphlayout.draw2d.graph.DirectedGraph;
import io.github.elcheapogary.satisplanory.graphlayout.draw2d.graph.DirectedGraphLayout;
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
import io.github.elcheapogary.satisplanory.ui.jfx.style.Style;
import io.github.elcheapogary.satisplanory.util.BigDecimalUtils;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import io.github.elcheapogary.satisplanory.util.FileNameUtils;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import org.controlsfx.control.Notifications;

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
    private static void addEdgeToLayoutGraph(DirectedGraph layoutGraph, io.github.elcheapogary.satisplanory.graphlayout.draw2d.graph.Edge edge)
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
    private static void addNodeToLayoutGraph(DirectedGraph layoutGraph, io.github.elcheapogary.satisplanory.graphlayout.draw2d.graph.Node node)
    {
        layoutGraph.nodes.add(node);
    }

    private static Dialog<Double> createScaleSelectionDialog(AppContext appContext, Pane graphPage)
    {
        double w = graphPage.getWidth();
        double h = graphPage.getHeight();

        Dialog<Double> dialog = new Dialog<>();
        dialog.getDialogPane().getStylesheets().addAll(Style.getStyleSheets(appContext));
        dialog.setTitle("Select image scale");
        dialog.setHeaderText("Select the scale of the image");
        dialog.getDialogPane().setMinWidth(300);

        VBox vbox = new VBox(10);
        dialog.getDialogPane().setContent(vbox);

        Slider slider = new Slider();
        slider.setMin(1);
        slider.setMax(20);
        slider.setValue(10);
        slider.setMajorTickUnit(1);
        slider.setShowTickMarks(true);
        slider.setSnapToTicks(true);

        DoubleBinding scaleBinding = Bindings.createDoubleBinding(() -> {
            double v = slider.getValue();

            if (v < 10.0){
                v /= 10.0;
            }else if (v > 10.0){
                v = (((v - 10.0) / 10.0) * 4.0) + 1.0;
            }else{
                v = 1;
            }

            return v;
        }, slider.valueProperty());

        StringBinding labelTextBinding = Bindings.createStringBinding(() -> {
            double v = scaleBinding.get();

            return "Scale: " + ((int)(v * 100.0)) + "%, " + ((int)Math.ceil(w * v)) + "x" + ((int)Math.ceil(h * v));
        }, scaleBinding);

        Label l = new Label();
        l.textProperty().bind(labelTextBinding);

        vbox.getChildren().add(l);
        vbox.getChildren().add(slider);

        dialog.setResultConverter(param -> {
            if (param == ButtonType.OK){
                return scaleBinding.get();
            }else{
                return null;
            }
        });

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        return dialog;
    }

    private static void configureGraphContextMenu(AppContext appContext, ScrollPane scrollPane, Pane graphPane, Graph<ProdPlanNodeData, ProdPlanEdgeData> graph, Supplier<String> planNameSupplier)
    {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setAutoHide(true);

        {
            MenuItem menuItem = new MenuItem("Copy image");
            contextMenu.getItems().add(menuItem);

            menuItem.onActionProperty().set(event -> {
                createScaleSelectionDialog(appContext, graphPane).showAndWait().ifPresent(scale -> {
                    SnapshotParameters parameters = new SnapshotParameters();
                    parameters.setFill(Color.TRANSPARENT);

                    WritableImage img = graphPane.snapshot(parameters, null);

                    Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.IMAGE, img));
                });
            });
        }

        {
            MenuItem menuItem = new MenuItem("Export image");
            contextMenu.getItems().add(menuItem);

            menuItem.onActionProperty().set(event -> {
                createScaleSelectionDialog(appContext, graphPane).showAndWait().ifPresent(scale -> {
                    FileChooser fc = new FileChooser();
                    if (appContext.getPersistentData().getPreferences().getLastImportExportDirectory() != null && appContext.getPersistentData().getPreferences().getLastImportExportDirectory().isDirectory()){
                        fc.setInitialDirectory(appContext.getPersistentData().getPreferences().getLastImportExportDirectory());
                    }
                    fc.setTitle("Select output file");
                    fc.setInitialFileName(FileNameUtils.removeUnsafeFilenameCharacters(planNameSupplier.get()) + ".png");
                    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Files", "*.png"));
                    File f = fc.showSaveDialog(graphPane.getScene().getWindow());
                    if (f != null){
                        appContext.getPersistentData().getPreferences().setLastImportExportDirectory(f.getAbsoluteFile().getParentFile());

                        SnapshotParameters parameters = new SnapshotParameters();
                        parameters.setFill(Color.TRANSPARENT);
                        parameters.setTransform(Transform.scale(scale / graphPane.getScaleX(), scale / graphPane.getScaleY()));

                        WritableImage img = graphPane.snapshot(parameters, null);

                        new TaskProgressDialog(appContext)
                                .setTitle("Saving image")
                                .setContentText("Saving image")
                                .runTask(taskContext -> {
                                    try{
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
                fc.setInitialFileName(FileNameUtils.removeUnsafeFilenameCharacters(planNameSupplier.get()) + ".graphml");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("GraphML Files", "*.graphml"));
                File f = fc.showSaveDialog(graphPane.getScene().getWindow());
                if (f != null){
                    appContext.getPersistentData().getPreferences().setLastImportExportDirectory(f.getAbsoluteFile().getParentFile());

                    new TaskProgressDialog(appContext)
                            .setTitle("Exporting GraphML")
                            .setContentText("Exporting GraphML")
                            .runTask(taskContext -> {
                                try{
                                    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(f))){
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

        EventHandler<MouseEvent> hideContextMenuEventFilter = event -> {
            if (event.getButton() == MouseButton.PRIMARY && contextMenu.isShowing()){
                Platform.runLater(contextMenu::hide);
                event.consume();
            }
        };

        contextMenuParent.addEventFilter(MouseEvent.MOUSE_PRESSED, hideContextMenuEventFilter);
        contextMenuParent.addEventFilter(MouseEvent.MOUSE_DRAGGED, hideContextMenuEventFilter);
        contextMenuParent.addEventFilter(MouseEvent.MOUSE_CLICKED, hideContextMenuEventFilter);
    }

    private static <N, E> void configureNodeComponentMouseEvents(Node<N, E> node, Region region, Pane parent, ObjectProperty<? super Node<N, E>> selectedNodeProperty, PaneState paneState, Collection<? extends Region> allNodes)
    {
        final DragInfo dragInfo = new DragInfo();

        region.onMouseClickedProperty().set(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.isStillSincePress()){
                selectedNodeProperty.set(node);
                event.consume();
            }
        });
        region.onMousePressedProperty().set(event -> {
            if (event.getButton() == MouseButton.PRIMARY){
                dragInfo.startSceneX = event.getSceneX();
                dragInfo.startSceneY = event.getSceneY();
                dragInfo.dragged = false;
                event.consume();
            }
        });
        region.onMouseDraggedProperty().set(event -> {
            if (event.getButton() == MouseButton.PRIMARY && !event.isStillSincePress()){
                dragInfo.dragged = true;
                paneState.hadAnyDragging = true;
                region.translateXProperty().set((event.getSceneX() - dragInfo.startSceneX) / parent.getScaleX());
                region.translateYProperty().set((event.getSceneY() - dragInfo.startSceneY) / parent.getScaleY());
            }
            event.consume();
        });
        region.onMouseDragReleasedProperty().set(event -> {
            updateNodeLayoutPosition(region, allNodes);
            event.consume();
        });
        region.onMouseReleasedProperty().set(event -> {
            if (dragInfo.dragged){
                updateNodeLayoutPosition(region, allNodes);
            }
            event.consume();
        });
    }

    private static void updateNodeLayoutPosition(Region region, Collection<? extends Region> allNodes)
    {
        double x = region.layoutXProperty().doubleValue() + region.translateXProperty().doubleValue();
        double y = region.layoutYProperty().doubleValue() + region.translateYProperty().doubleValue();

        double xAdjust = 20 - x;
        double yAdjust = 20 - y;

        for (Region other : allNodes){
            if (other != region){
                xAdjust = Math.max(xAdjust, 20 - other.layoutXProperty().get());
                yAdjust = Math.max(yAdjust, 20 - other.layoutYProperty().get());
            }
        }

        region.layoutXProperty().set(x + xAdjust);
        region.layoutYProperty().set(y + yAdjust);
        region.translateXProperty().set(0);
        region.translateYProperty().set(0);

        for (Region other : allNodes){
            if (other != region){
                other.layoutXProperty().set(other.layoutXProperty().get() + xAdjust);
                other.layoutYProperty().set(other.layoutYProperty().get() + yAdjust);
            }
        }
    }

    public static Tab create(AppContext appContext, ProdPlanModel model)
    {
        Tab tab = new Tab();
        tab.setClosable(false);
        tab.setText("Graph");

        setContent(tab, appContext, model);

        model.planProperty().addListener((observable, oldValue, newValue) -> setContent(tab, appContext, model));

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
            sb.append(item.toNormalizedDisplayAmountString(amount));
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

    private static TitledPane createEmptyRightPane()
    {
        TitledPane p = createRightTitledPane();
        p.setText("No item selected");
        p.setPrefWidth(250);

        VBox vbox = new VBox(10);
        p.setContent(vbox);

        for (String s : new String[]{
                "Click on one the items in the graph to the left for more details.",
                "You can drag nodes to re-organize them, zoom with the scroll sheel etc."
        }){
            Label l = new Label(s);
            l.setWrapText(true);
            vbox.getChildren().add(l);
        }

        return p;
    }

    private static Region createGraphPane(AppContext appContext, ProductionPlan plan, Supplier<String> planNameSupplier)
    {
        BorderPane bp = new BorderPane();
        ObjectProperty<Node<ProdPlanNodeData, ProdPlanEdgeData>> selectedNodeProperty = new SimpleObjectProperty<>();

        BorderPane rightContainer = new BorderPane();
        rightContainer.setMinWidth(250);
        bp.setRight(rightContainer);
        rightContainer.setCenter(createEmptyRightPane());

        selectedNodeProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue == null){
                rightContainer.setCenter(createEmptyRightPane());
            }else{
                rightContainer.setCenter(createNodeDetailPane(newValue.getData()));
            }
        });

        var graph = plan.toGraph();

        Pane pane = createGraphPane(graph, GraphTab::createNodeComponent, GraphTab::createEdgeComponent, selectedNodeProperty);

        ScrollPane sp = ZoomableScrollPane.create(pane, 0.1, 5.0);
        sp.setPannable(true);
        bp.setCenter(sp);

        sp.onMouseClickedProperty().set(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.isStillSincePress()){
                selectedNodeProperty.set(null);
                event.consume();
            }
        });

        configureGraphContextMenu(appContext, sp, pane, graph, planNameSupplier);

        return bp;
    }

    private static <N, E> Pane createGraphPane(Graph<N, E> graph, BiFunction<? super N, ? super BooleanBinding, ? extends Region> nodeComponentFactory, BiFunction<? super E, ? super BooleanBinding, ? extends Region> edgeComponentFactory, ObjectProperty<Node<N, E>> selectedNodeProperty)
    {
        Map<Node<N, E>, Region> nodeComponentMap = new HashMap<>();

        final PaneState paneState = new PaneState();

        final Pane pane = new Pane()
        {
            @Override
            protected void layoutChildren()
            {
                super.layoutChildren();
                if (!paneState.hadAnyDragging){
                    double w = getWidth() - (getInsets().getLeft() + getInsets().getRight());
                    double h = getHeight() - (getInsets().getTop() + getInsets().getBottom());
                    Platform.runLater(() -> doGraphLayout(w, h, nodeComponentMap));
                }
            }
        };

        /*
         * Set background color to it comes through in exported image
         */
        pane.setStyle("-fx-background-color: -fx-background;");
        pane.setPadding(new javafx.geometry.Insets(20));

        for (Node<N, E> n : graph.getNodes()){
            Region component = nodeComponentFactory.apply(n.getData(), Bindings.createBooleanBinding(() -> selectedNodeProperty.getValue() == n, selectedNodeProperty));
            nodeComponentMap.put(n, component);
        }

        for (var entry : nodeComponentMap.entrySet()){
            var n = entry.getKey();
            Region component = entry.getValue();
            configureNodeComponentMouseEvents(n, component, pane, selectedNodeProperty, paneState, nodeComponentMap.values());
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

        vbox.getChildren().add(new Label("Amount: " + nodeData.getItem().toNormalizedDisplayAmountString(nodeData.getAmount()) + " / min"));

        TitledPane titledPane = createRightTitledPane();
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
            line2 = inputItemNodeData.getItem().toNormalizedDisplayAmountString(inputItemNodeData.getAmount()) + " / min";
        }else if (nodeData instanceof OutputItemNodeData outputItemNodeData){
            vbox.getStyleClass().add("stpnr-graph-node-output");
            line1 = "Output: " + outputItemNodeData.getItem().getName();
            line2 = outputItemNodeData.getItem().toNormalizedDisplayAmountString(outputItemNodeData.getAmount()) + " / min";
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

        hbox.onContextMenuRequestedProperty().set(Event::consume);

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

        vbox.getChildren().add(new Label("Amount: " + nodeData.getItem().toNormalizedDisplayAmountString(nodeData.getAmount()) + " / min"));

        TitledPane titledPane = createRightTitledPane();
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
            lines.getChildren().add(new Label(ri.getItem().getName() + ": " + ri.getItem().toNormalizedDisplayAmountString(ri.getAmount().getAmountPerMinute().multiply(amount)) + " / min"));
        }

        return titledPane;
    }

    private static void addMachines(boolean even, double maxClockSpeed, String buildingName, BigFraction amount, VBox lines, Property<BigFraction> copyClockSpeedProperty)
    {
        lines.getChildren().clear();

        BigDecimal maxClockSpeedDecimal = BigDecimal.valueOf(maxClockSpeed);

        BigFraction nBuildings = amount.divide(BigFraction.valueOf(maxClockSpeedDecimal).divide(100));

        if (even){
            if (!nBuildings.isInteger()){
                nBuildings = BigFraction.valueOf(nBuildings.toBigInteger().add(BigInteger.ONE));
            }
            BigFraction clockSpeed = amount.divide(nBuildings).multiply(100);
            copyClockSpeedProperty.setValue(clockSpeed);

            lines.getChildren().add(new Label("" + nBuildings + " x " + buildingName + " @ " + BigDecimalUtils.normalize(clockSpeed.toBigDecimal(4, RoundingMode.HALF_UP)) + "%"));
        }else{
            BigInteger intBuildings = nBuildings.toBigInteger();

            if (intBuildings.signum() > 0){
                lines.getChildren().add(new Label("" + intBuildings + " x " + buildingName + " @ " + BigDecimalUtils.normalize(maxClockSpeedDecimal.setScale(4, RoundingMode.HALF_UP)) + "%"));
            }

            BigFraction remainder = amount.subtract(BigFraction.valueOf(maxClockSpeedDecimal).divide(100).multiply(intBuildings)).multiply(100);
            copyClockSpeedProperty.setValue(remainder);

            if (remainder.signum() > 0){
                lines.getChildren().add(new Label("1 x " + buildingName + " @ " + BigDecimalUtils.normalize(remainder.toBigDecimal(4, RoundingMode.HALF_UP)) + "%"));
            }
        }
    }

    private static Region createRecipeNodeDetailPane(RecipeNodeData nodeData)
    {
        VBox vbox = new VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(10));

        {
            BooleanProperty evenClockSpeedProperty = new SimpleBooleanProperty(true);
            IntegerProperty maxClockSpeedProperty = new SimpleIntegerProperty(100);
            Property<BigFraction> copyClockSpeedProperty = new SimpleObjectProperty<>();

            TitledPane titledPane = new TitledPane();
            vbox.getChildren().add(titledPane);

            titledPane.setText("Clock speed");
            titledPane.setCollapsible(true);

            VBox container = new VBox(10);
            titledPane.setContent(container);
            container.setPadding(new javafx.geometry.Insets(10));

            VBox lines = new VBox(3);
            container.getChildren().add(lines);

            {
                CheckBox checkbox = new CheckBox();
                checkbox.setText("Even clock speeds");
                checkbox.setSelected(true);
                container.getChildren().add(checkbox);
                evenClockSpeedProperty.bind(checkbox.selectedProperty());
            }

            {
                Slider slider = new Slider();
                slider.setMin(0);
                slider.setMax(250);
                slider.setValue(100);
                slider.setMajorTickUnit(50);
                slider.setMinorTickCount(10);
                slider.setShowTickMarks(true);
                slider.setSnapToTicks(true);
                slider.setShowTickLabels(true);
                slider.setSnapToPixel(false);

                slider.valueProperty().addListener((observable, oldValue, newValue) -> {
                    slider.setValue(BigDecimal.valueOf(newValue.doubleValue())
                            .divide(BigDecimal.valueOf(5), 0, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(5))
                            .doubleValue()
                    );
                });

                maxClockSpeedProperty.bind(Bindings.createIntegerBinding(() -> Math.max(1, (int)slider.valueProperty().get()), slider.valueProperty()));

                VBox v = new VBox(5);

                StringBinding labelBinding = Bindings.createStringBinding(() -> "Max clock speed: " + maxClockSpeedProperty.get() + "%", maxClockSpeedProperty);
                Label label = new Label();
                label.textProperty().bind(labelBinding);

                v.getChildren().add(label);
                v.getChildren().add(slider);

                container.getChildren().add(v);
            }

            addMachines(evenClockSpeedProperty.get(), maxClockSpeedProperty.get(), nodeData.getRecipe().getProducedInBuilding().getName(), nodeData.getAmount(), lines, copyClockSpeedProperty);
            maxClockSpeedProperty.addListener(observable -> {
                addMachines(evenClockSpeedProperty.get(), maxClockSpeedProperty.get(), nodeData.getRecipe().getProducedInBuilding().getName(), nodeData.getAmount(), lines, copyClockSpeedProperty);
            });
            evenClockSpeedProperty.addListener(observable -> {
                addMachines(evenClockSpeedProperty.get(), maxClockSpeedProperty.get(), nodeData.getRecipe().getProducedInBuilding().getName(), nodeData.getAmount(), lines, copyClockSpeedProperty);
            });

            MenuButton menuButton = new MenuButton("Copy");
            container.getChildren().add(menuButton);
            menuButton.managedProperty().bind(Bindings.createBooleanBinding(() -> copyClockSpeedProperty.getValue() != null, copyClockSpeedProperty));

            MenuItem clockSpeed = new MenuItem("Clock speed");
            menuButton.getItems().add(clockSpeed);
            clockSpeed.onActionProperty().set(event -> {
                ClipboardContent clipboardContent = new ClipboardContent();
                String copyText = BigDecimalUtils.normalize(copyClockSpeedProperty.getValue().toBigDecimal(4, RoundingMode.HALF_UP)).toString().concat("%");
                clipboardContent.putString(copyText);
                Clipboard.getSystemClipboard().setContent(clipboardContent);

                Notifications.create()
                        .title("Clock speed copied")
                        .text("Clock speed copied to clipboard: " + copyText)
                        .hideAfter(Duration.seconds(3))
                        .position(Pos.TOP_CENTER)
                        .show();
            });

            MenuItem clockspeedFraction = new MenuItem("Clock speed as fraction");
            menuButton.getItems().add(clockspeedFraction);
            clockspeedFraction.onActionProperty().set(event -> {
                ClipboardContent clipboardContent = new ClipboardContent();
                String copyText = copyClockSpeedProperty.getValue().toString();
                clipboardContent.putString(copyText);
                Clipboard.getSystemClipboard().setContent(clipboardContent);

                Notifications.create()
                        .title("Clock speed copied")
                        .text("Clock speed copied to clipboard: " + copyText)
                        .hideAfter(Duration.seconds(3))
                        .position(Pos.TOP_CENTER)
                        .show();
            });

            MenuItem itemsPerMin = new MenuItem("Target production rate");
            menuButton.getItems().add(itemsPerMin);
            itemsPerMin.onActionProperty().set(event -> {
                String copyText = BigDecimalUtils.normalize(
                        nodeData.getRecipe().getPrimaryProduct().toDisplayAmountFraction(
                                nodeData.getRecipe().getPrimaryProductAmount()
                                        .getAmountPerMinute()
                                        .multiply(copyClockSpeedProperty.getValue().divide(100))
                        ).toBigDecimal(4, RoundingMode.HALF_UP)
                ).toString();
                ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.putString(copyText);
                Clipboard.getSystemClipboard().setContent(clipboardContent);

                Notifications.create()
                        .title("Target production rate copied")
                        .text("Target production rate copied to clipboard: " + copyText)
                        .hideAfter(Duration.seconds(3))
                        .position(Pos.TOP_CENTER)
                        .show();
            });

            MenuItem itemsPerMinFraction = new MenuItem("Target production rate as fraction");
            menuButton.getItems().add(itemsPerMinFraction);
            itemsPerMinFraction.onActionProperty().set(event -> {
                String copyText = nodeData.getRecipe().getPrimaryProduct().toDisplayAmountFraction(
                        nodeData.getRecipe().getPrimaryProductAmount()
                                .getAmountPerMinute()
                                .multiply(copyClockSpeedProperty.getValue().divide(100))
                ).toString();
                ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.putString(copyText);
                Clipboard.getSystemClipboard().setContent(clipboardContent);

                Notifications.create()
                        .title("Target production rate copied")
                        .text("Target production rate copied to clipboard: " + copyText)
                        .hideAfter(Duration.seconds(3))
                        .position(Pos.TOP_CENTER)
                        .show();
            });
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

        TitledPane titledPane = createRightTitledPane();
        titledPane.setText(nodeData.getRecipe().getName());
        titledPane.setContent(vbox);
        return titledPane;
    }

    private static TitledPane createRightTitledPane()
    {
        TitledPane titledPane = new TitledPane();
        titledPane.setCollapsible(false);
        titledPane.setAlignment(Pos.CENTER);
        titledPane.setMaxHeight(Double.MAX_VALUE);
        titledPane.getStyleClass().add("stpnr-tenbold");
        return titledPane;
    }

    private static <N, E> void doGraphLayout(double width, double height, Map<Node<N, E>, Region> componentMap)
    {
        Map<Node<N, E>, io.github.elcheapogary.satisplanory.graphlayout.draw2d.graph.Node> layoutNodeMap = new HashMap<>();

        final DirectedGraph layoutGraph = new DirectedGraph();
        layoutGraph.setDefaultPadding(new Insets(50));
        layoutGraph.setDirection(PositionConstants.SOUTH);

        for (var entry : componentMap.entrySet()){
            var n = entry.getKey();
            Region r = entry.getValue();

            io.github.elcheapogary.satisplanory.graphlayout.draw2d.graph.Node layoutNode = new io.github.elcheapogary.satisplanory.graphlayout.draw2d.graph.Node();
            layoutNode.width = (int)r.getWidth();
            layoutNode.height = (int)r.getHeight();
            layoutNodeMap.put(n, layoutNode);
            addNodeToLayoutGraph(layoutGraph, layoutNode);
        }

        for (Node<N, E> n : componentMap.keySet()){
            for (Node<N, E> c : n.getOutgoingEdges().keySet()){
                io.github.elcheapogary.satisplanory.graphlayout.draw2d.graph.Edge layoutEdge = new io.github.elcheapogary.satisplanory.graphlayout.draw2d.graph.Edge(layoutNodeMap.get(n), layoutNodeMap.get(c));
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

            io.github.elcheapogary.satisplanory.graphlayout.draw2d.graph.Node layoutNode = layoutNodeMap.get(key);

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

    private static void setContent(Tab tab, AppContext appContext, ProdPlanModel model)
    {
        if (model.getPlan() == null){
            tab.setContent(new Pane());
        }else{
            tab.setContent(createGraphPane(appContext, model.getPlan(), model::getName));
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
