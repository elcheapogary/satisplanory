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

import io.github.elcheapogary.satisplanory.Satisplanory;
import io.github.elcheapogary.satisplanory.ui.jfx.context.AppContext;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.ExceptionDialog;
import io.github.elcheapogary.satisplanory.util.JsonUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonWriter;

public class SatisplanoryPersistence
{
    private SatisplanoryPersistence()
    {
    }

    public static File getJsonFile()
    {
        return new File(getSatisplanoryDataDirectory(), "Satisplanory.json");
    }

    private static File getSatisplanoryDataDirectory()
    {
        String osName = System.getProperty("os.name");

        File localAppData;

        if (osName != null && osName.startsWith("Windows")){
            String path = System.getenv("LOCALAPPDATA");
            if (path == null){
                throw new RuntimeException("Unable to determine %LOCALAPPDATA% path");
            }
            localAppData = new File(path);
        }else{
            String xdgDataHome = System.getenv("XDG_DATA_HOME");

            if (xdgDataHome != null){
                localAppData = new File(xdgDataHome);
            }else{
                String home = System.getenv("HOME");
                if (home == null){
                    home = System.getProperty("user.home");
                }
                if (home == null){
                    throw new RuntimeException("Cannot determine user's home directory");
                }
                File f = new File(home, ".local");
                localAppData = new File(f, "share");
            }
        }

        if (Satisplanory.isDevelopmentVersion()){
            return new File(localAppData, "Satisplanory-dev");
        }else{
            return new File(localAppData, "Satisplanory");
        }
    }

    public static PersistentData load()
            throws IOException, UnsupportedVersionException
    {
        File jsonFile = getJsonFile();

        if (!jsonFile.isFile()){
            return new PersistentData();
        }

        try (JsonReader r = Json.createReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(jsonFile)), StandardCharsets.UTF_8))){
            return new PersistentData(r.readObject());
        }
    }

    /*
     * We actually run this on the UI thread as a means of getting a stable snapshot
     */
    public static void save(AppContext appContext, PersistentData data)
    {
        File jsonFile = getJsonFile();

        try{
            Files.createDirectories(jsonFile.getParentFile().toPath());

            File tmpFile = new File(jsonFile.getParentFile(), "." + jsonFile.getName() + ".tmp");
            try{
                try (JsonWriter w = JsonUtils.createWriter(tmpFile)){
                    w.writeObject(data.toJson());
                }
                Files.move(tmpFile.toPath(), jsonFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }finally{
                Files.deleteIfExists(tmpFile.toPath());
            }
        }catch (IOException | RuntimeException e){
            new ExceptionDialog(appContext)
                    .setTitle("Error saving Satisplanory data")
                    .setContextMessage("An error occurred while saving Satisplanory data")
                    .setDetailsMessage(e.toString())
                    .setException(e)
                    .showAndWait();
        }
    }
}
