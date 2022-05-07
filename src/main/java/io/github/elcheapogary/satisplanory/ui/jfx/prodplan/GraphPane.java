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
import io.github.elcheapogary.satisplanory.prodplan.ProductionPlan;
import io.github.elcheapogary.satisplanory.prodplan.graph.data.InputItemNodeData;
import io.github.elcheapogary.satisplanory.prodplan.graph.data.OutputItemNodeData;
import io.github.elcheapogary.satisplanory.prodplan.graph.data.ProdPlanEdgeData;
import io.github.elcheapogary.satisplanory.prodplan.graph.data.ProdPlanNodeData;
import io.github.elcheapogary.satisplanory.prodplan.graph.data.RecipeNodeData;
import io.github.elcheapogary.satisplanory.prodplan.graph.lib.Edge;
import io.github.elcheapogary.satisplanory.prodplan.graph.lib.Graph;
import io.github.elcheapogary.satisplanory.prodplan.graph.lib.Node;
import io.github.elcheapogary.satisplanory.util.BigDecimalUtils;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.math.RoundingMode;
import java.util.Comparator;
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
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurve;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.graph.DirectedGraph;
import org.eclipse.draw2d.graph.DirectedGraphLayout;

class GraphPane
{
    private GraphPane()
    {
    }

    private static Region createNodeComponent(ProdPlanNodeData nodeData, BooleanBinding selectedBinding)
    {
        final Color color;
        String line1;
        String line2;
        if (nodeData instanceof RecipeNodeData recipeNodeData){
            color = Color.valueOf("#5F96F5");
            line1 = recipeNodeData.getRecipe().getName();
            line2 = BigDecimalUtils.normalize(recipeNodeData.getAmount().toBigDecimal(4, RoundingMode.HALF_UP)) + " " + recipeNodeData.getRecipe().getProducedInBuilding().getName();
        }else if (nodeData instanceof InputItemNodeData inputItemNodeData){
            color = Color.valueOf("#FA975F");
            line1 = "Input: " + inputItemNodeData.getItem().getName();
            line2 = BigDecimalUtils.normalize(inputItemNodeData.getItem().toDisplayAmount(inputItemNodeData.getAmount()).toBigDecimal(4, RoundingMode.HALF_UP)) + " / min";
        }else if (nodeData instanceof OutputItemNodeData outputItemNodeData){
            color = Color.valueOf("#4BDE7C");
            line1 = "Output: " + outputItemNodeData.getItem().getName();
            line2 = BigDecimalUtils.normalize(outputItemNodeData.getItem().toDisplayAmount(outputItemNodeData.getAmount()).toBigDecimal(4, RoundingMode.HALF_UP)) + " / min";
        }else{
            throw new AssertionError();
        }

        VBox vbox = new VBox();
        vbox.setFillWidth(true);
        vbox.setPadding(new javafx.geometry.Insets(10));

        vbox.backgroundProperty().bind(Bindings.createObjectBinding(() -> {
            if (selectedBinding.get()){
                return Background.fill(color.brighter().brighter().desaturate());
            }else{
                return Background.fill(color);
            }
        }, selectedBinding));

        Label l1 = new Label(line1);
        l1.setMaxWidth(Double.MAX_VALUE);
        l1.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, Font.getDefault().getSize()));
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
        hbox.setBackground(Background.fill(Color.WHITE));
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

    public static Pane createGraphPane(ProductionPlan plan)
    {
        return createGraphPane(plan.toGraph(), GraphPane::createNodeComponent, GraphPane::createEdgeComponent);
    }

    private static <N, E> Pane createGraphPane(Graph<N, E> graph, BiFunction<? super N, ? super BooleanBinding, ? extends Region> nodeComponentFactory, BiFunction<? super E, ? super BooleanBinding, ? extends Region> edgeComponentFactory)
    {
        Map<Node<N, E>, Region> nodeComponentMap = new TreeMap<>(Comparator.comparing(Node::getName));

        final ObjectProperty<Node<N, E>> selectedNodeProperty = new SimpleObjectProperty<>();

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
            layoutNode.width = (int) r.getWidth();
            layoutNode.height = (int) r.getHeight();
            layoutNodeMap.put(n, layoutNode);
            layoutGraph.nodes.add(layoutNode);
        }

        for (Node<N, E> n : componentMap.keySet()){
            for (Node<N, E> c : n.getOutgoingEdges().keySet()){
                org.eclipse.draw2d.graph.Edge layoutEdge = new org.eclipse.draw2d.graph.Edge(layoutNodeMap.get(n), layoutNodeMap.get(c));
                layoutGraph.edges.add(layoutEdge);
            }
        }

        new DirectedGraphLayout().visit(layoutGraph);

        double xOffset = 0;
        double yOffset = 0;

        if (layoutGraph.getLayoutSize().width < width){
            xOffset = (width - (double) layoutGraph.getLayoutSize().width) / 2.0;
        }

        if (layoutGraph.getLayoutSize().height < height){
            yOffset = (height - (double) layoutGraph.getLayoutSize().height) / 2.0;
        }

        for (var entry : componentMap.entrySet()){
            var key = entry.getKey();
            Region r = entry.getValue();

            org.eclipse.draw2d.graph.Node layoutNode = layoutNodeMap.get(key);

            r.layoutXProperty().set(layoutNode.x + xOffset);
            r.layoutYProperty().set(layoutNode.y + yOffset);
        }
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
