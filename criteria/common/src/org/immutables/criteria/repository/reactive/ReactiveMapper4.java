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

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.repository.MapperFunction4;
import org.immutables.criteria.repository.Mappers;
import org.immutables.criteria.repository.Tuple;

import java.util.function.Function;

public class ReactiveMapper4<T1, T2, T3, T4> {

  private final ReactiveFetcher<Tuple> fetcher;

  public ReactiveMapper4(Query query, Backend.Session session) {
    this.fetcher = ReactiveFetcher.of(query, session);
  }

  public <R> ReactiveFetcher<R> map(MapperFunction4<T1, T2, T3, T4, R> mapFn) {
    return map(Mappers.fromTuple(mapFn));
  }

  public <X> ReactiveFetcher<X> map(Function<? super Tuple, ? extends X> mapFn) {
    return fetcher.map(mapFn);
  }


}
