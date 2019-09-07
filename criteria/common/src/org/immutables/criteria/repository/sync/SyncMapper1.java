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

package org.immutables.criteria.repository.sync;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.repository.reactive.ReactiveMapper1;

import java.util.List;
import java.util.Optional;

public class SyncMapper1<T1> implements SyncFetcher<T1> {

  private final ReactiveMapper1<T1> mapper;
  private final SyncFetcher<T1> fetcher;

  SyncMapper1(Query query, Backend.Session session) {
    this.mapper = new ReactiveMapper1<>(query, session);
    this.fetcher = SyncFetcherDelegate.fromReactive(mapper);
  }

  public SyncFetcher<Optional<T1>> asOptional() {
    return SyncFetcherDelegate.fromReactive(mapper.asOptional());
  }

  @Override
  public List<T1> fetch() {
    return fetcher.fetch();
  }

  @Override
  public T1 one() {
    return fetcher.one();
  }

  @Override
  public Optional<T1> oneOrNone() {
    return fetcher.oneOrNone();
  }

  @Override
  public boolean exists() {
    return fetcher.exists();
  }

}
