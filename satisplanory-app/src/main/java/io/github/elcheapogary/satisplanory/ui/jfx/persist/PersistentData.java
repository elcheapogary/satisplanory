/*
 * Copyright (c) 2023 elcheapogary
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class PersistentData
{
    private final Preferences preferences;
    private final ObservableList<PersistentProductionPlan> productionPlans;

    public PersistentData()
    {
        this.preferences = new Preferences();
        this.productionPlans = FXCollections.observableList(new ArrayList<>());
    }

    public PersistentData(JsonObject json)
            throws UnsupportedVersionException
    {
        if (json.containsKey("v") && Double.parseDouble(json.getString("v")) > 1.8){
            throw new UnsupportedVersionException();
        }
        this.preferences = Optional.ofNullable(json.getJsonObject("preferences"))
                .map(Preferences::new)
                .orElseGet(Preferences::new);
        this.productionPlans = FXCollections.observableList(new ArrayList<>());
        JsonArray jsonProductionPlans = json.getJsonArray("productionPlans");
        if (jsonProductionPlans != null){
            for (JsonObject jsonPlan : jsonProductionPlans.getValuesAs(JsonObject.class)){
                PersistentProductionPlan plan = new PersistentProductionPlan();
                plan.loadJson(jsonPlan);
                productionPlans.add(plan);
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

    JsonObject toJson()
    {
        return Json.createObjectBuilder()
                .add("v", "1.8")
                .add("preferences", preferences.toJson())
                .add("productionPlans", Json.createArrayBuilder(
                        productionPlans.stream()
                                .map(PersistentProductionPlan::toJson)
                                .collect(Collectors.toList())
                ))
                .build();
    }

    public static class Preferences
    {
        private final UIPreferences uiPreferences;
        private final ObjectProperty<File> lastImportExportDirectory = new SimpleObjectProperty<>();

        public Preferences()
        {
            this.uiPreferences = new UIPreferences();
        }

        public Preferences(JsonObject json)
        {
            this.uiPreferences = Optional.ofNullable(json.getJsonObject("ui"))
                    .map(UIPreferences::new)
                    .orElseGet(UIPreferences::new);

            if (json.containsKey("lastImportExportDirectory")){
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

        JsonObject toJson()
        {
            JsonObjectBuilder b = Json.createObjectBuilder();
            b = b.add("ui", uiPreferences.toJson());
            if (lastImportExportDirectory.getValue() != null){
                b = b.add("lastImportExportDirectory", lastImportExportDirectory.getValue().getAbsolutePath());
            }
            return b.build();
        }

        public static class UIPreferences
        {
            private final BooleanProperty darkModeEnabled = new SimpleBooleanProperty();

            public UIPreferences()
            {
            }

            public UIPreferences(JsonObject jsonObject)
            {
                darkModeEnabled.set(jsonObject.getBoolean("darkModeEnabled", false));
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

            JsonObject toJson()
            {
                return Json.createObjectBuilder()
                        .add("darkModeEnabled", darkModeEnabled.get())
                        .build();
            }
        }
    }
}
