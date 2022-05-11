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

import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

public abstract class FractionExpression
        extends Expression
{
    static final FractionExpression ZERO = new FractionExpression(BigFraction.ZERO)
    {
        @Override
        Map<? extends Variable, ? extends BigFraction> getVariableValues()
        {
            return Collections.emptyMap();
        }
    };

    FractionExpression(BigFraction constantValue)
    {
        super(constantValue);
    }

    public BigFraction getValue(OptimizationResult result)
    {
        return result.getValue(this);
    }
}
