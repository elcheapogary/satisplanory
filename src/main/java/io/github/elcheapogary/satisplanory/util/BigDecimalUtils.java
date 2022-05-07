/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BigDecimalUtils
{
    private BigDecimalUtils()
    {
    }

    public static BigDecimal normalize(BigDecimal v)
    {
        v = v.stripTrailingZeros();
        if (v.scale() < 0){
            v = v.setScale(0, RoundingMode.UNNECESSARY);
        }
        return v;
    }
}
