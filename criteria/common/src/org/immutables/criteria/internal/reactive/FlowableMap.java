/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package org.immutables.criteria.internal.reactive;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.Objects;
import java.util.function.Function;


final class FlowableMap<T, U> implements Publisher<U> {

  final Function<? super T, ? extends U> mapper;
  final Publisher<T> source;

  public FlowableMap(Publisher<T> source, Function<? super T, ? extends U> mapper) {
    this.source = source;
    this.mapper = mapper;
  }

  @Override
  public void subscribe(Subscriber<? super U> s) {
    source.subscribe(new MapSubscriber<T, U>(s, mapper));
  }

  static final class MapSubscriber<T, U> extends BasicFuseableSubscriber<T, U> {
    final Function<? super T, ? extends U> mapper;

    MapSubscriber(Subscriber<? super U> actual, Function<? super T, ? extends U> mapper) {
      super(actual);
      this.mapper = mapper;
    }

    @Override
    public void onNext(T t) {
      if (done) {
        return;
      }

      if (sourceMode != NONE) {
        downstream.onNext(null);
        return;
      }

      U v;

      try {
        v = Objects.requireNonNull(mapper.apply(t), "The mapper function returned a null value.");
      } catch (Throwable ex) {
        fail(ex);
        return;
      }
      downstream.onNext(v);
    }

    @Override
    public int requestFusion(int mode) {
      return transitiveBoundaryFusion(mode);
    }

    @Override
    public U poll() throws Exception {
      T t = qs.poll();
      return t != null ? Objects.<U>requireNonNull(mapper.apply(t), "The mapper function returned a null value.") : null;
    }
  }


}
