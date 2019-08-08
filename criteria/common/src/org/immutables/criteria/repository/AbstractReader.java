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

package org.immutables.criteria.repository;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.expression.Collation;
import org.immutables.criteria.expression.Ordering;
import org.immutables.criteria.expression.Query;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Template class for Readers. For future API compatibility prefer extend it rather than
 * implement {@link Reader} directly.s
 */
public abstract class AbstractReader<T, R extends Reader<T, R>> implements Reader<T, R> {

  private final Query query;
  private final Backend.Session session;

  protected AbstractReader(Query query, Backend.Session session) {
    this.query = Objects.requireNonNull(query, "query");
    this.session = Objects.requireNonNull(session, "backend");
  }

  protected abstract R newReader(Query query);

  /**
   * Perform read operation returning generic {@link Publisher}
   */
  protected Publisher<T> fetchInternal() {
    return session.execute(StandardOperations.Select.of(query));
  }

  /**
   * Expose current query (used mostly for SPIs)
   */
  Query query() {
    return query;
  }

  @Override
  public R orderBy(Ordering first, Ordering... rest) {
    final List<Ordering> orderings = new ArrayList<>();
    orderings.add(first);
    orderings.addAll(Arrays.asList(rest));

    final List<Collation> collations = orderings.stream().map(x -> (Collation) x).collect(Collectors.toList());
    return newReader(query.addCollations(collations));
  }

  @Override
  public R limit(long limit) {
    return newReader(query.withLimit(limit));
  }

  @Override
  public R offset(long offset) {
    return newReader(query.withOffset(offset));
  }
}
