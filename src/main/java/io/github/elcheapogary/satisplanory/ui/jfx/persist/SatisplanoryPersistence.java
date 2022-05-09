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

import io.github.elcheapogary.satisplanory.ui.jfx.context.AppContext;
import io.github.elcheapogary.satisplanory.ui.jfx.dialog.ExceptionDialog;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.json.JSONObject;
import org.json.JSONTokener;

public class SatisplanoryPersistence
{
    private SatisplanoryPersistence()
    {
    }

    private static File getJsonFile()
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

        File f = new File(localAppData, "Satisplanory");
        f = new File(f, "Satisplanory.json");
        return f;
    }

    public static PersistentData load()
            throws IOException
    {
        File jsonFile = getJsonFile();

        if (!jsonFile.isFile()){
            return new PersistentData();
        }

        try (Reader r = new InputStreamReader(new BufferedInputStream(new FileInputStream(jsonFile)), StandardCharsets.UTF_8)) {
            return new PersistentData(new JSONObject(new JSONTokener(r)));
        }
    }

    /*
     * We actually run this on the UI thread as a means of getting a stable snapshot
     */
    public static void save(AppContext appContext, PersistentData data)
    {
        File jsonFile = getJsonFile();

        try {
            Files.createDirectories(jsonFile.getParentFile().toPath());

            File tmpFile = new File(jsonFile.getParentFile(), "." + jsonFile.getName() + ".tmp");
            try {
                try (Writer w = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(tmpFile)), StandardCharsets.UTF_8)) {
                    data.toJson().write(w, 4, 0);
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
