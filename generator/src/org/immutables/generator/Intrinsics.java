/*
   Copyright 2014 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.generator;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Chars;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.util.ArrayList;
import java.util.Collection;
import org.immutables.generator.Templates.Apply;
import org.immutables.generator.Templates.Binary;
import org.immutables.generator.Templates.Invokable;
import org.immutables.generator.Templates.Invokation;
import org.immutables.generator.Templates.Product;

public final class Intrinsics {
  private Intrinsics() {}

  @SuppressWarnings("unchecked")
  public static <T> T $cast(Object object) {
    return (T) object;
  }

  /**
   * Effect of using this method is boxing in calling code.
   * @param value value
   * @param <T> result type
   * @return the same value.
   */
  public static <T> T $(T value) {
    return value;
  }

  public static <F, T> T $(Function<? super F, T> unary, F value) {
    return unary.apply(value);
  }

  public static <F> boolean $(Predicate<? super F> predicate, F value) {
    return predicate.apply(value);
  }

  public static <L, R, T> T $(L left, Binary<? super L, ? super R, T> binary, R right) {
    return binary.apply(left, right);
  }

  public static <T> T $(Apply<T> apply, Object... parameters) {
    return apply.apply(parameters);
  }

  public static String $(String a, String b) {
    return a.concat(b);
  }

  public static String $(String a, String b, String c) {
    return a.concat(b).concat(c);
  }

  public static Product $(Object... parameters) {
    return new Product(parameters);
  }
  
  @SafeVarargs
  public static <T> Iterable<T> $(Iterable<? extends T>... iterables) {
    return Iterables.concat(iterables);
  }

  public static <F> void $(Invokation invokation, Predicate<? super F> predicate, F value) {
    invokation.out($(predicate, value));
  }

  public static <F> void $(Invokation invokation, Function<? super F, ?> unary, F value) {
    invokation.out($(unary, value));
  }

  public static <A, B> void $(
      Invokation invokation,
      A left,
      Binary<? super A, ? super B, ?> binary,
      B right) {
    invokation.out($(left, binary, right));
  }

  public static void $(Invokation invokation, Apply<?> apply, Object... parameters) {
    invokation.out($(apply, parameters));
  }

  public static void $(Invokation invokation, Invokable invokable, Object... parameters) {
    invokable.invoke(invokation, parameters);
  }

/*  public static void $(Invokation invokation, Object object, Object... parameters) {
    // TBD do we need this overload
    invokation.out(object).out(parameters);
  }
*/
  public static void $(Invokation invokation, Object object) {
    invokation.out(object);
  }

  public static void $(Invokation invokation, String string) {
    invokation.out(string);
  }

  public static boolean $if(boolean value) {
    return value;
  }

  public static boolean $if(Boolean value) {
    return value == Boolean.TRUE;
  }

  public static boolean $if(String value) {
    return value != null && !value.isEmpty();
  }

  public static boolean $if(Collection<?> value) {
    return value != null && !value.isEmpty();
  }

  public static boolean $if(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof Boolean) {
      return ((Boolean) value).booleanValue();
    }
    if (value instanceof String) {
      return !((String) value).isEmpty();
    }
    if (value instanceof Iterable<?>) {
      return !Iterables.isEmpty((Iterable<?>) value);
    }
    if (value instanceof Number) {
      return ((Number) value).intValue() != 0;
    }
    if (value instanceof Optional<?>) {
      return ((Optional<?>) value).isPresent();
    }
    return true;
  }

  public static <T> Iterable<T> $in(Iterable<T> iterable) {
    return iterable;
  }

  public static <T> Iterable<T> $in(Optional<T> optional) {
    return optional.asSet();
  }

  public static <T> T[] $in(T[] elements) {
    return elements;
  }

  public static Iterable<Character> $in(char[] elements) {
    return Chars.asList(elements);
  }

  public static Iterable<Integer> $in(int[] elements) {
    return Ints.asList(elements);
  }

  public static Iterable<Long> $in(long[] elements) {
    return Longs.asList(elements);
  }

  public static <E> Collection<E> $collect() {
    return new ArrayList<>();
  }
}
