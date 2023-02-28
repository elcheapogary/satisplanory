/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.prodplan.lp;

import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.util.Collections;
import java.util.Map;

public class BinaryExpression
        extends IntegerExpression
{
    static final BinaryExpression ZERO = new BinaryExpression(Collections.emptyMap(), BigFraction.zero());

    BinaryExpression(Map<DecisionVariable, BigFraction> coefficients, BigFraction constantValue)
    {
        super(coefficients, constantValue);
    }
}
