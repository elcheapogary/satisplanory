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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class CharStreams
{
    private CharStreams()
    {
    }

    public static Reader createReader(InputStream in)
            throws IOException
    {
        in = new BufferedInputStream(in);

        in.mark(3);

        int c = in.read();

        if (c == 0xff){
            c = in.read();

            if (c < 0){
                /*
                 * Only part of a UTF16LE BOM. Fuck it, you get that one byte as UTF8. Good luck with that.
                 */
                in.reset();
                return new InputStreamReader(in, StandardCharsets.UTF_8);
            }else if (c == 0xfe){
                return new InputStreamReader(in, StandardCharsets.UTF_16LE);
            }else{
                throw new IOException("Invalid byte order mark");
            }
        }else if (c == 0xfe){
            c = in.read();

            if (c < 0){
                /*
                 * Only part of a UTF16BE BOM. Fuck it, you get that one byte as UTF8. Good luck with that.
                 */
                in.reset();
                return new InputStreamReader(in, StandardCharsets.UTF_8);
            }else if (c == 0xff){
                return new InputStreamReader(in, StandardCharsets.UTF_16BE);
            }else{
                throw new IOException("Invalid byte order mark");
            }
        }else if (c < 0){
            /*
             * Empty file. Suppose you can have an empty UTF8 reader.
             */
            in.reset();
            return new InputStreamReader(in, StandardCharsets.UTF_8);
        }else{
            in.reset();
            return new InputStreamReader(in, StandardCharsets.UTF_8);
        }
    }
}
