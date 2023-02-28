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

public class SteamDataFormatException
        extends Exception
{
    public SteamDataFormatException()
    {
    }

    public SteamDataFormatException(String message)
    {
        super(message);
    }

    public SteamDataFormatException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public SteamDataFormatException(Throwable cause)
    {
        super(cause);
    }
}
