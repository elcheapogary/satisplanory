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

import java.util.Comparator;
import java.util.function.Predicate;

public class Comparators
{
    private Comparators()
    {
    }

    public static <T> Comparator<T> sortFirst(Predicate<? super T> predicate)
    {
        return (o1, o2) -> {
            if (predicate.test(o1)){
                if (predicate.test(o2)){
                    return 0;
                }else{
                    return -1;
                }
            }else if (predicate.test(o2)){
                return 1;
            }else{
                return 0;
            }
        };
    }

    public static <T> Comparator<T> sortLast(Predicate<? super T> predicate)
    {
        return Comparators.<T>sortFirst(predicate).reversed();
    }
}
