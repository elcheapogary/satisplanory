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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

class Epic
{
    private Epic()
    {
    }

    public static Collection<? extends SatisfactoryInstallation> findSatisfactoryInstallations()
    {
        File epicDirectory = getEpicDirectory();

        File f = new File(epicDirectory, "UnrealEngineLauncher");
        f = new File(f, "LauncherInstalled.dat");

        if (!f.isFile()){
            return Collections.emptyList();
        }

        JSONObject wholeFile;
        try {
            try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                wholeFile = new JSONObject(new JSONTokener(r));
            }
        }catch (IOException | JSONException e){
            e.printStackTrace();
            return Collections.emptyList();
        }

        try {
            JSONArray installations = wholeFile.getJSONArray("InstallationList");

            File earlyAccess = null;
            File experimental = null;

            for (int i = 0; i < installations.length(); i++){
                JSONObject installation = installations.getJSONObject(i);

//        Thanks Goz3rr
//
//        {
//            "InstallLocation": "D:\\Games\\Epic Games\\SatisfactoryEarlyAccess",
//            "NamespaceId": "crab",
//            "ItemId": "b915dfe8dcf74770841c82a4162dc954",
//            "ArtifactId": "CrabEA",
//            "AppVersion": "186638.729",
//            "AppName": "CrabEA"
//        },
//        {
//            "InstallLocation": "D:\\Games\\Epic Games\\SatisfactoryExperimental",
//            "NamespaceId": "crab",
//            "ItemId": "ef4a63daa7d4420e91420a72050be89d",
//            "ArtifactId": "CrabTest",
//            "AppVersion": "186638.729",
//            "AppName": "CrabTest"
//        },

                if (installation.has("NamespaceId") && installation.getString("NamespaceId").equals("crab")){
                    if (installation.has("ArtifactId") && installation.has("InstallLocation")){
                        File installDir = new File(installation.getString("InstallLocation"));
                        if (installation.getString("ArtifactId").equals("CrabEA")){
                            earlyAccess = installDir;
                        }else if (installation.getString("ArtifactId").equals("CrabTest")){
                            experimental = installDir;
                        }
                    }
                }
            }

            Collection<SatisfactoryInstallation> retv = new LinkedList<>();

            if (earlyAccess != null){
                retv.add(new SatisfactoryInstallation(SatisfactoryInstallationDiscoveryMechanism.Epic, SatisfactoryBranch.EARLY_ACCESS, earlyAccess));
            }

            if (experimental != null){
                retv.add(new SatisfactoryInstallation(SatisfactoryInstallationDiscoveryMechanism.Epic, SatisfactoryBranch.EXPERIMENTAL, experimental));
            }

            return retv;
        }catch (JSONException e){
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private static File getEpicDirectory()
    {
        String programDataEnvVar = System.getenv("PROGRAMDATA");

        if (programDataEnvVar != null && !programDataEnvVar.isBlank()){
            return new File(programDataEnvVar, "Epic");
        }

        return new File("C:\\ProgramData\\Epic");
    }
}
