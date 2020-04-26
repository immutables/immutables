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

package org.immutables.criteria.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;
import org.immutables.criteria.backend.KeyExtractor;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.ExpressionConverter;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Visitors;

import java.util.Objects;
import java.util.function.Predicate;

final class Elasticsearch {

  private  Elasticsearch() {}

  static QueryBuilders.QueryBuilder toBuilder(Expression expression, PathNaming pathNaming, Predicate<Path> idPredicate) {
    return expression.accept(new ElasticsearchQueryVisitor(pathNaming, idPredicate));
  }

  /**
   * {@code query} part of the JSON
   */
  static ExpressionConverter<ObjectNode> constantScoreQuery(ObjectMapper mapper, PathNaming pathNaming, Predicate<Path> idPredicate) {
    Objects.requireNonNull(mapper, "expression");
    return expression -> {
      final QueryBuilders.QueryBuilder builder = toBuilder(expression, pathNaming, idPredicate);
      return QueryBuilders.constantScoreQuery(builder).toJson(mapper);
    };
  }

  /**
   * Predicate which checks if a path represents entity key (id). Used to generate {@code ids}
   * type of queries.
   */
  static Predicate<Path> idPredicate(KeyExtractor.KeyMetadata metadata) {
    Objects.requireNonNull(metadata, "metadata");
    Predicate<Path> alwaysFalse = p -> false;
    if (!(metadata.isKeyDefined() && metadata.isExpression())) {
      return alwaysFalse;
    }

    if (metadata.keys().size() != 1) {
      return alwaysFalse;
    }

    Expression expression = Iterables.getOnlyElement(metadata.keys());
    return Visitors.maybePath(expression)
            .map(p -> ((Predicate<Path>) p::equals))
            .orElse(alwaysFalse);
  }
}
