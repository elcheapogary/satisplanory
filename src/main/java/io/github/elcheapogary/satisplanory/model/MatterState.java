/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.model;

import io.github.elcheapogary.satisplanory.util.BigDecimalUtils;
import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.math.BigDecimal;
import java.math.RoundingMode;

public enum MatterState
{
    SOLID{
        @Override
        public BigDecimal fromDisplayAmount(BigDecimal amount)
        {
            return amount;
        }

        @Override
        public BigDecimal toDisplayAmount(BigDecimal amount)
        {
            return amount;
        }

        @Override
        public BigFraction fromDisplayAmount(BigFraction amount)
        {
            return amount;
        }

        @Override
        public BigFraction toDisplayAmount(BigFraction amount)
        {
            return amount;
        }
    }, LIQUID, GAS;

    public BigDecimal fromDisplayAmount(BigDecimal amount)
    {
        return BigDecimalUtils.normalize(amount.multiply(BigDecimal.valueOf(1000)));
    }

    public BigFraction fromDisplayAmount(BigFraction amount)
    {
        return amount.multiply(1000).simplify();
    }

    public BigDecimal toDisplayAmount(BigDecimal amount)
    {
        return BigDecimalUtils.normalize(amount.divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP));
    }

    public BigFraction toDisplayAmount(BigFraction amount)
    {
        return amount.divide(1000).simplify();
    }
}
