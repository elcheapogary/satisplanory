/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

open module io.github.elcheapogary.satisplanory
{
    requires draw2d;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.web;
    requires jdk.xml.dom;
    requires org.apache.commons.io;
    requires org.controlsfx.controls;
    requires org.json;

    exports io.github.elcheapogary.satisplanory.ui.jfx.app;
}