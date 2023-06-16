/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MathExpressionTest
{

    @Test
    void evaluate()
    {
        assertEquals(BigFraction.valueOf(1), MathExpression.evaluate("1"));
        assertEquals(BigFraction.valueOf(-1), MathExpression.evaluate("-1"));
        assertEquals(BigFraction.valueOf(2), MathExpression.evaluate("1 + 1"));
        assertEquals(BigFraction.valueOf(-1), MathExpression.evaluate("1 - 2"));
        assertEquals(BigFraction.valueOf(-4), MathExpression.evaluate("1 - (3 + 2)"));
        assertEquals(BigFraction.valueOf(-38).subtract(BigFraction.valueOf(7).divide(8)), MathExpression.evaluate("1 + 2 * 3 - 4(5 + 6) - 7 / 8 + (9 - 10)"));
        assertEquals(BigFraction.valueOf(3), MathExpression.evaluate("-(-(1+2))"));
        assertEquals(BigFraction.valueOf(-3), MathExpression.evaluate("-(1+2)"));
    }
}