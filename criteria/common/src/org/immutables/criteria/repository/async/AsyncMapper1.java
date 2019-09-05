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

package org.immutables.criteria.repository.async;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.repository.reactive.ReactiveFetcher;
import org.immutables.criteria.repository.reactive.ReactiveMapper1;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class AsyncMapper1<T1> extends AsyncFetcher<T1> {

  private final ReactiveMapper1<T1> delegate;

  AsyncMapper1(Query query, Backend.Session session) {
    super(new ReactiveFetcher<>(query, session));
    this.delegate = new ReactiveMapper1<>(query, session);
  }

  public AsyncFetcher<Optional<T1>> asOptional() {
    return new AsyncFetcher<>(delegate.asOptional());
  }

  @Override
  public CompletionStage<List<T1>> fetch() {
    return new AsyncFetcher<>(delegate).fetch();
  }

}