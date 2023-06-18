/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx;

import io.github.elcheapogary.satisplanory.ui.jfx.context.AppContext;
import io.github.elcheapogary.satisplanory.ui.jfx.style.Style;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
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

    public static Node create(Application application, AppContext appContext)
            throws IOException
    {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(createTab("About", "AboutPane_Main.html", application, appContext));
        tabPane.getTabs().add(createTab("Credits", "AboutPane_Credits.html", application, appContext));
        tabPane.getTabs().add(createTab("License", "AboutPane_EPL-2.0.html", application, appContext));
        return tabPane;
    }

    private static Tab createTab(String title, String resourceName, Application application, AppContext appContext)
            throws IOException
    {
        Tab tab = new Tab(title);
        tab.setClosable(false);
        tab.setContent(createWebView(resourceName, application, appContext));
        return tab;
    }

    private static Node createWebView(String resourceName, Application application, AppContext appContext)
            throws IOException
    {
        String content;
        try (Reader r = new BufferedReader(new InputStreamReader(Optional.ofNullable(AboutPane.class.getResourceAsStream(resourceName)).orElseThrow(() -> new IOException("Missing resource: " + resourceName)), StandardCharsets.UTF_8))){
            content = IOUtils.toString(r);
        }
        WebView webView = new WebView();
        Style.configureWebView(appContext, webView);
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
