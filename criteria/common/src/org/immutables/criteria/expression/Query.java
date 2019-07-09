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

import com.google.common.collect.ImmutableList;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Query which is composed of predicates, projections, limit, offset, group by and order by expressions.
 */
public final class Query  {

  private final EntityPath entityPath;
  private final Expression filter;
  private final List<Collation> collations;
  private final Long limit;
  private final Long offset;

  private Query(EntityPath entityPath, Expression filter, List<Collation> collations, Long limit, Long offset) {
    this.entityPath = Objects.requireNonNull(entityPath, "entityPath");
    this.collations = ImmutableList.copyOf(collations);
    this.filter = filter;
    this.limit = limit;
    this.offset = offset;
  }

  public EntityPath entityPath() {
    return this.entityPath;
  }

  public Optional<Expression> filter() {
    return Optional.ofNullable(filter);
  }

  public OptionalLong limit() {
    return limit == null ? OptionalLong.empty() : OptionalLong.of(limit);
  }

  public Query withLimit(long limit) {
    return new Query(entityPath, filter, collations, limit, offset);
  }

  public OptionalLong offset() {
    return offset == null ? OptionalLong.empty() : OptionalLong.of(offset);
  }

  public Query withOffset(long offset) {
    return new Query(entityPath, filter, collations, limit, offset);
  }

  public static Query of(Class<?> entityClass) {
    return new Query(EntityPath.of(entityClass), null, ImmutableList.of(), null, null);
  }

  public Query withFilter(Expression filter) {
    Objects.requireNonNull(filter, "filter");
    return new Query(entityPath, filter, collations, limit, offset);
  }

  public Query addCollations(Iterable<Collation> collations) {
    Objects.requireNonNull(collations, "collations");
    List<Collation> newCollations = ImmutableList.<Collation>builder().addAll(this.collations).addAll(collations).build();
    return new Query(entityPath, filter, newCollations, limit, offset);
  }

  public List<Collation> collations() {
    return collations;
  }

  @Override
  public String toString() {
    final StringWriter string = new StringWriter();
    final PrintWriter writer = new PrintWriter(string);

    writer.append("entity: ").append(entityPath().annotatedElement().getName()).println();

    if (filter != null) {
      writer.append("filter: ");
      filter.accept(new DebugExpressionVisitor<>(writer));
      writer.println();
    }

    if (limit != null) {
      writer.append(" limit:").append(String.valueOf(limit)).println();
    }

    if (offset != null) {
      writer.append(" offset:").append(String.valueOf(offset)).println();
    }

    return string.toString();
  }
}
