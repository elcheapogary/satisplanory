/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.util.function.throwing.tryreturn;

import java.lang.reflect.UndeclaredThrowableException;

public class ReturnValue
{
    public static abstract class Throwing1<V, X extends Throwable>
    {
        public static <V, X extends Throwable> Throwing1<V, X> failure(Class<X> exceptionClass, Throwable throwable)
        {
            return new Failure<>(exceptionClass, throwable);
        }

        public static <V, X extends Throwable> Throwing1<V, X> success(V value)
        {
            return new Success<>(value);
        }

        public abstract V getOrThrow()
                throws X;

        public abstract Throwable getThrowable();

        public abstract boolean isFailure();

        private static class Failure<V, X extends Throwable>
                extends Throwing1<V, X>
        {
            private final Class<X> exceptionClass;
            private final Throwable throwable;

            public Failure(Class<X> exceptionClass, Throwable throwable)
            {
                this.exceptionClass = exceptionClass;
                this.throwable = throwable;
            }

            @Override
            public V getOrThrow()
                    throws X
            {
                if (throwable instanceof RuntimeException r){
                    throw r;
                }else if (throwable instanceof Error e){
                    throw e;
                }else if (exceptionClass.isAssignableFrom(throwable.getClass())){
                    throw exceptionClass.cast(throwable);
                }else{
                    throw new UndeclaredThrowableException(throwable);
                }
            }

            @Override
            public Throwable getThrowable()
            {
                return throwable;
            }

            @Override
            public boolean isFailure()
            {
                return true;
            }
        }

        private static class Success<V, X extends Throwable>
                extends Throwing1<V, X>
        {
            private final V value;

            public Success(V value)
            {
                this.value = value;
            }

            @Override
            public V getOrThrow()
            {
                return value;
            }

            @Override
            public Throwable getThrowable()
            {
                return null;
            }

            @Override
            public boolean isFailure()
            {
                return false;
            }
        }
    }
}
