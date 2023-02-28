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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

public class ResourceUtils
{
    private ResourceUtils()
    {
    }

    public static String getResourceAsString(Class<?> c, String resourceName)
            throws IOException
    {
        InputStream in = c.getResourceAsStream(resourceName);

        if (in == null){
            throw new IOException("Missing resource for class: " + c.getName() + ": " + resourceName);
        }

        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return IOUtils.toString(r);
        }
    }
}
