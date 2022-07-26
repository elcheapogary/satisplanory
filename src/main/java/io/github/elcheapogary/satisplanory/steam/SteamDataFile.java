/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.steam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

class SteamDataFile
{
    private SteamDataFile()
    {
    }

    public static SteamDataObject parse(File f)
            throws IOException, SteamDataFormatException
    {
        try (InputStream in = new FileInputStream(f)) {
            return parse(in);
        }
    }

    public static SteamDataObject parse(InputStream in)
            throws SteamDataFormatException, IOException
    {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return parseObjectBody(r, true);
        }
    }

    private static SteamDataObject parseObject(BufferedReader r)
            throws SteamDataFormatException, IOException
    {
        for (; ; ){
            String line = r.readLine();

            if (line == null){
                throw new SteamDataFormatException("EOF after object name");
            }

            line = line.trim();

            if (line.equals("{")){
                return parseObjectBody(r, false);
            }else if (!line.isEmpty()){
                throw new SteamDataFormatException("Unexpected line looking for start of object: " + line);
            }
        }
    }

    private static SteamDataObject parseObjectBody(BufferedReader r, boolean eofAllowed)
            throws IOException, SteamDataFormatException
    {
        SteamDataObject retv = new SteamDataObject();

        for (; ; ){
            String line = r.readLine();

            if (line == null){
                if (eofAllowed){
                    return retv;
                }else{
                    throw new SteamDataFormatException("EOF in object");
                }
            }

            line = line.trim();

            if (line.isEmpty()){
                continue;
            }

            if (line.equals("}")){
                break;
            }

            if (!line.startsWith("\"")){
                throw new SteamDataFormatException("Bad format: " + line);
            }

            int endIndex = line.indexOf('"', 1);
            if (endIndex < 2){
                throw new SteamDataFormatException("Bad format: " + line);
            }

            String propertyName = line.substring(1, endIndex);

            String value = line.substring(endIndex + 1).trim();

            if (value.isEmpty()){
                SteamDataObject o = parseObject(r);
                retv.setObject(propertyName, o);
            }else if (!value.startsWith("\"") || !value.endsWith("\"")){
                throw new SteamDataFormatException("Bad value: " + line);
            }else{
                value = value.substring(1, value.length() - 1);
                value = value.replace("\\\"", "\"").replace("\\\\", "\\");

                retv.setString(propertyName, value);
            }
        }

        return retv;
    }
}
