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

import com.google.common.collect.ImmutableMap;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.matcher.Matchers;
import org.immutables.criteria.matcher.Projection;
import org.immutables.criteria.repository.Updater;
import org.reactivestreams.Publisher;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ReactiveUpdater<T> implements Updater<T, Publisher<WriteResult>> {

  private final Query query;
  private final Backend.Session session;

  protected ReactiveUpdater(Query query, Backend.Session session) {
    this.query = Objects.requireNonNull(query, "query");
    this.session = Objects.requireNonNull(session, "session");
  }

  @Override
  public <V> ReactiveSetter set(Projection<V> projection, V value) {
    return new ReactiveSetter().set(projection, value);
  }

  @Override
  public Executor<Publisher<WriteResult>> replace(T newValue) {
    return new ReactiveSetter(ImmutableMap.of(Path.of(query.entityClass()), newValue));
  }

  public class ReactiveSetter implements Updater.Setter<Publisher<WriteResult>> {

    private final Map<Expression, Object> values;

    private ReactiveSetter() {
      this(ImmutableMap.of());
    }

    private ReactiveSetter(Map<Expression, Object> values) {
      this.values = ImmutableMap.copyOf(values);
    }

    @Override
    public <V> ReactiveSetter set(Projection<V> projection, V value) {
      Object nonNullValue = value;
      if (nonNullValue == null) {
        // wrap in optional for nulls. Values are unwrapped by backend implementation
        nonNullValue = Optional.empty();
      }
      Expression expression = Matchers.toExpression(projection);
      ImmutableMap<Expression, Object> newValues = ImmutableMap.<Expression, Object>builder().putAll(values).put(expression, nonNullValue).build();
      return new ReactiveSetter(newValues);
    }

    @Override
    public Publisher<WriteResult> execute() {
      return session.execute(StandardOperations.Update.of(query, values)).publisher();
    }
  }

}
