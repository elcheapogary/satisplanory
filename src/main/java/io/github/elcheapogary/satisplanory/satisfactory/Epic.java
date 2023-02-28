/*
 * Copyright (c) 2023 elcheapogary
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
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

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

        JsonObject wholeFile;
        try{
            try (JsonReader r = Json.createReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))){
                wholeFile = r.readObject();
            }
        }catch (IOException | RuntimeException e){
            e.printStackTrace();
            return Collections.emptyList();
        }

        try{
            JsonArray installations = wholeFile.getJsonArray("InstallationList");

            File earlyAccess = null;
            File experimental = null;

            for (JsonObject installation : installations.getValuesAs(JsonObject.class)){

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

                if (installation.containsKey("NamespaceId") && installation.getString("NamespaceId").equals("crab")){
                    if (installation.containsKey("ArtifactId") && installation.containsKey("InstallLocation")){
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
        }catch (RuntimeException e){
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
