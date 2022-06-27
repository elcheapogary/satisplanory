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
    protected final BigInteger numerator;
    protected final BigInteger denominator;

    private BigFraction(BigInteger numerator, BigInteger denominator)
    {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public static BigFraction negativeOne()
    {
        return NegativeOne.INSTANCE;
    }

    public static BigFraction one()
    {
        return One.INSTANCE;
    }

    public static BigFraction valueOf(long value)
    {
        return IntegerBigFraction.valueOfInteger(value);
    }

    public static BigFraction valueOf(BigInteger value)
    {
        return IntegerBigFraction.valueOfInteger(value);
    }

    private static BigFraction valueOf(BigInteger numerator, BigInteger denominator)
    {
        Objects.requireNonNull(numerator);
        Objects.requireNonNull(denominator);

        if (denominator.signum() == 0){
            throw new ArithmeticException();
        }

        if (numerator.signum() == 0){
            return zero();
        }

        if (denominator.signum() < 0){
            numerator = numerator.negate();
            denominator = denominator.negate();
        }

        if (numerator.equals(denominator)){
            return BigFraction.one();
        }else if (numerator.equals(denominator.negate())){
            return BigFraction.negativeOne();
        }

        if (denominator.equals(BigInteger.ONE)){
            return new IntegerBigFraction(numerator);
        }

        BigInteger gcd = numerator.gcd(denominator);

        if (gcd.compareTo(BigInteger.ONE) > 0){
            numerator = numerator.divide(gcd);
            denominator = denominator.divide(gcd);

            if (denominator.equals(BigInteger.ONE)){
                return new IntegerBigFraction(numerator);
            }
        }

        return new BigFraction(numerator, denominator);
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
            return valueOf(unscaled, BigInteger.TEN.pow(scale));
        }
    }

    public static BigFraction zero()
    {
        return Zero.INSTANCE;
    }

    public BigFraction abs()
    {
        if (signum() >= 0){
            return this;
        }
        return valueOf(numerator.abs(), denominator.abs());
    }

    public BigFraction add(BigFraction addend)
    {
        return valueOf(
                this.numerator.multiply(addend.denominator).add(addend.numerator.multiply(this.denominator)),
                this.denominator.multiply(addend.denominator)
        );
    }

    public BigFraction add(long addend)
    {
        return add(valueOf(addend));
    }

    public BigFraction add(BigInteger addend)
    {
        return add(valueOf(addend));
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
        return numerator.doubleValue() / denominator.doubleValue();
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
        return Objects.hash(numerator, denominator);
    }

    @Override
    public int intValue()
    {
        return (int)doubleValue();
    }

    public boolean isInteger()
    {
        return denominator.equals(BigInteger.ONE);
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
        return numerator.toString() + "/" + denominator;
    }

    private static class IntegerBigFraction
            extends BigFraction
    {
        public IntegerBigFraction(BigInteger numerator)
        {
            super(numerator, BigInteger.ONE);
        }

        public static BigFraction valueOfInteger(long value)
        {
            if (value == 0){
                return zero();
            }else if (value == 1){
                return one();
            }else if (value == -1){
                return negativeOne();
            }

            return new IntegerBigFraction(BigInteger.valueOf(value));
        }

        public static BigFraction valueOfInteger(BigInteger value)
        {
            Objects.requireNonNull(value);

            if (value.signum() == 0){
                return zero();
            }else if (value.equals(BigInteger.ONE)){
                return one();
            }else if (value.equals(BigInteger.ONE.negate())){
                return negativeOne();
            }

            return new IntegerBigFraction(value);
        }

        @Override
        public BigFraction add(BigInteger addend)
        {
            return valueOfInteger(numerator.add(addend));
        }

        @Override
        public byte byteValue()
        {
            return numerator.byteValue();
        }

        @Override
        public BigFraction divide(BigInteger divisor)
        {
            return BigFraction.valueOf(numerator, divisor);
        }

        @Override
        public double doubleValue()
        {
            return numerator.doubleValue();
        }

        @Override
        public float floatValue()
        {
            return numerator.floatValue();
        }

        @Override
        public int intValue()
        {
            return numerator.intValue();
        }

        @Override
        public boolean isInteger()
        {
            return true;
        }

        @Override
        public long longValue()
        {
            return numerator.longValue();
        }

        @Override
        public BigFraction negate()
        {
            return valueOfInteger(numerator.negate());
        }

        @Override
        public short shortValue()
        {
            return numerator.shortValue();
        }

        @Override
        public BigFraction subtract(BigInteger value)
        {
            return valueOfInteger(numerator.subtract(value));
        }

        @Override
        public BigDecimal toBigDecimal(int scale, RoundingMode roundingMode)
        {
            return new BigDecimal(numerator).setScale(scale, roundingMode);
        }

        @Override
        public BigDecimal toBigDecimal(MathContext mathContext)
        {
            return new BigDecimal(numerator, mathContext);
        }

        @Override
        public BigInteger toBigInteger()
        {
            return numerator;
        }

        @Override
        public BigInteger toBigIntegerExact()
        {
            return numerator;
        }

        @Override
        public String toString()
        {
            return numerator.toString();
        }
    }

    private static class NegativeOne
            extends IntegerBigFraction
    {
        public static final NegativeOne INSTANCE = new NegativeOne();

        private NegativeOne()
        {
            super(BigInteger.ONE.negate());
        }

        @Override
        public BigFraction abs()
        {
            return BigFraction.one();
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
            return BigFraction.one();
        }

        @Override
        public int signum()
        {
            return -1;
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
    }

    private static class One
            extends IntegerBigFraction
    {
        public static final One INSTANCE = new One();

        private One()
        {
            super(BigInteger.ONE);
        }

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
            return negativeOne();
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
    }

    private static class Zero
            extends IntegerBigFraction
    {
        public static final Zero INSTANCE = new Zero();

        private Zero()
        {
            super(BigInteger.ZERO);
        }

        @Override
        public BigFraction abs()
        {
            return this;
        }

        @Override
        public BigFraction add(BigFraction addend)
        {
            return Objects.requireNonNull(addend);
        }

        @Override
        public BigFraction add(long addend)
        {
            return BigFraction.valueOf(addend);
        }

        @Override
        public BigFraction add(BigInteger addend)
        {
            return valueOf(addend);
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
    }
}
