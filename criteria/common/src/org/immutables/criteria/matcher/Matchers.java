/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.matcher;

import org.immutables.criteria.Criterias;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.expression.Expression;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Matchers {

  private Matchers() {}

  public static <R> BooleanMatcher<R> booleanMatcher(CriteriaContext context) {
    Objects.requireNonNull(context, "context");
    class Local implements BooleanMatcher.Self, HasContext {
      @Override
      public CriteriaContext context() {
        return context;
      }
    }

    return (BooleanMatcher<R>) new Local();
  }

  public static <R> StringMatcher<R> stringMatcher(CriteriaContext context) {
    Objects.requireNonNull(context, "context");
    class Local implements StringMatcher.Self, HasContext {
      @Override
      public CriteriaContext context() {
        return context;
      }
    }

    return (StringMatcher<R>) new Local();
  }

  public static <R, V extends Comparable<V>> ComparableMatcher<R, V> comparableMatcher(CriteriaContext context) {
    Objects.requireNonNull(context, "context");
    class Local implements ComparableMatcher.Self, HasContext {
      @Override
      public CriteriaContext context() {
        return context;
      }
    }
    return (ComparableMatcher<R, V>) new Local();
  }

  public static <R, V> ObjectMatcher<R, V> objectMatcher(CriteriaContext context) {
    Objects.requireNonNull(context, "context");
    class Local implements ObjectMatcher.Self<V>, HasContext {
      @Override
      public CriteriaContext context() {
        return context;
      }
    }
    return (ObjectMatcher<R, V>) new Local();
  }

  public static <R, S, C> OptionalMatcher<R, S> optionalMatcher(CriteriaContext context) {
    Objects.requireNonNull(context, "context");
    class Local implements OptionalMatcher, HasContext {
      @Override
      public CriteriaContext context() {
        return context;
      }
    }
    return new Local();
  }

  public static <R, S, C, V> IterableMatcher<R, S, V> collectionMatcher(CriteriaContext context) {
    Objects.requireNonNull(context, "context");

    class Local implements IterableMatcher.Self, HasContext {
      @Override
      public CriteriaContext context() {
        return context;
      }
    }

    return (IterableMatcher<R, S, V>) new Local();
  }

  public static <T> T create(Class<T> type, CriteriaContext context) {
    Objects.requireNonNull(type, "type");

    if (BooleanMatcher.class.isAssignableFrom(type)) {
      return (T) booleanMatcher(context);
    } else if (StringMatcher.class.isAssignableFrom(type)) {
      return (T) stringMatcher(context);
    } else if (ComparableMatcher.class.isAssignableFrom(type)) {
      return (T) comparableMatcher(context);
    } else if (ObjectMatcher.class.isAssignableFrom(type)) {
      return (T) objectMatcher(context);
    } else if (OptionalMatcher.class.isAssignableFrom(type)) {
      return (T) optionalMatcher(context);
    } else if (IterableMatcher.class.isAssignableFrom(type)) {
      return (T) collectionMatcher(context);
    }

    throw new UnsupportedOperationException("Don't know how to create matcher " + type.getName());
  }

  static List<Expression> concatFilters(Expression existing, Criterion<?> first, Criterion<?> ... rest) {
    Stream<Expression> restStream = Stream.concat(Stream.of(first), Arrays.stream(rest))
            .map(Criterias::toQuery)
            .filter(q -> q.filter().isPresent())
            .map(q -> q.filter().get());

    return Stream.concat(Stream.of(existing), restStream)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

  }

  /**
   * Extracts criteria context from an arbitrary object.
   * @see HasContext
   */
  static CriteriaContext extract(Object object) {
    Objects.requireNonNull(object, "object");

    if (object instanceof HasContext) {
      return ((HasContext) object).context();
    }

    throw new IllegalArgumentException(String.format("%s does not implement %s", object.getClass().getName(),
            HasContext.class.getSimpleName()));
  }

  static <C> UnaryOperator<Expression> toExpressionOperator(Supplier<C> supplier, UnaryOperator<C> expr) {
    return expression -> {
      final C initial = supplier.get();
      final C changed = expr.apply(initial);
      return Matchers.extract(changed).query().filter().orElseThrow(() -> new IllegalStateException("filter should be set"));
    };
  }

  static <C> UnaryOperator<Expression> toInnerExpression(CriteriaContext context, UnaryOperator<C> expr) {
    return expression -> {
      final CriteriaContext newContext = context.newChild();
      final C initial = (C) newContext.factory().createRoot();
      final C changed = expr.apply(initial);
      return Matchers.extract(changed).query().filter().orElseThrow(() -> new IllegalStateException("filter should be set"));
    };
  }

}
