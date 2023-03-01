/*
 * Copyright (c) 2023 elcheapogary
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
        public BigDecimal toDisplayAmount(BigDecimal amount)
        {
            return amount.setScale(4, RoundingMode.HALF_UP);
        }

        @Override
        public BigFraction fromDisplayAmount(BigFraction amount)
        {
            return amount;
        }

        @Override
        public BigFraction toDisplayAmountFraction(BigFraction amount)
        {
            return amount;
        }

        @Override
        protected String appendDisplayUnits(String displayAmount)
        {
            return displayAmount;
        }
    }, LIQUID, GAS;

    public BigFraction fromDisplayAmount(BigFraction amount)
    {
        return amount.multiply(1000);
    }

    /**
     * Returns the provided {@code amount} in display units, scaled to 4 decimal places. This result is not normalized.
     *
     * @param amount The amount to convert to display units.
     * @return the provided {@code amount} in display units, scaled to 4 decimal places. This result is not normalized.
     */
    public BigDecimal toDisplayAmount(BigDecimal amount)
    {
        return amount.divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);
    }

    public BigFraction toDisplayAmountFraction(BigFraction amount)
    {
        return amount.divide(1000);
    }

    /**
     * Returns the provided {@code amount} in display units, scaled to 4 decimal places. This result is not normalized.
     *
     * @param amount The amount to convert to display units.
     * @return the provided {@code amount} in display units, scaled to 4 decimal places. This result is not normalized.
     */
    public final BigDecimal toDisplayAmount(BigFraction amount)
    {
        return toDisplayAmountFraction(amount).toBigDecimal(4, RoundingMode.HALF_UP);
    }

    protected String appendDisplayUnits(String displayAmount)
    {
        return displayAmount + "m³";
    }

    public final String toDisplayAmountString(BigDecimal amount)
    {
        return appendDisplayUnits(toDisplayAmount(amount).toString());
    }

    public final String toNormalizedDisplayAmountString(BigDecimal amount)
    {
        return appendDisplayUnits(BigDecimalUtils.normalize(toDisplayAmount(amount)).toString());
    }

    public final String toDisplayAmountString(BigFraction amount)
    {
        return appendDisplayUnits(toDisplayAmount(amount).toString());
    }

    public final String toNormalizedDisplayAmountString(BigFraction amount)
    {
        return appendDisplayUnits(BigDecimalUtils.normalize(toDisplayAmount(amount)).toString());
    }
}
