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

package org.immutables.criteria.geode;

import com.google.common.base.Preconditions;
import org.apache.geode.cache.Region;
import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.Constant;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Visitors;

import java.lang.reflect.Member;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Tries to detect if current filter is based only on keys (IDs) and extracts them if possible.
 * Useful in cases when these is a more efficient {@code getByKey} / {@code deleteByKey} API provided
 * by backend.
 *
 * <p><strong>Usage example</strong> Geode (currently) doesn't support delete by query syntax ({@code DELETE ... WHERE ...}) and elements have to be
 * removed explicitly by key (using {@link Map#remove(Object)} or {@link Region#removeAll} API). With this method
 * one can extract keys from expression and use delete by key API if delete query has only IDs.
 *
 * <p>Example:
 * <pre>
 *  {@code
 *     key = 123 (valid)
 *     key in [1, 2, 3] (valid)
 *
 *     key not in [1, 2, 3] (invalid since keys are unknown)
 *     key != 1 (invalid since keys are unknown)
 *     key > 1 (invalid since keys are unknown)
 *  }
 * </pre>
 *
 */
class IdOnlyFilter {

  private final Expression filter;
  private final List<?> ids;

  IdOnlyFilter(Expression filter, Member idProperty) {
    Objects.requireNonNull(idProperty, "idProperty");
    this.filter = Objects.requireNonNull(filter, "filter");
    this.ids = maybeKeyOnlyLookup(filter, idProperty::equals);
  }

  /**
   * Can extract ids from current filter ? Only {@code = (equal) } and {@code in} operations
   * are considered and they can't be combined.
   */
  boolean hasOnlyIds() {
    return ids != null;
  }

  Optional<List<?>> toOptionalList() {
    return Optional.ofNullable(ids);
  }

  List<?> toList() {
    Preconditions.checkState(hasOnlyIds(), "Filter %s does not have only IDs. " +
            "Did you check with hasOnlyIds() ?", filter);
    return ids;
  }

  private static List<?> maybeKeyOnlyLookup(Expression filter, Predicate<Member> idPredicate) {
    if (!(filter instanceof Call)) {
      return null;
    }

    final Call predicate = (Call) filter;
    // require "equal" or "in" operators
    if (!(predicate.operator() == Operators.EQUAL || predicate.operator() == Operators.IN)) {
      return null;
    }

    final List<Expression> args = predicate.arguments();
    Preconditions.checkArgument(args.size() == 2, "Expected size 2 but got %s for %s",
            args.size(), predicate);


    if (!(args.get(0) instanceof Path && args.get(1) instanceof Constant)) {
      // second argument should be constant
      return null;
    }

    final Path path = Visitors.toPath(predicate.arguments().get(0));

    if (!(path.members().size() == 1 && idPredicate.test((Member) path.element()))) {
      return null;
    }

    return Visitors.toConstant(predicate.arguments().get(1)).values();
  }

}
