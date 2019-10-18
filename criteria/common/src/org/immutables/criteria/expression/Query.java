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

package org.immutables.criteria.expression;

import org.immutables.value.Value;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Query which is composed of predicates, projections, limit, offset, group by and order by expressions.
 */
@Value.Style(visibility = Value.Style.ImplementationVisibility.PACKAGE)
@Value.Immutable
public abstract class Query {

  @Value.Parameter
  public abstract Class<?> entityClass();

  public abstract Optional<Expression> filter();

  public abstract OptionalLong limit();

  public abstract OptionalLong offset();

  public abstract List<Expression> projections();

  public abstract List<Collation> collations();

  public abstract List<Expression> groupBy();

  /**
   * Similar to {@code COUNT(*)} in SQL. Wherever to compute just number of to be returned records.
   */
  @Value.Default
  public boolean count() {
    return false;
  }

  /**
   * Check if current query has any projections
   */
  public boolean hasProjections() {
    return !projections().isEmpty();
  }

  /**
   * Check if current query has any aggregation calls
   */
  public boolean hasAggregations() {
    return !groupBy().isEmpty() || projections().stream().anyMatch(p -> p instanceof AggregationCall);
  }

  public static Query of(Class<?> entityClass) {
    return ImmutableQuery.of(entityClass);
  }

  public Query withFilter(Expression filter) {
    return ImmutableQuery.copyOf(this).withFilter(filter);
  }

  public Query addCollations(Iterable<Collation> collations) {
    return ImmutableQuery.builder().from(this).addAllCollations(collations).build();
  }

  public Query addProjections(Expression ... projections) {
    return ImmutableQuery.builder().from(this).addProjections(projections).build();
  }

  public Query addProjections(Iterable<Expression> projections) {
    return ImmutableQuery.builder().from(this).addAllProjections(projections).build();
  }

  public Query addGroupBy(Iterable<Expression> groupBy) {
    return ImmutableQuery.builder().from(this).addAllGroupBy(groupBy).build();
  }

  public Query addGroupBy(Expression ... groupBy) {
    return ImmutableQuery.builder().from(this).addGroupBy(groupBy).build();
  }

  public Query withCount(boolean count) {
    return ImmutableQuery.builder().from(this).count(count).build();
  }

  public Query withOffset(long offset) {
    return ImmutableQuery.copyOf(this).withOffset(offset);
  }

  public Query withLimit(long limit) {
    return ImmutableQuery.copyOf(this).withLimit(limit);
  }

  @Override
  public String toString() {
    final StringWriter string = new StringWriter();
    final PrintWriter writer = new PrintWriter(string);

    final DebugExpressionVisitor<PrintWriter> visitor = new DebugExpressionVisitor<>(writer);
    writer.append("entity: ").append(entityClass().getName()).println();

    if (!projections().isEmpty()) {
      projections().forEach(p -> p.accept(visitor));
      writer.println();
    }

    filter().ifPresent(f -> {
      writer.append("filter: ");

      f.accept(visitor);
      writer.println();
    });

    if (!groupBy().isEmpty()) {
      writer.append("groupBy: ");
      groupBy().forEach(g -> g.accept(visitor));
      writer.println();
    }

    limit().ifPresent(limit -> writer.append(" limit:").append(String.valueOf(limit)).println());
    offset().ifPresent(offset -> writer.append(" offset:").append(String.valueOf(offset)).println());

    return string.toString();
  }
}
