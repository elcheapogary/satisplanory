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

import io.github.elcheapogary.satisplanory.util.function.throwing.ThrowingCallable;
import io.github.elcheapogary.satisplanory.util.function.throwing.ThrowingFunction;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class Try
{
    private Try()
    {
    }

    public static <X extends Throwable> Catching1<X> catching(Class<X> exceptionClass)
    {
        return new Catching1<>(exceptionClass);
    }

    private static class Accumulation<A>
    {
        private final A value;
        private Throwable throwable;

        public Accumulation(A value)
        {
            this.value = value;
        }
    }

    public static class Catching1<X extends Throwable>
    {
        private final Class<X> exceptionClass;

        private Catching1(Class<X> exceptionClass)
        {
            this.exceptionClass = exceptionClass;
        }

        public <V> ReturnValue.Throwing1<V, X> call(ThrowingCallable<? extends V, ? extends X> callable)
        {
            try {
                return ReturnValue.Throwing1.success(callable.call());
            }catch (Throwable e){
                return ReturnValue.Throwing1.failure(exceptionClass, e);
            }
        }

        public <T, A, R> Collector<ReturnValue.Throwing1<T, X>, ?, ReturnValue.Throwing1<R, X>> collector(Collector<T, A, R> collector)
        {
            return new Collector<ReturnValue.Throwing1<T, X>, Accumulation<A>, ReturnValue.Throwing1<R, X>>()
            {
                @Override
                public BiConsumer<Accumulation<A>, ReturnValue.Throwing1<T, X>> accumulator()
                {
                    return (a, t) -> {
                        if (a.throwable != null){
                            if (t.isFailure()){
                                a.throwable.addSuppressed(t.getThrowable());
                            }
                        }else if (t.isFailure()){
                            a.throwable = t.getThrowable();
                        }else{
                            try {
                                collector.accumulator().accept(a.value, t.getOrThrow());
                            }catch (Throwable e){
                                a.throwable = e;
                            }
                        }
                    };
                }

                @Override
                public Set<Characteristics> characteristics()
                {
                    Set<Characteristics> s = EnumSet.noneOf(Characteristics.class);
                    s.addAll(collector.characteristics());
                    s.remove(Characteristics.IDENTITY_FINISH);
                    return s;
                }

                @Override
                public BinaryOperator<Accumulation<A>> combiner()
                {
                    return (a, b) -> {
                        if (a.throwable != null){
                            if (b.throwable != null){
                                a.throwable.addSuppressed(b.throwable);
                            }
                            return a;
                        }else if (b.throwable != null){
                            return b;
                        }else{
                            return new Accumulation<>(collector.combiner().apply(a.value, b.value));
                        }
                    };
                }

                @Override
                public Function<Accumulation<A>, ReturnValue.Throwing1<R, X>> finisher()
                {
                    return a -> {
                        if (a.throwable != null){
                            return ReturnValue.Throwing1.failure(exceptionClass, a.throwable);
                        }else{
                            return ReturnValue.Throwing1.success(collector.finisher().apply(a.value));
                        }
                    };
                }

                @Override
                public Supplier<Accumulation<A>> supplier()
                {
                    return () -> new Accumulation<>(collector.supplier().get());
                }
            };
        }

        public <T, R> Function<T, ReturnValue.Throwing1<R, X>> function(ThrowingFunction<T, R, X> function)
        {
            return t -> call(() -> function.apply(t));
        }
    }
}
