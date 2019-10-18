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

package org.immutables.criteria.inmemory;

import org.immutables.criteria.Criterias;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.expression.Expression;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Factory and util methods to be used with {@code InMemory} backend
 */
public final class InMemory {

  /**
   * Converts criteria to a predicate. Note that filter must be set on criteria.
   * For instance {@code person.name.is("John")} is a valid filter but {@code person.name} is not.
   *
   * @throws NullPointerException if argument is null
   * @throws IllegalArgumentException if filter is not set on criteria
   * @return predicate which reflects criteria filter expression
   */
  public static <T> Predicate<T> toPredicate(Criterion<T> criterion) {
    Objects.requireNonNull(criterion, "criterion");
    Expression expression = Criterias.toQuery(criterion).filter().orElseThrow(() -> new IllegalArgumentException("Filter not set"));
    @SuppressWarnings("unchecked")
    Predicate<T> predicate = (Predicate<T>) ExpressionInterpreter.of(expression).asPredicate();
    return predicate;
  }

  private InMemory() {}
}
