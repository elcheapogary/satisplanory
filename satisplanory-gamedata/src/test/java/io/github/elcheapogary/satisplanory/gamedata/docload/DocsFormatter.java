/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.gamedata.docload;

import io.github.elcheapogary.satisplanory.util.CharStreams;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

public class DocsFormatter
{
    public static void main(String[] args)
            throws IOException
    {
        File inputFile = new File("");
        File outputFile = new File("");

        JsonArray array;

        try (JsonReader r = Json.createReader(CharStreams.createReader(new FileInputStream(inputFile)))){
            array = r.readArray();
        }

        try (JsonWriter w = Json.createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true))
                .createWriter(new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(outputFile.toPath()), StandardCharsets.UTF_8), 4096 * 4))){
            w.writeArray(array);
        }
    }
}
