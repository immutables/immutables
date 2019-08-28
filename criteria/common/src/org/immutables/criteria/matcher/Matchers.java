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
import org.immutables.criteria.Criterias;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Query;

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
   * Gets generic type variable of an interface at runtime using reflection API.
   */
  static Type genericTypeOf(Object from, Class<?> searchFor) {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(searchFor, "searchFor");
    Preconditions.checkArgument(searchFor.isInterface(), "%s not an interface", searchFor);
    final Class<?> start = from.getClass();
    Set<Class<?>> visited = new HashSet<>();
    Deque<Class<?>> toVisit = new ArrayDeque<>();
    toVisit.push(start);
    while (!toVisit.isEmpty()) {
      Class<?> type = toVisit.pop();
      if (Arrays.asList(type.getInterfaces()).contains(searchFor)) {
        for (Type genericType: type.getGenericInterfaces()) {
          if ((genericType instanceof ParameterizedType) && ((ParameterizedType) genericType).getRawType().equals(searchFor)) {
            ParameterizedType parameterized = (ParameterizedType) genericType;
            Type[] args = parameterized.getActualTypeArguments();
            if (args.length != 1) {
              throw new IllegalArgumentException(String.format("While extracting generic type of %s. Expected single generic variable for %s but got %d: %s", searchFor, genericType, args.length, Arrays.asList(args)));
            }
            return args[0];
          }
        }
      }

      // continue searching
      if (visited.add(type)) {
        Set<Class<?>> candidates = new HashSet<>(Arrays.asList(type.getInterfaces()));
        candidates.removeAll(visited);
        toVisit.addAll(candidates);
      }
    }

    throw new IllegalArgumentException(String.format("Couldn't find %s in type-hierarchy of %s", searchFor, start));
  }

  public static Expression toExpression(Projection<?> projection) {
    Objects.requireNonNull(projection, "projection");
    return extract((Matcher) projection).path();
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
