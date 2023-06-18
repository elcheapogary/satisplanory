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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

class BONObject
{
    private final Map<String, String> map;

    public BONObject(Map<String, String> map)
    {
        this.map = Collections.unmodifiableMap(map);
    }

    public int getInteger(String name)
            throws BracketObjectNotationParseException
    {
        String s = getString(name);

        if (s == null){
            throw new BracketObjectNotationParseException("Missing element: " + name);
        }

        try{
            return Integer.parseInt(s);
        }catch (NumberFormatException e){
            throw new BracketObjectNotationParseException("Invalid integer for key: " + name + ": " + s, e);
        }
    }

    public String getString(String name)
    {
        return map.get(name);
    }

    public Set<String> keys()
    {
        return map.keySet();
    }

    public int size()
    {
        return map.size();
    }
}
