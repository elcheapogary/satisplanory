/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

module io.github.elcheapogary.satisplanory
{
    requires io.github.elcheapogary.satisplanory.gamedata;
    requires io.github.elcheapogary.satisplanory.graphlayout;
    requires java.json;
    requires java.net.http;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.swing;
    requires javafx.web;
    requires jdk.xml.dom;
    requires org.controlsfx.controls;

    exports io.github.elcheapogary.satisplanory.ui.jfx.app;
}