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

import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;

class ZeroIfLessThanBranchingConstraint
        extends BranchingConstraint
{
    static final String JSON_TYPE = "zero-if-lt";
    private final Expression expression;
    private final BigFraction minimum;

    public ZeroIfLessThanBranchingConstraint(Expression expression, BigFraction minimum)
    {
        this.expression = expression;
        this.minimum = minimum;
    }

    static ZeroIfLessThanBranchingConstraint fromJson(JsonObject json, Map<Integer, ? extends DecisionVariable> decisionVariableMap)
    {
        Expression expression = Expression.fromJson(json.getJsonObject("expression"), decisionVariableMap);
        BigFraction minimum = BigFraction.parse(json.getString("min"));
        return new ZeroIfLessThanBranchingConstraint(expression, minimum);
    }

    @Override
    Collection<? extends Constraint> getConstraints(Tableau tableau)
    {
        BigFraction value = tableau.getValue(expression);

        if (value.signum() > 0 && value.compareTo(minimum) < 0){
            List<Constraint> constraints = new ArrayList<>(2);
            constraints.add(expression.eq(0));
            constraints.add(expression.gte(minimum));
            return constraints;
        }else{
            return Collections.emptyList();
        }
    }

    @Override
    public JsonObject toJson()
    {
        return Json.createObjectBuilder()
                .add("type", JSON_TYPE)
                .add("expression", expression.toJson())
                .add("min", minimum.toString())
                .build();
    }
}
