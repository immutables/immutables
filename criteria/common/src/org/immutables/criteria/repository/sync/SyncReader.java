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

import io.reactivex.Flowable;
import org.immutables.criteria.expression.Ordering;
import org.immutables.criteria.repository.Reader;
import org.immutables.criteria.repository.Repository;
import org.immutables.criteria.repository.reactive.ReactiveReader;

import java.util.List;
import java.util.Objects;

/**
 * Synchronous (blocking) reader operations.
 * @param <T> entity type
 */
public class SyncReader<T> implements Reader<T, SyncReader<T>> {

  private final ReactiveReader<T> reader;

  public SyncReader(ReactiveReader<T> reader) {
    this.reader = Objects.requireNonNull(reader, "reader");
  }

  public List<T> fetch() {
    return Flowable.fromPublisher(reader.fetch()).toList().blockingGet();
  }

  @Override
  public SyncReader<T>  orderBy(Ordering first, Ordering... rest) {
    return new SyncReader<>(reader.orderBy(first, rest));
  }

  @Override
  public SyncReader<T>  limit(long limit) {
    return new SyncReader<>(reader.limit(limit));
  }

  @Override
  public SyncReader<T>  offset(long offset) {
    return new SyncReader<>(reader.offset(offset));
  }
}
