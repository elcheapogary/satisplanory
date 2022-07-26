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

import io.github.elcheapogary.satisplanory.steam.Steam;
import java.io.File;

public class SatisfactoryInstallation
{
    private SatisfactoryInstallation()
    {
    }

    public static File findSatisfactoryInstallation()
    {
        File f = Steam.findSatisfactoryInstallation();

        if (f != null && !isValidSatisfactoryInstallation(f)){
            f = null;
        }

        if (f == null){
            f = Epic.findSatisfactoryInstallation();
        }

        if (f != null && !isValidSatisfactoryInstallation(f)){
            f = null;
        }

        return f;
    }

    public static boolean isValidSatisfactoryInstallation(File installFolder)
    {
        File f = new File(installFolder, "CommunityResources");
        f = new File(f, "Docs");
        f = new File(f, "Docs.json");
        return f.isFile();
    }
}
