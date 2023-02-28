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

public class FileNameUtils
{
    private FileNameUtils()
    {
    }

    public static String removeUnsafeFilenameCharacters(String s)
    {
        StringBuilder sb = new StringBuilder(s.length());
        char[] unsafe = "\\/:*?\"<>|".toCharArray();

        for (char c : s.toCharArray()){
            boolean safe = true;
            for (char u : unsafe){
                if (c == u){
                    safe = false;
                    break;
                }
            }
            if (safe){
                sb.append(c);
            }
        }

        return sb.toString();
    }
}
