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

import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import org.immutables.criteria.Criterias;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Query;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Util functions for matchers
 */
public final class Matchers {

  private Matchers() {}

  static List<Expression> concat(Expression existing, Criterion<?> first, Criterion<?> ... rest) {
    Stream<Expression> restStream = Stream.concat(Stream.of(first), Arrays.stream(rest))
            .map(Criterias::toQuery)
            .filter(q -> q.filter().isPresent())
            .map(q -> q.filter().get());

    return Stream.concat(Stream.of(existing), restStream)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

  }

  /**
   * Gets generic type variable of aggregation interface.
   */
  static Type aggregationType(Class<?> from, Class<?> searchFor, String methodName) {
    try {
      final Method method = searchFor.getMethod(methodName);
      Type type = TypeToken.of(from).resolveType(method.getGenericReturnType()).getType();
      if (!(type instanceof ParameterizedType)) {
        throw new IllegalArgumentException(String.format("Expected %s for method %s but got %s", ParameterizedType.class.getSimpleName(), searchFor.getSimpleName() + "." + methodName, type));
      }
      ParameterizedType parameterized = (ParameterizedType) type;
      if (parameterized.getRawType() != Aggregation.class) {
        throw new IllegalArgumentException(String.format("Expected %s but got %s", Aggregation.class, parameterized.getRawType()));
      }

      if (parameterized.getActualTypeArguments().length != 1) {
        throw new IllegalArgumentException(String.format("Expected single type parameter for %s got %d", parameterized, parameterized.getActualTypeArguments().length));
      }

      return parameterized.getActualTypeArguments()[0];
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  public static Expression toExpression(Projection<?> projection) {
    Objects.requireNonNull(projection, "projection");
    CriteriaContext context = extract((Matcher) projection);
    Expression expression = context.expression();
    // ugly hack for now
    if (expression instanceof DnfExpression) {
      return context.path();
    }
    return context.expression();
  }

  public static Expression toExpression(Aggregation<?> aggregation) {
    Objects.requireNonNull(aggregation, "aggregation");
    return extract((Matcher) aggregation).expression();
  }

  public static CriteriaContext extract(Criterion<?> criterion) {
    Objects.requireNonNull(criterion, "criterion");

    if (criterion instanceof AbstractContextHolder) {
      return ((AbstractContextHolder) criterion).context();
    }

    throw new IllegalArgumentException(String.format("%s does not implement %s", criterion.getClass().getName(),
            AbstractContextHolder.class.getSimpleName()));
  }
  /**
   * Extracts criteria context from an arbitrary object.
   * @see AbstractContextHolder
   */
  public static CriteriaContext extract(Matcher object) {
    Objects.requireNonNull(object, "object");

    if (object instanceof AbstractContextHolder) {
      return ((AbstractContextHolder) object).context();
    }

    throw new IllegalArgumentException(String.format("%s does not implement %s", object.getClass().getName(),
            AbstractContextHolder.class.getSimpleName()));
  }

  static <C> UnaryOperator<Expression> toInnerExpression(CriteriaContext context, UnaryOperator<C> expr) {
    return expression -> {
      final CriteriaContext newContext = context.nested();
      final C initial = (C) newContext.creator().create(newContext);
      final C changed = expr.apply(initial);
      return Matchers.extract((Matcher) changed).query().filter().orElseThrow(() -> new IllegalStateException("filter should be set"));
    };
  }

}
