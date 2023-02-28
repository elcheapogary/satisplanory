/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.model.test;

import io.github.elcheapogary.satisplanory.model.GameData;
import io.github.elcheapogary.satisplanory.model.Item;
import io.github.elcheapogary.satisplanory.model.Recipe;
import io.github.elcheapogary.satisplanory.model.docload.DataException;
import io.github.elcheapogary.satisplanory.model.docload.DocsJsonLoader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class TestGameData
        extends GameData
{
    private static Map<String, String> testDataConfig = null;
    private static TestGameData update7GameData = null;

    private TestGameData(Builder builder)
    {
        super(builder);
    }

    private static Map<String, String> loadDataConfig()
    {
        synchronized (TestGameData.class){
            if (testDataConfig == null){
                File f = new File("testdata.conf");

                if (!f.isFile()){
                    return testDataConfig = Collections.emptyMap();
                }else{
                    try{
                        Map<String, String> m = new TreeMap<>();
                        try (BufferedReader r = new BufferedReader(new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8))){
                            String line;

                            while ((line = r.readLine()) != null){
                                line = line.trim();

                                if (line.isEmpty()){
                                    continue;
                                }

                                if (line.startsWith("#")){
                                    continue;
                                }

                                int idx = line.indexOf('=');

                                if (idx < 0){
                                    throw new IOException("Invalid line in testdata.conf: " + line);
                                }

                                String key = line.substring(0, idx).trim();
                                String value = line.substring(idx + 1).trim();

                                if (!key.isEmpty() && !value.isEmpty()){
                                    m.put(key, value);
                                }
                            }
                        }
                        testDataConfig = m;
                    }catch (IOException e){
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return testDataConfig;
    }

    public static TestGameData getLatestTestData()
    {
        return getUpdate7TestData();
    }

    public static TestGameData getUpdate7TestData()
    {
        synchronized (TestGameData.class){
            if (update7GameData == null){
                String fileName = loadDataConfig().get("u7.docsjson");

                if (fileName == null){
                    return null;
                }

                try{
                    update7GameData = loadGameDataFromFile(fileName);
                }catch (IOException | DataException e){
                    throw new RuntimeException(e);
                }
            }

            return update7GameData;
        }
    }

    private static TestGameData loadGameDataFromFile(String fileName)
            throws IOException, DataException
    {
        TestGameDataBuilder gameDataBuilder = new TestGameDataBuilder();

        try (InputStream in = Files.newInputStream(Path.of(fileName))){
            DocsJsonLoader.loadDocsJson(gameDataBuilder, in);
        }

        return gameDataBuilder.build();
    }

    public Item requireItemByName(String itemName)
    {
        return getItemByName(itemName)
                .orElseThrow(() -> new RuntimeException("Unable to find item by name: " + itemName));
    }

    public Recipe requireRecipeByName(String recipeName)
    {
        return getRecipeByName(recipeName)
                .orElseThrow(() -> new RuntimeException("Unable to find recipe by name: " + recipeName));
    }

    private static class TestGameDataBuilder
            extends Builder
    {
        @Override
        public TestGameData build()
        {
            return new TestGameData(this);
        }
    }
}
