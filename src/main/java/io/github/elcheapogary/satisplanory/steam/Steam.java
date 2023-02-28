/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.steam;

import io.github.elcheapogary.satisplanory.satisfactory.SatisfactoryBranch;
import io.github.elcheapogary.satisplanory.satisfactory.SatisfactoryInstallation;
import io.github.elcheapogary.satisplanory.satisfactory.SatisfactoryInstallationDiscoveryMechanism;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class Steam
{
    private Steam()
    {
    }

    public static Optional<SatisfactoryInstallation> findSatisfactoryInstallation()
    {
        File steamDirectory = findSteamDirectory();

        if (steamDirectory == null){
            return Optional.empty();
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

    private static Optional<SatisfactoryInstallation> getSatisfactoryInstallPath(File steamDirectory)
    {
        File libraryVdf = getLibraryFoldersVdfFile(steamDirectory);

        try {
            SteamDataObject data = SteamDataFile.parse(libraryVdf);

            if (!data.hasObject("libraryfolders")){
                throw new IOException("No \"libraryfolders\" object in " + libraryVdf.getAbsolutePath());
            }

            data = data.get("libraryfolders").getAsObject();

            for (String libraryName : data.getPropertyNames()){
                if (data.hasObject(libraryName)){
                    SteamDataObject library = data.get(libraryName).getAsObject();

                    if (library.hasString("path") && library.hasObject("apps")){
                        SteamDataObject apps = library.get("apps").getAsObject();

                        if (apps.hasString("526870")){
                            File steamAppsDirectory = new File(library.get("path").getAsString());
                            steamAppsDirectory = new File(steamAppsDirectory, "steamapps");

                            SteamDataObject appManifest = SteamDataFile.parse(new File(steamAppsDirectory, "appmanifest_526870.acf"));

                            if (appManifest.hasObject("AppState")){
                                SteamDataObject appState = appManifest.get("AppState").getAsObject();

                                if (appState.hasString("installdir")){
                                    File installDirectory = new File(steamAppsDirectory, "common");
                                    installDirectory = new File(installDirectory, appState.get("installdir").getAsString());

                                    SatisfactoryBranch branch = SatisfactoryBranch.EARLY_ACCESS;

                                    if (appState.hasObject("UserConfig")){
                                        SteamDataObject userConfig = appState.get("UserConfig").getAsObject();

                                        if (userConfig.hasString("betakey") && "experimental".equals(userConfig.get("betakey").getAsString())){
                                            branch = SatisfactoryBranch.EXPERIMENTAL;
                                        }
                                    }

                                    return Optional.of(new SatisfactoryInstallation(SatisfactoryInstallationDiscoveryMechanism.Steam, branch, installDirectory));
                                }
                            }
                        }
                    }
                }
            }
        }catch (IOException | SteamDataFormatException e){
            e.printStackTrace(System.err);
        }

        return Optional.empty();
    }

    private static boolean isValidSteamInstallation(File steamDirectory)
    {
        return getLibraryFoldersVdfFile(steamDirectory).isFile();
    }
}
