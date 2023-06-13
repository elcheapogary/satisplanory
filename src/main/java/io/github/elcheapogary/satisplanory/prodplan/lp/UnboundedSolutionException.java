/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.prodplan.lp;

public class UnboundedSolutionException
        extends Exception
{
    public UnboundedSolutionException()
    {
    }

    public UnboundedSolutionException(String message)
    {
        super(message);
    }

    public UnboundedSolutionException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UnboundedSolutionException(Throwable cause)
    {
        super(cause);
    }
}