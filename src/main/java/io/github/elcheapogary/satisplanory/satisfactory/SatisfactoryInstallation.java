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
import java.util.Collection;
import java.util.LinkedList;

public class SatisfactoryInstallation
{
    private final SatisfactoryInstallationDiscoveryMechanism gameStore;
    private final SatisfactoryBranch branch;
    private final File path;

    public SatisfactoryInstallation(SatisfactoryInstallationDiscoveryMechanism gameStore, SatisfactoryBranch branch, File path)
    {
        this.gameStore = gameStore;
        this.branch = branch;
        this.path = path;
    }

    public static Collection<SatisfactoryInstallation> findSatisfactoryInstallations()
    {
        Collection<SatisfactoryInstallation> installations = new LinkedList<>();

        Steam.findSatisfactoryInstallation()
                .ifPresent(installations::add);

        installations.addAll(Epic.findSatisfactoryInstallations());

        return installations;
    }

    public static boolean isValidSatisfactoryInstallation(File installFolder)
    {
        File f = new File(installFolder, "CommunityResources");
        f = new File(f, "Docs");
        f = new File(f, "Docs.json");
        return f.isFile();
    }

    public SatisfactoryBranch getBranch()
    {
        return branch;
    }

    public SatisfactoryInstallationDiscoveryMechanism getGameStore()
    {
        return gameStore;
    }

    public File getPath()
    {
        return path;
    }

    @Override
    public String toString()
    {
        if (branch == null){
            return gameStore.name() + " / " + path;
        }else{
            return gameStore.name() + " / " + branch.getDisplayName() + " / " + path;
        }
    }
}
