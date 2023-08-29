/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package io.github.elcheapogary.satisplanory.ui.jfx.component;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;

public class ZoomableScrollPane
{
    private ZoomableScrollPane()
    {
    }

    public static ScrollPane create(Node target, double maxScale)
    {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setPannable(true);

        Group zoomNode = new Group(target);
        VBox outer = new VBox(zoomNode);
        outer.setAlignment(Pos.CENTER);

        sp.setContent(outer);

        outer.setOnScroll(scrollEvent -> zoom(sp, zoomNode, target, scrollEvent, maxScale));

        return sp;
    }

    private static void zoom(ScrollPane sp, Node zoomNode, Node target, ScrollEvent scrollEvent, double maxScale)
    {
        scrollEvent.consume();
        double zoomFactor = Math.exp(scrollEvent.getDeltaY() * 0.005);

        Bounds innerBounds = zoomNode.getLayoutBounds();
        Bounds viewportBounds = sp.getViewportBounds();

        double minScale = Math.min(viewportBounds.getWidth() / target.getLayoutBounds().getWidth(), viewportBounds.getHeight() / target.getLayoutBounds().getHeight());

        double valX = sp.getHvalue() * (innerBounds.getWidth() - viewportBounds.getWidth());
        double valY = sp.getVvalue() * (innerBounds.getHeight() - viewportBounds.getHeight());

        double scale = target.getScaleX() * zoomFactor;

        scale = Double.min(maxScale, Double.max(minScale, scale));

        if (scale == target.getScaleX()){
            return;
        }

        zoomFactor = scale / target.getScaleX();

        target.setScaleX(scale);
        target.setScaleY(scale);
        sp.layout();

        Point2D posInZoomTarget = target.parentToLocal(zoomNode.parentToLocal(new Point2D(scrollEvent.getX(), scrollEvent.getY())));

        Point2D adjustment = target.getLocalToParentTransform().deltaTransform(posInZoomTarget.multiply(zoomFactor - 1));

        Bounds updatedInnerBounds = zoomNode.getBoundsInLocal();
        sp.setHvalue((valX + adjustment.getX()) / (updatedInnerBounds.getWidth() - viewportBounds.getWidth()));
        sp.setVvalue((valY + adjustment.getY()) / (updatedInnerBounds.getHeight() - viewportBounds.getHeight()));
    }
}