/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.model.docload;

import java.io.IOException;

class BracketObjectNotationParseException
        extends IOException
{
    public BracketObjectNotationParseException()
    {
    }

    public BracketObjectNotationParseException(String message)
    {
        super(message);
    }

    public BracketObjectNotationParseException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public BracketObjectNotationParseException(Throwable cause)
    {
        super(cause);
    }
}
