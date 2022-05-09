/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.persist;

import java.util.Optional;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.json.JSONObject;

public class PersistentData
{
    private final StringProperty satisfactoryPath = new SimpleStringProperty();
    private final Preferences preferences;

    public PersistentData()
    {
        this.preferences = new Preferences();
    }

    public PersistentData(JSONObject json)
    {
        this.satisfactoryPath.set(json.optString("satisfactoryPath"));
        this.preferences = Optional.ofNullable(json.optJSONObject("preferences"))
                .map(Preferences::new)
                .orElseGet(Preferences::new);
    }

    public Preferences getPreferences()
    {
        return preferences;
    }

    public String getSatisfactoryPath()
    {
        return satisfactoryPath.get();
    }

    public void setSatisfactoryPath(String satisfactoryPath)
    {
        this.satisfactoryPath.set(satisfactoryPath);
    }

    public StringProperty satisfactoryPathProperty()
    {
        return satisfactoryPath;
    }

    JSONObject toJson()
    {
        JSONObject json = new JSONObject();

        json.put("satisfactoryPath", satisfactoryPath.get());
        json.put("preferences", preferences.toJson());

        return json;
    }

    public static class Preferences
    {
        private final UIPreferences uiPreferences;

        public Preferences()
        {
            this.uiPreferences = new UIPreferences();
        }

        public Preferences(JSONObject jsonObject)
        {
            this.uiPreferences = Optional.ofNullable(jsonObject.optJSONObject("ui"))
                    .map(UIPreferences::new)
                    .orElseGet(UIPreferences::new);
        }

        public UIPreferences getUiPreferences()
        {
            return uiPreferences;
        }

        JSONObject toJson()
        {
            JSONObject json = new JSONObject();
            json.put("ui", uiPreferences.toJson());
            return json;
        }

        public static class UIPreferences
        {
            private final BooleanProperty darkModeEnabled = new SimpleBooleanProperty();

            public UIPreferences()
            {
            }

            public UIPreferences(JSONObject jsonObject)
            {
                darkModeEnabled.set(jsonObject.optBoolean("darkModeEnabled", false));
            }

            public BooleanProperty darkModeEnabledProperty()
            {
                return darkModeEnabled;
            }

            public boolean isDarkModeEnabled()
            {
                return darkModeEnabled.get();
            }

            public void setDarkModeEnabled(boolean darkModeEnabled)
            {
                this.darkModeEnabled.set(darkModeEnabled);
            }

            JSONObject toJson()
            {
                JSONObject json = new JSONObject();
                json.put("darkModeEnabled", darkModeEnabled.get());
                return json;
            }
        }
    }
}
