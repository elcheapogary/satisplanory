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

class SteamDataString
        extends SteamDataElement
{
    private final String string;

    public SteamDataString(String string)
    {
        this.string = string;
    }

    @Override
    public SteamDataObject getAsObject()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAsString()
    {
        return string;
    }

    @Override
    public boolean isObject()
    {
        return false;
    }

    @Override
    public boolean isString()
    {
        return true;
    }
}
