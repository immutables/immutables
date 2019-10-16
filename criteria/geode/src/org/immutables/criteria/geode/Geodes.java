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
import org.immutables.criteria.backend.IdResolver;
import org.immutables.criteria.backend.ProjectedTuple;
import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.Constant;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.ExpressionConverter;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Visitors;

import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Util functions for Geode backend
 */
final class Geodes {

  /**
   * Returns only predicate part to be appended to {@code WHERE} clause.
   *
   * @return predicate, empty string if no predicate
   */
  static ExpressionConverter<OqlWithVariables> converter(boolean useBindVariables) {
    return expression -> expression.accept(new GeodeQueryVisitor(useBindVariables));
  }

  /**
   * Used to convert between types. Sometimes geode backend returns different types for aggregate functions like AVG / MIN / MAX.
   * Eg. Long vs Integer
   */
  static ProjectedTuple castNumbers(ProjectedTuple tuple) {
    List<Object> newVaues = new ArrayList<>();
    for (int i = 0; i < tuple.values().size(); i++) {
      newVaues.add(convert(tuple.values().get(i), tuple.paths().get(i).returnType()));
    }

    return ProjectedTuple.of(tuple.paths(), newVaues);
  }

  private static Object convert(Object value, Type destinationType) {
    if (value == null) {
      return null;
    }

    if (value.getClass() == destinationType) {
      // no need to cast
      return value;
    }

    // try to convert between numbers
    if (value instanceof Number) {
      Primitive primitive = Primitive.ofAny(destinationType);
      if (primitive != null && !primitive.boxClass.isInstance(value)) {
        // cast
        return primitive.cast((Number) value);
      }
    }

    // don't know what to do with this value
    // return AS IS
    return value;
  }


  /**
   * Geode (currently) doesn't support delete by query syntax ({@code DELETE ... WHERE ...}) and elements have to be
   * removed explicitly by key (using {@link Map#remove(Object)} or {@link Region#removeAll} API)
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
   * @param expr filter applied on entries for deletion
   */
  static Optional<List<?>> canDeleteByKey(Expression expr, IdResolver idResolver) {
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

    final Path path = Visitors.toPath(predicate.arguments().get(0));

    if (!(path.paths().size() == 1 && idResolver.asPredicate().test((Member) path.annotatedElement()))) {
      return Optional.empty();
    }

    final List<Object> values = Visitors.toConstant(predicate.arguments().get(1)).values();
    return Optional.of(values);
  }

  private Geodes() {}
}
