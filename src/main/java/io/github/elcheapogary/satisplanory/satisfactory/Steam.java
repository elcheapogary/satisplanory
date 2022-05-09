/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.satisfactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

class Steam
{
    private Steam()
    {
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

    private static boolean isValidSteamInstallation(File steamDirectory)
    {
        return getLibraryFoldersVdfFile(steamDirectory).isFile();
    }

    private static File getLibraryFoldersVdfFile(File steamDirectory)
    {
        File f = new File(steamDirectory, "steamapps");
        f = new File(f, "libraryfolders.vdf");
        return f;
    }

    public static File findSatisfactoryInstallation()
    {
        File steamDirectory = findSteamDirectory();

        if (steamDirectory == null){
            return null;
        }

        return getSatisfactoryInstallPath(steamDirectory);
    }

    private static File getSatisfactoryInstallPath(File steamDirectory)
    {
        File libraryVdf = getLibraryFoldersVdfFile(steamDirectory);

        try {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(libraryVdf), StandardCharsets.UTF_8))){
                skipLinesUntil(r, "\"libraryfolders\"");
                skipLinesUntil(r, "{");
                int id = 0;
                String line;

                while ((line = r.readLine()) != null){
                    line = line.trim();

                    if (line.equals("\"" + id + "\"")){
                        id++;
                        skipLinesUntil(r, "{");
                        String path = null;
                        while ((line = r.readLine()) != null){
                            line = line.trim();

                            if (line.startsWith("\"path\"")){
                                line = line.substring(6).trim();
                                if (line.startsWith("\"") && line.endsWith("\"")){
                                    path = line.substring(1, line.length() - 1);
                                }
                            }else if (line.equals("\"apps\"")){
                                skipLinesUntil(r, "{");
                                while ((line = r.readLine()) != null){
                                    line = line.trim();

                                    if (line.equals("}")){
                                        break;
                                    }else if (line.startsWith("\"526870\"") && path != null){
                                        File f = new File(path);
                                        f = new File(f, "steamapps");
                                        f = new File(f, "common");
                                        f = new File(f, "Satisfactory");
                                        return f;
                                    }
                                }
                            }
                        }
                        skipLinesUntil(r, "}");
                    }else if (line.equals("}")){
                        break;
                    }
                }

                return null;
            }
        }catch (IOException e){
            e.printStackTrace(System.err);
            return null;
        }
    }

    private static void skipLinesUntil(BufferedReader r, String target)
            throws IOException
    {
        String line;

        while ((line = r.readLine()) != null){
            line = line.trim();

            if (line.equals(target)){
                return;
            }
        }

        throw new IOException("Did not find desired line: " + target);
    }
}
