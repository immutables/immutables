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

package org.immutables.criteria.repository.reactive;

import org.immutables.criteria.Criterias;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.adapter.Backend;
import org.immutables.criteria.adapter.Operations;
import org.immutables.criteria.expression.Collation;
import org.immutables.criteria.expression.Ordering;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.expression.Queryable;
import org.immutables.criteria.repository.Reader;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Reactive implementation of the reader.
 */
public final class ReactiveReader<T> implements Reader<T, ReactiveReader<T>>, Queryable {

  private final Query query;
  private final Backend backend;

  public ReactiveReader(Criterion<T> criteria, Backend backend) {
    this(Criterias.toQuery(criteria), backend);
  }

  private ReactiveReader(Query query, Backend backend) {
    this.query = Objects.requireNonNull(query, "query");
    this.backend = Objects.requireNonNull(backend, "backend");
  }

  private ReactiveReader<T> newReader(Query query) {
    return new ReactiveReader<>(query, backend);
  }

  @Override
  public ReactiveReader<T> orderBy(Ordering first, Ordering ... rest) {
    final List<Ordering> orderings = new ArrayList<>();
    orderings.add(first);
    orderings.addAll(Arrays.asList(rest));

    final List<Collation> collect = orderings.stream().map(x -> (Collation) x).collect(Collectors.toList());

    return newReader(query.addCollations(collect));
  }

  @Override
  public ReactiveReader<T> limit(long limit) {
    return newReader(query.withLimit(limit));
  }

  @Override
  public ReactiveReader<T> offset(long offset) {
    return newReader(query.withOffset(offset));
  }

  public Publisher<T> fetch() {
    return backend.execute(Operations.Select.of(query));
  }

  @Override
  public Query query() {
    return query;
  }
}
