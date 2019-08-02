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

package org.immutables.criteria.personmodel;

import com.google.common.base.Preconditions;
import io.reactivex.Flowable;
import org.immutables.check.IterableChecker;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.expression.Queryable;
import org.immutables.criteria.repository.Repositories;
import org.immutables.criteria.repository.Repository;
import org.immutables.criteria.repository.reactive.ReactiveReader;
import org.immutables.criteria.repository.Reader;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.immutables.check.Checkers.check;

/**
 * Generic checker for a {@link org.immutables.criteria.Criterion}
 * @param <T>
 */
public class CriteriaChecker<T> {

  private final ReactiveReader<T> reader;
  private final Query query;
  private final List<T> result;

  private CriteriaChecker(ReactiveReader<T> reader) {
    this.reader = Objects.requireNonNull(reader, "reader");
    this.query = Repositories.toQuery(reader);
    this.result = fetch(reader);
  }

  public void empty() {
    if (!result.isEmpty()) {
      throw new AssertionError(
              String.format("Expected query [%s] to return empty result but was of size %d: %s",
                      toDebugString(query),
                      result.size(),
                      result));
    }
  }

  public void hasSize(int size) {
    if (result.size() != size) {
      throw new AssertionError(
              String.format("Expected query [%s] to return %d entries but was of size %d: %s",
                      toDebugString(query),
                      size,
                      result.size(),
                      result));
    }
  }

  public void notEmpty() {
    if (result.isEmpty()) {
      throw new AssertionError(
              String.format("Expected query [%s] to return non empty result but was empty",
                      toDebugString(query)));
    }
  }

  public IterableChecker<List<T>, T> toList() {
    return toList(x -> x);
  }

  public <Z> IterableChecker<List<Z>, Z> toList(Function<? super T, ? extends Z> fn) {
    return check(result.stream().map(fn).collect(Collectors.toList()));
  }

  private static <T> List<T> fetch(ReactiveReader<T> reader) {
    return Flowable.fromPublisher(reader.fetch()).toList().blockingGet();
  }

  /**
   * Converts criteria to a string for debugging purposes
   */
  private static String toDebugString(Query query) {
    return query.toString();
  }

  public static <T> CriteriaChecker<T> of(Reader<T, ?> reader) {
    Preconditions.checkArgument(reader instanceof ReactiveReader,
            "%s should implement %s", reader.getClass(), ReactiveReader.class.getName());

    return new CriteriaChecker<>((ReactiveReader<T>) reader);
  }

}
