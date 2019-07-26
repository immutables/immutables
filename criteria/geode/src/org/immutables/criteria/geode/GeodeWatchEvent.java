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

package org.immutables.criteria.geode;

import org.apache.geode.cache.query.CqEvent;
import org.immutables.criteria.repository.WatchEvent;

import java.util.Objects;
import java.util.Optional;

/**
 * Geode change event
 * @param <T> event type
 */
class GeodeWatchEvent<T> implements WatchEvent<T> {

  private final Object key;
  private final T newValue;
  private final Operation operation;

  GeodeWatchEvent(CqEvent cqEvent) {
    this.key = Objects.requireNonNull(cqEvent.getKey(), "event.key");
    this.newValue = (T) cqEvent.getNewValue();
    this.operation = toOperation(cqEvent);
  }

  @Override
  public Object key() {
    return key;
  }

  @Override
  public Optional<T> newValue() {
    return Optional.ofNullable(newValue);
  }

  @Override
  public Operation operation() {
    return this.operation;
  }

  private static WatchEvent.Operation toOperation(CqEvent event) {
    // TODO add proper mapping between Geode and Criteria operations
    return Operation.UPDATE;
  }
}
