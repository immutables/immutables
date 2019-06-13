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
import org.immutables.criteria.Criterias;
import org.immutables.criteria.DocumentCriteria;
import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.Constant;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.ExpressionConverter;
import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.Root;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Util class for Geode
 */
class Geodes {


  /**
   * Returns only predicate part to be appended to {@code WHERE} clause.
   *
   * @return predicate, empty string if no predicate
   */
  static ExpressionConverter<String> converter() {
    return expression -> Expressions.extractPredicate(expression)
            .map(e -> e.accept(new GeodeQueryVisitor()))
            .orElse("");
  }

  /**
   * Checks if current criteria has any predicate expressions defined (like {@code foo = 123}).
   */
  static boolean hasPredicate(DocumentCriteria<?> criteria) {
    return Expressions.extractPredicate(Criterias.toExpression(criteria)).isPresent();
  }

  /**
   * Geode (currently) doesn't support delete by query syntax ({@code DELETE ... WHERE ...}) and elements have to be
   * removed explicitly by key ({@link Map#remove(Object)} API)
   *
   * <p>Tries to detect if current criteria is based only on keys and extract them from expression (if it is only
   * expression based on keys).
   *
   * <p>Example:
   * <pre>
   *  {@code
   *     key = 123
   *     key in [1, 2, 3]
   *     key not in [1, 2, 3] (invalid since keys are unknown)
   *     key != 1 (invalid since keys are unknown)
   *  }
   * </pre>
   *
   *
   */
  static Optional<List<?>> canDeleteByKey(DocumentCriteria<?> criteria) {
    if (!hasPredicate(criteria)) {
      return Optional.of(Collections.emptyList());
    }

    final Expression expr = Expressions.extractPredicate(Criterias.toExpression(criteria))
            .orElseThrow(() -> new IllegalStateException("Shouldn't get here"));

    if (!(expr instanceof Call)) {
      return Optional.empty();
    }

    final Call predicate = (Call) expr;
    if (!(predicate.operator() == Operators.EQUAL || predicate.operator() == Operators.IN)) {
      return Optional.empty();
    }

    final List<Expression> args = predicate.arguments();
    Preconditions.checkArgument(args.size() == 2, "Expected size 2 but got %s for %s",
            args.size(), predicate);


    if (!(args.get(0) instanceof Path && args.get(1) instanceof Constant)) {
      // second argument should be constant
      return Optional.empty();
    }

    // field should be id
    final Class<?> entityClass = ((Root) Criterias.toExpression(criteria)).entityClass();

    return Optional.empty();
  }
}
