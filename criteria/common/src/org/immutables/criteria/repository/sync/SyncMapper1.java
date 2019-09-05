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
import org.immutables.criteria.backend.ProjectedTuple;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.repository.MapperFunction2;
import org.immutables.criteria.repository.Mappers;
import org.immutables.criteria.repository.Publishers;
import org.immutables.criteria.repository.reactive.ReactiveFetcher;
import org.immutables.criteria.repository.reactive.ReactiveMapper1;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class SyncMapper1<T1> extends SyncFetcher<T1> {

  private final ReactiveMapper1<T1> delegate;

  SyncMapper1(Query query, Backend.Session session) {
    super(new ReactiveFetcher<>(query, session));
    this.delegate = new ReactiveMapper1<>(query, session);
  }

  public SyncFetcher<Optional<T1>> asOptional() {
    return new SyncFetcher<>(delegate.asOptional());
  }

  @Override
  public List<T1> fetch() {
    return new SyncFetcher<>(delegate).fetch();
  }

}
