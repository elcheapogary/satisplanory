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
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

public class BigFraction
        extends Number
        implements Comparable<BigFraction>
{
    public static final BigFraction NEGATIVE_ONE = new BigFraction(BigInteger.ONE.negate(), BigInteger.ONE, true)
    {
        @Override
        public BigFraction abs()
        {
            return BigFraction.ONE;
        }

        @Override
        public int compareTo(BigFraction o)
        {
            if (o == this){
                return 0;
            }
            return super.compareTo(o);
        }

        @Override
        public double doubleValue()
        {
            return -1.0;
        }

        @Override
        public float floatValue()
        {
            return -1F;
        }

        @Override
        public int intValue()
        {
            return -1;
        }

        @Override
        public boolean isInteger()
        {
            return true;
        }

        @Override
        public long longValue()
        {
            return -1L;
        }

        @Override
        public BigFraction multiply(BigFraction multiplicand)
        {
            return multiplicand.negate();
        }

        @Override
        public BigFraction negate()
        {
            return BigFraction.ONE;
        }

        @Override
        public int signum()
        {
            return -1;
        }

        @Override
        public BigFraction simplify()
        {
            return this;
        }

        @Override
        public BigInteger toBigInteger()
        {
            return BigInteger.ONE.negate();
        }

        @Override
        public BigInteger toBigIntegerExact()
        {
            return BigInteger.ONE.negate();
        }
    };

    public static final BigFraction ONE = new BigFraction(BigInteger.ONE, BigInteger.ONE, true)
    {
        @Override
        public BigFraction abs()
        {
            return this;
        }

        @Override
        public byte byteValue()
        {
            return 1;
        }

        @Override
        public int compareTo(BigFraction o)
        {
            if (o == this){
                return 0;
            }
            return super.compareTo(o);
        }

        @Override
        public BigFraction divide(long divisor)
        {
            if (divisor == 0){
                throw new ArithmeticException();
            }else if (divisor == 1){
                return this;
            }
            return BigFraction.valueOf(BigInteger.ONE, BigInteger.valueOf(divisor));
        }

        @Override
        public BigFraction divide(BigFraction divisor)
        {
            return BigFraction.valueOf(divisor.denominator, divisor.numerator);
        }

        @Override
        public double doubleValue()
        {
            return 1.0;
        }

        @Override
        public float floatValue()
        {
            return 1F;
        }

        @Override
        public int intValue()
        {
            return 1;
        }

        @Override
        public boolean isInteger()
        {
            return true;
        }

        @Override
        public long longValue()
        {
            return 1L;
        }

        @Override
        public BigFraction multiply(BigFraction multiplicand)
        {
            return Objects.requireNonNull(multiplicand);
        }

        @Override
        public BigFraction multiply(long multiplicand)
        {
            return BigFraction.valueOf(multiplicand);
        }

        @Override
        public BigFraction multiply(BigInteger multiplicand)
        {
            return BigFraction.valueOf(multiplicand);
        }

        @Override
        public BigFraction negate()
        {
            return NEGATIVE_ONE;
        }

        @Override
        public short shortValue()
        {
            return 1;
        }

        @Override
        public int signum()
        {
            return 1;
        }

        @Override
        public BigFraction simplify()
        {
            return this;
        }

        @Override
        public BigDecimal toBigDecimal(MathContext mathContext)
        {
            return BigDecimal.ONE.setScale(mathContext.getPrecision(), RoundingMode.UNNECESSARY);
        }

        @Override
        public BigDecimal toBigDecimal(int scale, RoundingMode roundingMode)
        {
            return BigDecimal.ONE.setScale(scale, RoundingMode.UNNECESSARY);
        }

        @Override
        public BigInteger toBigInteger()
        {
            return BigInteger.ONE;
        }

        @Override
        public BigInteger toBigIntegerExact()
        {
            return BigInteger.ONE;
        }

        @Override
        public String toString()
        {
            return "1";
        }
    };

    public static final BigFraction ZERO = new BigFraction(BigInteger.ZERO, BigInteger.ONE, true)
    {
        @Override
        public BigFraction abs()
        {
            return this;
        }

        @Override
        public BigFraction add(BigFraction val)
        {
            return Objects.requireNonNull(val);
        }

        @Override
        public BigFraction add(long value)
        {
            return BigFraction.valueOf(value);
        }

        @Override
        public BigFraction add(BigInteger value)
        {
            return valueOf(value);
        }

        @Override
        public byte byteValue()
        {
            return (byte)0;
        }

        @Override
        public int compareTo(BigFraction o)
        {
            return -o.signum();
        }

        @Override
        public BigFraction divide(BigFraction divisor)
        {
            if (equals(Objects.requireNonNull(divisor))){
                throw new ArithmeticException("divide by zero");
            }
            return this;
        }

        @Override
        public BigFraction divide(long divisor)
        {
            if (divisor == 0){
                throw new ArithmeticException("divide by zero");
            }
            return this;
        }

        @Override
        public BigFraction divide(BigInteger divisor)
        {
            Objects.requireNonNull(divisor);
            return this;
        }

        @Override
        public double doubleValue()
        {
            return 0.0;
        }

        @Override
        public float floatValue()
        {
            return 0F;
        }

        @Override
        public int intValue()
        {
            return 0;
        }

        @Override
        public boolean isInteger()
        {
            return true;
        }

        @Override
        public long longValue()
        {
            return 0L;
        }

        @Override
        public BigFraction max(BigFraction other)
        {
            if (other.signum() > 0){
                return other;
            }else{
                return this;
            }
        }

        @Override
        public BigFraction min(BigFraction other)
        {
            if (other.signum() < 0){
                return other;
            }else{
                return this;
            }
        }

        @Override
        public BigFraction movePointLeft(int places)
        {
            return this;
        }

        @Override
        public BigFraction movePointRight(int places)
        {
            return this;
        }

        @Override
        public BigFraction multiply(BigFraction multiplicand)
        {
            Objects.requireNonNull(multiplicand);
            return this;
        }

        @Override
        public BigFraction multiply(long multiplicand)
        {
            return this;
        }

        @Override
        public BigFraction multiply(BigInteger multiplicand)
        {
            Objects.requireNonNull(multiplicand);
            return this;
        }

        @Override
        public BigFraction negate()
        {
            return this;
        }

        @Override
        public short shortValue()
        {
            return 0;
        }

        @Override
        public int signum()
        {
            return 0;
        }

        @Override
        public BigFraction simplify()
        {
            return this;
        }

        @Override
        public BigFraction subtract(BigFraction val)
        {
            return val.negate();
        }

        @Override
        public BigDecimal toBigDecimal(MathContext mathContext)
        {
            return BigDecimal.ZERO.setScale(mathContext.getPrecision(), RoundingMode.UNNECESSARY);
        }

        @Override
        public BigDecimal toBigDecimal(int scale, RoundingMode roundingMode)
        {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.UNNECESSARY);
        }

        @Override
        public BigInteger toBigInteger()
        {
            return BigInteger.ZERO;
        }

        @Override
        public BigInteger toBigIntegerExact()
        {
            return BigInteger.ZERO;
        }

        @Override
        public String toString()
        {
            return "0";
        }
    };

    private final BigInteger numerator;
    private final BigInteger denominator;
    private final boolean simplified;

    private BigFraction(BigInteger numerator, BigInteger denominator, boolean simplified)
    {
        if (denominator.equals(BigInteger.ZERO)){
            throw new ArithmeticException("denominator == 0");
        }
        if (denominator.signum() == -1){
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        this.numerator = numerator;
        this.denominator = denominator;
        this.simplified = simplified;
    }

    private static BigFraction valueOf(BigInteger numerator, BigInteger denominator)
    {
        if (numerator.equals(BigInteger.ZERO)){
            return ZERO;
        }else if (numerator.equals(denominator)){
            return ONE;
        }else if (numerator.equals(denominator.negate())){
            return NEGATIVE_ONE;
        }

        return new BigFraction(numerator, denominator, false);
    }

    public static BigFraction valueOf(BigDecimal decimal)
    {
        BigInteger unscaled = decimal.unscaledValue();
        int scale = decimal.scale();

        if (scale == 0){
            return valueOf(unscaled, BigInteger.ONE);
        }else if (scale < 0){
            unscaled = unscaled.multiply(BigInteger.TEN.pow(Math.abs(scale)));
            return valueOf(unscaled, BigInteger.ONE);
        }else{
            return valueOf(unscaled, BigInteger.TEN.pow(scale)).simplify();
        }
    }

    public static BigFraction valueOf(long value)
    {
        return valueOf(BigInteger.valueOf(value), BigInteger.ONE);
    }

    public static BigFraction valueOf(BigInteger value)
    {
        return valueOf(value, BigInteger.ONE);
    }

    public BigFraction abs()
    {
        return valueOf(numerator.abs(), denominator.abs());
    }

    public BigFraction add(BigFraction val)
    {
        return valueOf(
                this.numerator.multiply(val.denominator).add(val.numerator.multiply(this.denominator)),
                this.denominator.multiply(val.denominator)
        );
    }

    public BigFraction add(long value)
    {
        return add(valueOf(value));
    }

    public BigFraction add(BigInteger value)
    {
        return add(valueOf(value));
    }

    @Override
    public int compareTo(BigFraction o)
    {
        return this.numerator.multiply(o.denominator).compareTo(o.numerator.multiply(this.denominator));
    }

    public BigFraction divide(BigFraction divisor)
    {
        if (divisor.numerator.equals(BigInteger.ZERO)){
            throw new ArithmeticException("divide by zero");
        }
        return valueOf(numerator.multiply(divisor.denominator), denominator.multiply(divisor.numerator));
    }

    public BigFraction divide(long divisor)
    {
        return divide(valueOf(divisor));
    }

    public BigFraction divide(BigInteger divisor)
    {
        return divide(valueOf(divisor));
    }

    @Override
    public double doubleValue()
    {
        /*
         * Simplify first for better accuracy
         */
        BigFraction simplified = simplify();
        return simplified.numerator.doubleValue() / simplified.denominator.doubleValue();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof BigFraction that)) return false;
        return compareTo(that) == 0;
    }

    @Override
    public float floatValue()
    {
        return (float)doubleValue();
    }

    @Override
    public int hashCode()
    {
        BigFraction simplified = simplify();
        return Objects.hash(simplified.numerator, simplified.denominator);
    }

    @Override
    public int intValue()
    {
        return (int)doubleValue();
    }

    public boolean isInteger()
    {
        BigFraction simplified = simplify();

        /*
         * Yes, I mean to test first identity then equality
         */
        return simplified.denominator == BigInteger.ONE || simplified.denominator.equals(BigInteger.ONE);
    }

    @Override
    public long longValue()
    {
        return (long)doubleValue();
    }

    public BigFraction max(BigFraction other)
    {
        if (this.compareTo(other) < 0){
            return other;
        }

        return this;
    }

    public BigFraction min(BigFraction other)
    {
        if (this.compareTo(other) > 0){
            return other;
        }

        return this;
    }

    public BigFraction movePointLeft(int places)
    {
        if (places == 0){
            return this;
        }else if (places > 0){
            return divide(BigInteger.TEN.pow(places));
        }else{
            return multiply(BigInteger.TEN.pow(places * -1));
        }
    }

    public BigFraction movePointRight(int places)
    {
        if (places == 0){
            return this;
        }else if (places > 0){
            return multiply(BigInteger.TEN.pow(places));
        }else{
            return divide(BigInteger.TEN.pow(places * -1));
        }
    }

    public BigFraction multiply(BigFraction multiplicand)
    {
        return valueOf(numerator.multiply(multiplicand.numerator), denominator.multiply(multiplicand.denominator));
    }

    public BigFraction multiply(long multiplicand)
    {
        return multiply(valueOf(multiplicand));
    }

    public BigFraction multiply(BigInteger multiplicand)
    {
        return multiply(valueOf(multiplicand));
    }

    public BigFraction negate()
    {
        return valueOf(numerator.negate(), denominator);
    }

    public int signum()
    {
        return numerator.signum();
    }

    public BigFraction simplify()
    {
        /*
         * Yes, I mean to compare identity not equality here
         */
        if (simplified || denominator == BigInteger.ONE){
            return this;
        }

        BigInteger n = numerator;

        /*
         * Yes, I mean to compare identity then equality here
         */
        if (n == BigInteger.ZERO || n.equals(BigInteger.ZERO)){
            return BigFraction.ZERO;
        }

        BigInteger d = denominator;

        BigInteger gcd = n.gcd(d);

        if (gcd.compareTo(BigInteger.ONE) > 0){
            n = n.divide(gcd);
            d = d.divide(gcd);
        }

        if (n.equals(d)){
            return BigFraction.ONE;
        }

        if (n.signum() == 0){
            return ZERO;
        }

        return new BigFraction(n, d, true);
    }

    public BigFraction subtract(BigFraction val)
    {
        return valueOf(
                this.numerator.multiply(val.denominator).subtract(val.numerator.multiply(this.denominator)),
                this.denominator.multiply(val.denominator)
        );
    }

    public BigFraction subtract(long value)
    {
        return subtract(valueOf(value));
    }

    public BigFraction subtract(BigInteger value)
    {
        return subtract(valueOf(value));
    }

    public BigDecimal toBigDecimal(int scale, RoundingMode roundingMode)
    {
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), scale, roundingMode);
    }

    public BigDecimal toBigDecimal(MathContext mathContext)
    {
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), mathContext);
    }

    public BigInteger toBigInteger()
    {
        return numerator.divide(denominator);
    }

    public BigInteger toBigIntegerExact()
    {
        BigInteger[] a = numerator.divideAndRemainder(denominator);
        if (a[1].signum() != 0){
            throw new ArithmeticException();
        }
        return a[0];
    }

    @Override
    public String toString()
    {
        if (denominator.equals(BigInteger.ONE)){
            return numerator.toString();
        }else{
            return numerator.toString() + "/" + denominator;
        }
    }
}
