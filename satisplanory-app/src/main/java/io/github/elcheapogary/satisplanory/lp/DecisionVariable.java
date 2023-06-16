/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.lp;

class DecisionVariable
        extends Variable
{
    private final String name;

    public DecisionVariable(int id, String name)
    {
        super(id);
        this.name = name;
    }

    @Override
    public String getDebugName()
    {
        return "x" + getId();
    }

    public String getName()
    {
        return name;
    }
}
