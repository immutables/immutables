/*
 * Copyright 2020 Immutables Authors and Contributors
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

import com.google.common.collect.ImmutableSet;
import org.immutables.criteria.backend.StandardOperations;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

class SyncGetByKey implements Callable<Iterable<Object>> {

  private final GeodeBackend.Session session;
  private final Set<?> keys;

  SyncGetByKey(GeodeBackend.Session session, StandardOperations.GetByKey op) {
    this(session, op.keys());
  }

  SyncGetByKey(GeodeBackend.Session session, Iterable<?> keys) {
    this.session = Objects.requireNonNull(session, "session");
    this.keys = ImmutableSet.copyOf(keys);
  }

  @Override
  public Iterable<Object> call() throws Exception {
    return session.region.getAll(keys)
            .values().stream()
            .filter(Objects::nonNull) // skip missing keys (null values)
            .collect(Collectors.toList());
  }

}
