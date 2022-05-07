/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.model.test;

import io.github.elcheapogary.satisplanory.util.CharStreams;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONTokener;

public class DocsFormatter
{
    public static void main(String[] args)
            throws IOException
    {
        File inputFile = new File("");
        File outputFile = new File("");

        JSONArray array;

        try (Reader r = CharStreams.createReader(new FileInputStream(inputFile))) {
            array = new JSONArray(new JSONTokener(r));
        }

        try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            array.write(w, 1, 0);
        }
    }
}
