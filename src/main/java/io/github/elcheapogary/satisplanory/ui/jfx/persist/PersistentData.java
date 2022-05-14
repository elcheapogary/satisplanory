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

import java.io.File;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.json.JSONArray;
import org.json.JSONObject;

public class PersistentData
{
    private final StringProperty satisfactoryPath = new SimpleStringProperty();
    private final Preferences preferences;
    private final ObservableList<PersistentProductionPlan> productionPlans;

    public PersistentData()
    {
        this.preferences = new Preferences();
        this.productionPlans = FXCollections.observableList(new ArrayList<>());
    }

    public PersistentData(JSONObject json)
            throws UnsupportedVersionException
    {
        if (json.has("v") && Double.parseDouble(json.getString("v")) > 1.2){
            throw new UnsupportedVersionException();
        }
        this.satisfactoryPath.set(json.optString("satisfactoryPath"));
        this.preferences = Optional.ofNullable(json.optJSONObject("preferences"))
                .map(Preferences::new)
                .orElseGet(Preferences::new);
        this.productionPlans = FXCollections.observableList(new ArrayList<>());
        JSONArray jsonProductionPlans = json.optJSONArray("productionPlans");
        if (jsonProductionPlans != null){
            for (int i = 0; i < jsonProductionPlans.length(); i++){
                productionPlans.add(new PersistentProductionPlan(jsonProductionPlans.getJSONObject(i)));
            }
        }
    }

    public Preferences getPreferences()
    {
        return preferences;
    }

    public ObservableList<PersistentProductionPlan> getProductionPlans()
    {
        return productionPlans;
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

        json.put("v", "1.2");

        json.put("satisfactoryPath", satisfactoryPath.get());
        json.put("preferences", preferences.toJson());

        json.put("productionPlans", new JSONArray(
                productionPlans.stream()
                        .map(PersistentProductionPlan::toJson)
                        .collect(Collectors.toList())
        ));

        return json;
    }

    public static class Preferences
    {
        private final UIPreferences uiPreferences;
        private final ObjectProperty<File> lastImportExportDirectory = new SimpleObjectProperty<>();

        public Preferences()
        {
            this.uiPreferences = new UIPreferences();
        }

        public Preferences(JSONObject json)
        {
            this.uiPreferences = Optional.ofNullable(json.optJSONObject("ui"))
                    .map(UIPreferences::new)
                    .orElseGet(UIPreferences::new);

            if (json.has("lastImportExportDirectory")){
                lastImportExportDirectory.set(new File(json.getString("lastImportExportDirectory")));
            }
        }

        public File getLastImportExportDirectory()
        {
            return lastImportExportDirectory.get();
        }

        public void setLastImportExportDirectory(File lastImportExportDirectory)
        {
            this.lastImportExportDirectory.set(lastImportExportDirectory);
        }

        public UIPreferences getUiPreferences()
        {
            return uiPreferences;
        }

        public ObjectProperty<File> lastImportExportDirectoryProperty()
        {
            return lastImportExportDirectory;
        }

        JSONObject toJson()
        {
            JSONObject json = new JSONObject();
            json.put("ui", uiPreferences.toJson());
            if (lastImportExportDirectory.getValue() != null){
                json.put("lastImportExportDirectory", lastImportExportDirectory.getValue().getAbsolutePath());
            }
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
