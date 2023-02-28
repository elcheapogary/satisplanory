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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MathExpression
{
    private final String expression;
    private final BigFraction value;

    private MathExpression(String expression, BigFraction value)
    {
        this.expression = expression;
        this.value = value;
    }

    public static BigFraction evaluate(String s)
    {
        return evaluate(new Parser(s).readTerms());
    }

    private static BigFraction evaluate(List<? extends Term> terms)
    {
        Iterator<? extends Term> it = terms.iterator();

        if (!it.hasNext()){
            throw new IllegalArgumentException();
        }

        List<Term> addList = new LinkedList<>();

        Term current = it.next();

        while (it.hasNext()){
            Term second = it.next();

            if (second.operator == Operator.MULTIPLY){
                BigFraction v = current.value().getBigFraction().multiply(second.value.getBigFraction());
                current = new Term(current.operator, new BigFractionValue(v));
            }else if (second.operator == Operator.DIVIDE){
                BigFraction v = current.value().getBigFraction().divide(second.value.getBigFraction());
                current = new Term(current.operator, new BigFractionValue(v));
            }else{
                addList.add(current);
                current = second;
            }
        }

        addList.add(current);

        BigFraction retv = BigFraction.zero();

        for (Term term : addList){
            if (term.operator == Operator.ADD){
                retv = retv.add(term.value().getBigFraction());
            }else if (term.operator == Operator.SUBTRACT){
                retv = retv.subtract(term.value().getBigFraction());
            }else{
                throw new IllegalStateException();
            }
        }

        return retv;
    }

    public static MathExpression parse(String s)
    {
        BigFraction value = evaluate(s);
        return new MathExpression(s, value);
    }

    public static MathExpression valueOf(long v)
    {
        return new MathExpression(Long.toString(v), BigFraction.valueOf(v));
    }

    public static MathExpression valueOf(BigDecimal d)
    {
        return new MathExpression(d.toString(), BigFraction.valueOf(d));
    }

    public String getExpression()
    {
        return expression;
    }

    public BigFraction getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return expression;
    }

    private enum Operator
    {
        ADD, SUBTRACT, MULTIPLY, DIVIDE
    }

    private static class BigFractionValue
            extends Value
    {
        private final BigFraction value;

        public BigFractionValue(BigFraction value)
        {
            this.value = value;
        }

        @Override
        public BigFraction getBigFraction()
        {
            return value;
        }
    }

    private static class BracketValue
            extends Value
    {
        private final List<? extends Term> terms;

        public BracketValue(List<? extends Term> terms)
        {
            this.terms = terms;
        }

        @Override
        public BigFraction getBigFraction()
        {
            return evaluate(terms);
        }
    }

    private static class IntegerValue
            extends Value
    {
        private final BigInteger integer;

        public IntegerValue(BigInteger integer)
        {
            this.integer = integer;
        }

        @Override
        public BigFraction getBigFraction()
        {
            return BigFraction.valueOf(integer);
        }
    }

    private static class NegativeValue
            extends Value
    {
        private final Value value;

        public NegativeValue(Value value)
        {
            this.value = value;
        }

        @Override
        public BigFraction getBigFraction()
        {
            return value.getBigFraction().negate();
        }
    }

    private static class Parser
    {
        private final String input;
        private int pos = 0;

        public Parser(String input)
        {
            this.input = input;
        }

        public boolean isNotEndOfInput()
        {
            return pos < input.length();
        }

        private BracketValue readBracketValue()
        {
            List<Term> terms = new LinkedList<>();

            skipWhiteSpace();

            terms.add(new Term(Operator.ADD, readValue()));

            skipWhiteSpace();

            while (isNotEndOfInput()){
                if (input.charAt(pos) == ')'){
                    pos++;
                    break;
                }
                Operator operator = readOperator();
                skipWhiteSpace();
                Value value = readValue();
                skipWhiteSpace();
                terms.add(new Term(operator, value));
            }

            return new BracketValue(terms);
        }

        private void readOneOrMoreDigits(StringBuilder sb)
        {
            int ds = pos;
            while (isNotEndOfInput()){
                char c = input.charAt(pos);

                if (Character.isDigit(c)){
                    sb.append(c);
                    pos++;
                }else{
                    break;
                }
            }
            if (pos == ds){
                throw new NumberFormatException();
            }
        }

        public Operator readOperator()
        {
            char c = input.charAt(pos);

            pos++;

            return switch (c){
                case '+' -> Operator.ADD;
                case '-' -> Operator.SUBTRACT;
                case '*' -> Operator.MULTIPLY;
                case '/' -> Operator.DIVIDE;
                case '(' -> {
                    pos--;
                    yield Operator.MULTIPLY;
                }
                default -> throw new NumberFormatException();
            };
        }

        public List<? extends Term> readTerms()
        {
            List<Term> terms = new LinkedList<>();

            skipWhiteSpace();

            terms.add(new Term(Operator.ADD, readValue()));

            skipWhiteSpace();

            while (isNotEndOfInput()){
                Operator operator = readOperator();
                skipWhiteSpace();
                Value value = readValue();
                skipWhiteSpace();
                terms.add(new Term(operator, value));
            }

            return terms;
        }

        public Value readValue()
        {
            StringBuilder sb = new StringBuilder();

            if (input.charAt(pos) == '-'){
                sb.append("-");
                pos++;
                skipWhiteSpace();
            }

            if (input.charAt(pos) == '.'){
                sb.append("0.");
                pos++;
                readOneOrMoreDigits(sb);
                return new BigFractionValue(BigFraction.valueOf(new BigDecimal(sb.toString())));
            }else if (input.charAt(pos) == '('){
                pos++;
                Value v = readBracketValue();
                if (!sb.isEmpty()){
                    v = new NegativeValue(v);
                }
                return v;
            }

            readOneOrMoreDigits(sb);

            if (isNotEndOfInput() && input.charAt(pos) == '.'){
                sb.append(".");
                pos++;
                readOneOrMoreDigits(sb);
                return new BigFractionValue(BigFraction.valueOf(new BigDecimal(sb.toString())));
            }else{
                return new IntegerValue(new BigInteger(sb.toString()));
            }
        }

        public void skipWhiteSpace()
        {
            while (isNotEndOfInput()){
                if (input.charAt(pos) == ' '){
                    pos++;
                }else{
                    break;
                }
            }
        }
    }

    private record Term(Operator operator, Value value)
    {
    }

    private static abstract class Value
    {
        public abstract BigFraction getBigFraction();
    }
}
