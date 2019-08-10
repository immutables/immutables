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

package org.immutables.criteria.repository;

import io.reactivex.Flowable;
import org.immutables.criteria.backend.Backend;
import org.reactivestreams.Publisher;

import java.util.Objects;

/**
 * Backend which returns same result for any operation. Intended for tests
 */
public class FakeBackend implements Backend {

  private final Publisher<?> existing;

  public FakeBackend() {
    this(Flowable.empty());
  }

  public FakeBackend(Publisher<?> existing) {
    this.existing = Objects.requireNonNull(existing, "result");
  }

  @Override
  public Session open(Class<?> context) {
    return new Session();
  }

  private class Session implements Backend.Session {

    @SuppressWarnings("unchecked")
    @Override
    public <T> Publisher<T> execute(Operation operation) {
      return (Publisher<T>) existing;
    }
  }
}
