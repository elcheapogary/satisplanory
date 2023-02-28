/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.style;

import io.github.elcheapogary.satisplanory.ui.jfx.context.AppContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;

public class Style
{
    private Style()
    {
    }

    public static void init()
    {
        try {
            loadFont("fonts/Roboto/Roboto-Regular.ttf");
            loadFont("fonts/Roboto/Roboto-Bold.ttf");
            loadFont("fonts/Roboto/Roboto-Italic.ttf");
            loadFont("fonts/Roboto/Roboto-BoldItalic.ttf");
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    private static Font loadFont(String resourceName)
            throws IOException
    {
        try (InputStream in = Style.class.getResourceAsStream(resourceName)) {
            if (in == null){
                throw new IOException("Missing resource: " + Style.class.getName() + ": " + resourceName);
            }
            return Font.loadFont(in, 12);
        }
    }

    public static String getDarkModeStylesheet()
    {
        return Style.class.getResource("darkmode.css").toString();
    }

    public static String getCustomStylesheet()
    {
        return Style.class.getResource("custom.css").toString();
    }

    public static List<? extends String> getStyleSheets(AppContext appContext)
    {
        if (appContext.getPersistentData().getPreferences().getUiPreferences().darkModeEnabledProperty().get()){
            return Arrays.asList(getCustomStylesheet(), getDarkModeStylesheet());
        }else{
            return Collections.singletonList(getCustomStylesheet());
        }
    }

    public static void configureWebView(AppContext appContext, WebView webView)
    {
        BooleanProperty darkMode = appContext.getPersistentData().getPreferences().getUiPreferences().darkModeEnabledProperty();

        webView.getEngine().userStyleSheetLocationProperty().bind(Bindings.createStringBinding(() -> {
            if (darkMode.get()){
                return Style.class.getResource("htmldark.css").toString();
            }else{
                return Style.class.getResource("html.css").toString();
            }
        }, darkMode));
    }
}
