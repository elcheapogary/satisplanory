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

import io.github.elcheapogary.satisplanory.util.ResourceUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.web.WebView;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

public class AboutPane
{
    private AboutPane()
    {
    }

    public static Node create(Application application)
            throws IOException
    {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(createTab("About", "AboutPane_Main.html", application));
        tabPane.getTabs().add(createTab("Credits", "AboutPane_Credits.html", application));
        tabPane.getTabs().add(createTab("License", "AboutPane_EPL-2.0.html", application));
        return tabPane;
    }

    private static Tab createTab(String title, String resourceName, Application application)
            throws IOException
    {
        Tab tab = new Tab(title);
        tab.setClosable(false);
        tab.setContent(createWebView(resourceName, application));
        return tab;
    }

    private static Node createWebView(String resourceName, Application application)
            throws IOException
    {
        String content = ResourceUtils.getResourceAsString(AboutPane.class, resourceName);
        WebView webView = new WebView();
        webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED){
                NodeList nl = webView.getEngine().getDocument().getElementsByTagName("a");
                for (int i = 0; i < nl.getLength(); i++){
                    org.w3c.dom.Node node = nl.item(i);
                    if ((node instanceof EventTarget eventTarget) && (node instanceof HTMLAnchorElement htmlAnchorElement)){
                        eventTarget.addEventListener("click", evt -> {
                            String href = htmlAnchorElement.getHref();
                            if (href != null){
                                application.getHostServices().showDocument(href);
                            }
                            evt.preventDefault();
                        }, false);
                    }
                }
            }
        });
        webView.getEngine().loadContent(content);
        return webView;
//        TextArea textArea = new TextArea(content);
//        textArea.setFont(Font.font("Monospaced", Font.getDefault().getSize()));
//        textArea.setEditable(false);
//        textArea.setWrapText(true);
//        return textArea;
    }
}
