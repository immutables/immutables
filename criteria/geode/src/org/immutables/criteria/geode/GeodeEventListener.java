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

import io.reactivex.Emitter;
import org.apache.geode.cache.query.CqEvent;
import org.apache.geode.cache.query.CqStatusListener;

import java.util.Objects;

/**
 * Passes events to existing {@link Emitter}
 * @param <T> event type
 */
class GeodeEventListener<T> implements CqStatusListener {

  private final Emitter<T> emitter;
  private final String query;

  GeodeEventListener(final String query, Emitter<T> emitter) {
    this.emitter = Objects.requireNonNull(emitter, "emitter");
    this.query = Objects.requireNonNull(query, "query");
  }

  @Override
  public void onCqDisconnected() {
    emitter.onError(new IllegalStateException(String.format("CQ disconnected [%s]", query)));
  }

  @Override
  public void onCqConnected() {
    // nop
  }

  @Override
  public void onEvent(CqEvent event) {
    emitter.onNext((T) event.getNewValue());
  }

  @Override
  public void onError(CqEvent event) {
    emitter.onError(event.getThrowable());
  }

  @Override
  public void close() {
    emitter.onComplete();
  }
}
