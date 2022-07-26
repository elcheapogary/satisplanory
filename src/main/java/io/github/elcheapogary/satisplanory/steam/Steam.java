/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.steam;

import java.io.File;
import java.io.IOException;

public class Steam
{
    private Steam()
    {
    }

    public static File findSatisfactoryInstallation()
    {
        File steamDirectory = findSteamDirectory();

        if (steamDirectory == null){
            return null;
        }

        return getSatisfactoryInstallPath(steamDirectory);
    }

    private static File findSteamDirectory()
    {
        File f;

        // TODO: 32bit: reg query "HKEY_LOCAL_MACHINE\SOFTWARE\Valve\Steam" /v InstallPath
        // TODO: 64bit: reg query "HKEY_LOCAL_MACHINE\SOFTWARE\Wow6432Node\Valve\Steam" /v InstallPath

        f = getDefaultSteamDirectory();
        if (isValidSteamInstallation(f)){
            return f;
        }

        return null;
    }

    private static File getDefaultSteamDirectory()
    {
        String programFiles = System.getenv("PROGRAMFILES(x86)");

        if (programFiles == null){
            programFiles = System.getenv("PROGRAMFILES");
        }

        if (programFiles == null){
            return null;
        }

        return new File(programFiles, "Steam");
    }

    private static File getLibraryFoldersVdfFile(File steamDirectory)
    {
        File f = new File(steamDirectory, "steamapps");
        f = new File(f, "libraryfolders.vdf");
        return f;
    }

    private static File getSatisfactoryInstallPath(File steamDirectory)
    {
        File libraryVdf = getLibraryFoldersVdfFile(steamDirectory);

        try {
            SteamDataObject data = SteamDataFile.parse(libraryVdf);

            if (!data.hasObject("libraryfolders")){
                return null;
            }

            data = data.get("libraryfolders").getAsObject();

            for (String propertyName : data.getPropertyNames()){
                if (data.hasObject(propertyName)){
                    SteamDataObject library = data.getAsObject();

                    if (library.hasString("path") && library.hasObject("apps")){
                        SteamDataObject apps = library.get("apps").getAsObject();

                        if (apps.hasString("526870")){
                            File f = new File(library.get("path").getAsString());
                            f = new File(f, "steamapps");
                            f = new File(f, "common");
                            f = new File(f, "Satisfactory");
                            return f;
                        }
                    }
                }
            }
        }catch (IOException | SteamDataFormatException e){
            e.printStackTrace(System.err);
        }

        return null;
    }

    private static boolean isValidSteamInstallation(File steamDirectory)
    {
        return getLibraryFoldersVdfFile(steamDirectory).isFile();
    }
}
