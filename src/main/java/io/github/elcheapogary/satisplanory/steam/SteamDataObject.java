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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class SteamDataObject
        extends SteamDataElement
{
    private final Map<String, SteamDataElement> properties = new TreeMap<>();

    public SteamDataObject()
    {
    }

    public SteamDataElement get(String propertyName)
    {
        return properties.get(propertyName);
    }

    @Override
    public SteamDataObject getAsObject()
    {
        return this;
    }

    @Override
    public String getAsString()
    {
        throw new UnsupportedOperationException();
    }

    public Set<String> getPropertyNames()
    {
        return Collections.unmodifiableSet(properties.keySet());
    }

    public boolean hasObject(String propertyName)
    {
        return properties.get(propertyName) instanceof SteamDataObject;
    }

    public boolean hasString(String propertyName)
    {
        return properties.get(propertyName) instanceof SteamDataString;
    }

    @Override
    public boolean isObject()
    {
        return true;
    }

    @Override
    public boolean isString()
    {
        return false;
    }

    public void setObject(String propertyName, SteamDataObject object)
    {
        properties.put(propertyName, object);
    }

    public void setString(String propertyName, String propertyValue)
    {
        properties.put(propertyName, new SteamDataString(propertyValue));
    }
}
