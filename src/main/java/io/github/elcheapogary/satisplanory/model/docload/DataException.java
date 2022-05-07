/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.model.docload;

public class DataException
        extends Exception
{
    public DataException()
    {
    }

    public DataException(String message)
    {
        super(message);
    }

    public DataException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public DataException(Throwable cause)
    {
        super(cause);
    }
}
