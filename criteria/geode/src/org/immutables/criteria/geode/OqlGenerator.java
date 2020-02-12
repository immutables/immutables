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
import org.immutables.criteria.expression.*;
import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates geode specific query (OQL) out of {@link Query} and some strategies like {@link PathNaming}
 */
@Value.Immutable
abstract class OqlGenerator {

  abstract PathNaming pathNaming();

  abstract String regionName();

  static OqlGenerator of(String regionName, PathNaming pathNaming) {
    return ImmutableOqlGenerator.builder().regionName(regionName).pathNaming(pathNaming).build();
  }

  @Value.Default
  boolean useBindVariables() {
    return true;
  }

  /**
   * Don't generate queries with bind variables, use literals instead (careful with OQL injection)
   */
  OqlGenerator withoutBindVariables() {
    return ImmutableOqlGenerator.copyOf(this).withUseBindVariables(false);
  }

  Oql generate(Query query) {
    if (query.count() && (query.hasAggregations() || !query.groupBy().isEmpty())) {
      throw new UnsupportedOperationException("Aggregations / Group By and count(*) are not yet supported");
    }

    // wherever to rewrite query as "select count(*) from (select distinct ...)"
    boolean addOuterCountQuery = query.count() && query.distinct() && query.hasProjections();

    final StringBuilder oql = new StringBuilder("SELECT");
    if (query.distinct()) {
      oql.append(" DISTINCT");
    }

    if (query.hasProjections()) {
      // explicitly add list of projections
      List<String> paths = query.projections().stream()
              .map(this::toProjection)
              .collect(Collectors.toList());
      String projections = query.count() && !addOuterCountQuery ? " COUNT(*) " : String.join(", ", paths);
      oql.append(" ");
      oql.append(projections);
    } else {
      // no projections
      oql.append(query.count() && !addOuterCountQuery ? " COUNT(*)" : " *");
    }

    oql.append(" FROM ").append(regionName());
    final List<Object> variables = new ArrayList<>();
    if (query.filter().isPresent()) {
      Oql withVars = Geodes.converter(useBindVariables(), pathNaming()).convert(query.filter().get());
      oql.append(" WHERE ").append(withVars.oql());
      variables.addAll(withVars.variables());
    }

    if (!query.groupBy().isEmpty()) {
      oql.append(" GROUP BY ");
      oql.append(query.groupBy().stream().map(this::toProjection).collect(Collectors.joining(", ")));
    }

    if (!query.collations().isEmpty()) {
      oql.append(" ORDER BY ");

      final String ascending = isMultiDirectionCollation(query) ? " ASC" : "";
      final String orderBy = query.collations().stream()
              .map(c -> pathNaming().name(c.path()) + (c.direction().isAscending() ? ascending : " DESC"))
              .collect(Collectors.joining(", "));

      oql.append(orderBy);
    }

    query.limit().ifPresent(limit -> oql.append(" LIMIT ").append(limit));
    query.offset().ifPresent(offset -> oql.append(" OFFSET ").append(offset));

    if (addOuterCountQuery) {
      // rewrite query as "SELECT COUNT(*) FROM ($oql)"
      // Example: select count(*) from (select distinct a, b, c from /region)
      oql.insert(0, "SELECT COUNT(*) FROM (");
      oql.append(")");
    }

    return new Oql(variables, oql.toString());
  }

  private static boolean isMultiDirectionCollation(Query query) {
    final long directionCount = query.collations().stream()
            .map(Collation::direction)
            .distinct()
            .limit(Ordering.Direction.values().length)
            .count();
    return directionCount > 1;
  }

  private String toProjection(Expression expression) {
    if (expression instanceof AggregationCall) {
      AggregationCall aggregation = (AggregationCall) expression;
      return String.format("%s(%s)", aggregation.operator().name(), toProjection(aggregation.arguments().get(0)));
    }

    return pathNaming().name((Path) expression);
  }

}
