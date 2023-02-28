/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import javax.json.Json;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

public class JsonUtils
{
    private JsonUtils()
    {
    }

    public static JsonWriter createWriter(File file)
            throws FileNotFoundException
    {
        return createWriter(new FileOutputStream(file));
    }

    public static JsonWriter createWriter(OutputStream out)
    {
        return createWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
    }

    public static JsonWriter createWriter(Writer writer)
    {
        return Json.createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true))
                .createWriter(new BufferedWriter(writer));
    }
}
