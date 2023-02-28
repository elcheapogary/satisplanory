/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui;

import io.github.elcheapogary.satisplanory.model.GameData;
import io.github.elcheapogary.satisplanory.model.docload.DataException;
import io.github.elcheapogary.satisplanory.model.docload.DocsJsonLoader;
import java.io.File;
import java.io.IOException;

public class SatisfactoryDataLoader
{
    private SatisfactoryDataLoader()
    {
    }

    private static void loadSatisfactoryData(GameData.Builder gameDataBuilder, File satisfactoryDirectory)
            throws InvalidSatisfactoryDirectoryException, DataException, IOException
    {
        if (!satisfactoryDirectory.isDirectory()){
            throw new InvalidSatisfactoryDirectoryException(satisfactoryDirectory, "Directory does not exist");
        }

        File f = new File(satisfactoryDirectory, "CommunityResources");

        if (!f.isDirectory()){
            throw new InvalidSatisfactoryDirectoryException(satisfactoryDirectory, "Missing subdirectory: CommunityResources");
        }

        f = new File(f, "Docs");

        if (!f.isDirectory()){
            throw new InvalidSatisfactoryDirectoryException(satisfactoryDirectory, "Missing subdirectory: CommunityResources\\Docs");
        }

        f = new File(f, "Docs.json");

        if (!f.isFile()){
            throw new InvalidSatisfactoryDirectoryException(satisfactoryDirectory, "Missing file: CommunityResources\\Docs\\Docs.json");
        }

        DocsJsonLoader.loadDocsJson(gameDataBuilder, f);
    }

    public static GameData.Builder loadSatisfactoryData(File satisfactoryDirectory)
            throws DataException, InvalidSatisfactoryDirectoryException, IOException
    {
        GameData.Builder gameDataBuilder = new GameData.Builder();

        loadSatisfactoryData(gameDataBuilder, satisfactoryDirectory);

        return gameDataBuilder;
    }

    private static class InvalidSatisfactoryDirectoryException
            extends Exception
    {
        public InvalidSatisfactoryDirectoryException(File satisfactoryDirectory, String message)
        {
            super(satisfactoryDirectory.getAbsolutePath() + " is not a vlid Satisfactory installation: " + message);
        }
    }
}
