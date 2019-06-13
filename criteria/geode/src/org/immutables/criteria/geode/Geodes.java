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

import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressions;

/**
 * Util class for Geode
 */
class Geodes {

  static String toGeodeQuery(String regionName, Expression expression) {

    final String emptyQuery = String.format("select * from /%s", regionName);

    if (Expressions.isNil(expression)) {
      return emptyQuery;
    }

    final String predicate = expression.accept(new GeodeQueryVisitor());

    if (predicate.isEmpty()) {
      return emptyQuery;
    }

    return String.format("select * from /%s where %s", regionName, predicate);
  }

}
