/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.observe;

import java.util.Objects;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.Node;

public class NodeLifeCycle<T extends Node>
{
    private T node;

    private NodeLifeCycle(T node)
    {
        this.node = node;
    }

    public static <T extends Node> NodeLifeCycle<T> of(T node)
    {
        return new NodeLifeCycle<>(Objects.requireNonNull(node));
    }

    public <T> void addChangeListener(ObservableValue<? extends T> observableValue, ChangeListener<? super T> changeListener)
    {
        node.disableProperty().addListener(new NodeAnchor(changeListener));
        observableValue.addListener(new WeakChangeListener<>(changeListener));
        changeListener.changed(observableValue, observableValue.getValue(), observableValue.getValue());
    }

    private static class NodeAnchor
            implements ChangeListener<Boolean>
    {
        private final ChangeListener<?> referencedChangeListener;

        public NodeAnchor(ChangeListener<?> referencedChangeListener)
        {
            this.referencedChangeListener = referencedChangeListener;
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            // do nothing
        }
    }
}
