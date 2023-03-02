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
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class Constraint
{
    private final Expression expression;
    private final Comparison comparison;

    Constraint(Expression expression, Comparison comparison)
    {
        this.expression = expression;
        this.comparison = comparison;
    }

    static Constraint fromJson(JsonObject json, List<? extends DecisionVariable> decisionVariables)
    {
        Comparison comparison = Comparison.valueOf(json.getString("cmp"));
        Expression expression = Expression.fromJson(json, decisionVariables);
        return new Constraint(expression, comparison);
    }

    Comparison getComparison()
    {
        return comparison;
    }

    Expression getExpression()
    {
        return expression;
    }

    public JsonObject toJson()
    {
        JsonObjectBuilder b = Json.createObjectBuilder();
        b.add("cmp", comparison.name());
        expression.toJson(b);
        return b.build();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        Expression tmpExpression = expression;
        BigFraction constant = expression.getConstantValue();

        if (constant.signum() != 0){
            tmpExpression = tmpExpression.subtract(constant);
            constant = constant.negate();
        }

        tmpExpression.appendToStringBuilder(sb);

        sb.append(" ");

        sb.append(switch (comparison){
            case EQ -> "==";
            case GTE -> ">=";
            case LTE -> "<=";
        });

        sb.append(" ");
        sb.append(constant.toString());

        return sb.toString();
    }

    enum Comparison
    {
        LTE, GTE, EQ;
    }
}
