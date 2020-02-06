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

package org.immutables.criteria.backend;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Backend wrapper which notifies a callback before opening a session.
 * Useful in tests when one wants to auto-create collection / index / table before running tests.
 */
public class WithSessionCallback implements Backend {

  private final Backend backend;
  private final Consumer<Class<?>> creator; // callback

  private WithSessionCallback(Backend backend, Consumer<Class<?>> callback) {
    this.backend = Objects.requireNonNull(backend, "backend");
    this.creator = Objects.requireNonNull(callback, "callback");
  }

  @Override
  public String name() {
    return backend.name();
  }

  @Override
  public Session open(Class<?> entityType) {
    creator.accept(entityType);
    return backend.open(entityType);
  }

  public static Backend wrap(Backend backend, Consumer<Class<?>> callback) {
    return new WithSessionCallback(backend, callback);
  }
}
