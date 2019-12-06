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

import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.backend.ProjectedTuple;
import org.immutables.criteria.expression.ExpressionConverter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Util functions for Geode backend
 */
final class Geodes {

  /**
   * Returns only predicate part to be appended to {@code WHERE} clause.
   *
   * @return predicate, empty string if no predicate
   */
  static ExpressionConverter<Oql> converter(boolean useBindVariables, PathNaming pathNaming) {
    return expression -> expression.accept(new GeodeQueryVisitor(useBindVariables, pathNaming));
  }

  /**
   * Replace single quote {@code '} with two quotes {@code ''}
   *
   * <p>From <a href="https://www.postgresql.org/docs/9.1/sql-syntax-lexical.html">SQL syntax in PostgreSQL</a>:
   *  <pre>
   *    To include the escape character in the identifier literally, write it twice.
   *  </pre>
   * </p>
   * @param oql string to escape
   * @return escaped string
   * @see
   */
  static String escapeOql(CharSequence oql) {
    return oql.toString().replace("'", "''");
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

  static Object convert(Object value, Type destinationType) {
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

  private Geodes() {}
}
