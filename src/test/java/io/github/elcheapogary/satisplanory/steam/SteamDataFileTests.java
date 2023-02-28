/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.steam;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SteamDataFileTests
{
    @Test
    public void testSample1()
            throws IOException, SteamDataFormatException
    {
        InputStream in = SteamDataFileTests.class.getResourceAsStream("sample1.sdf");

        assert in != null;

        try {
            SteamDataObject o = SteamDataFile.parse(in);

            assertEquals(Set.of("libraryfolders"), o.getPropertyNames());

            assertTrue(o.hasObject("libraryfolders"));
            assertFalse(o.hasString("libraryfolders"));

            o = o.get("libraryfolders").getAsObject();

            assertEquals(Set.of("0", "1"), o.getPropertyNames());

            o = o.get("0").getAsObject();

            assertTrue(o.hasString("path"));
            assertEquals("C:\\Program Files (x86)\\Steam", o.get("path").getAsString());
        }finally{
            in.close();
        }
    }
}
