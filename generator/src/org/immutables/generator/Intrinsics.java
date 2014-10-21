package org.immutables.generator;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Chars;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.util.Arrays;
import org.immutables.generator.Templates.Apply;
import org.immutables.generator.Templates.Binary;
import org.immutables.generator.Templates.Invokable;
import org.immutables.generator.Templates.Invokation;
import org.immutables.generator.Templates.Product;

public final class Intrinsics {
  private Intrinsics() {}

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

  public static <F> Boolean $(Predicate<? super F> predicate, F value) {
    return predicate.apply(value);
  }

  public static <L, R, T> T $(L left, Binary<? super L, ? super R, T> binary, R right) {
    return binary.apply(left, right);
  }

  public static <T> T $(Apply<T> apply, Object... parameters) {
    return apply.apply(parameters);
  }

  public static Product $(Object... parameters) {
    return new Product(parameters);
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

  public static void $(Invokation invokation, Object object, Object... parameters) {
    // TBD do we need this overload
    invokation.out(object).out(parameters);
  }

  public static boolean $if(boolean value) {
    return value;
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

  public static <T> Iterable<T> $in(T[] elements) {
    return Arrays.asList(elements);
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
}
