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

package org.immutables.criteria.adapter;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utils for <a href="https://www.reactive-streams.org/">reactive streams</a> with typical operators
 * like map / error / flatMap / flatMapIterable which are applied on a {@link Publisher}.
 *
 * <p>This is internal class and should not be used outside criteria and adapters.
 *
 * <p>It is currently a naive implementation which didn't pass the
 * <a href="https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck">TCK</a>
 *
 * <p>Wherever Criteria should depend directly on existing reactive libraries
 * like <a href="https://github.com/ReactiveX/RxJava">rxjava</a> or <a href="https://projectreactor.io/">reactor</a>
 * instead of home-made implementation is a valid question and will be decided later.
 */
public final class Reactive {

  private Reactive() {}

  /**
   *  Converts streaming elements into a different type using provided {@code mapper}
   */
  public static <T, U> Publisher<U> map(Publisher<T> source, Function<? super T, ? extends U> mapper) {
    return new MapPublisher<>(mapper, source);
  }

  public static <T> Publisher<T> error(Supplier<? extends Throwable> errorSupplier) {
    return new ErrorPublisher<>(errorSupplier);
  }

  /**
   * Used to convert {@code Publisher<Iterable<U>>} into {@code Publisher<U>}
   */
  public static  <T, U> Publisher<U> flatMapIterable(Publisher<T> publisher, final Function<? super T, ? extends Iterable<? extends U>> mapper) {
      return new FlattenIterablePublisher<>(publisher, mapper);
  }

  public static <T> Publisher<T> error(Throwable e) {
    return error(() -> e);
  }

  private static class MapPublisher<T, U> implements Publisher<U> {

    private final Function<? super T, ? extends U> mapper;
    private final Publisher<T> source;

    private MapPublisher(Function<? super T, ? extends U> mapper, Publisher<T> source) {
      this.mapper = Objects.requireNonNull(mapper, "mapper");
      this.source = Objects.requireNonNull(source, "source");
    }

    @Override
    public void subscribe(Subscriber<? super U> s) {
      source.subscribe(new MapSubscriber<>(mapper, s));
    }

    private static class MapSubscriber<T, U> implements Subscriber<T> {
      private final Function<? super T, ? extends U> mapper;
      private final Subscriber<? super U> actual;
      private boolean done = false;

      private MapSubscriber(Function<? super T, ? extends U> mapper, Subscriber<? super U> actual) {
        this.mapper = mapper;
        this.actual = actual;
      }

      @Override
      public void onSubscribe(Subscription s) {
        actual.onSubscribe(s);
      }

      @Override
      public void onNext(T t) {
        final U apply;
        try {
          apply = mapper.apply(t);
        } catch (Throwable throwable) {
          onError(throwable);
          return;
        }

        actual.onNext(apply);
      }

      @Override
      public void onError(Throwable t) {
        if (done) {
          return;
        }
        done = true;
        actual.onError(t);
      }

      @Override
      public void onComplete() {
        if (done) {
          return;
        }
        done = true;
        actual.onComplete();
      }
    }
  }


  private static class ErrorPublisher<T> implements Publisher<T> {

    private final Supplier<? extends Throwable> errorSupplier;

    private ErrorPublisher(Supplier<? extends Throwable> errorSupplier) {
      this.errorSupplier = Objects.requireNonNull(errorSupplier, "errorSupplier");
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
      s.onSubscribe(EmptySubscription.INSTANCE);
      s.onError(errorSupplier.get());
    }
  }

  private static class EmptySubscription implements Subscription {

    private static final EmptySubscription INSTANCE = new EmptySubscription();

    @Override
    public void request(long n) {
      // nop
    }

    @Override
    public void cancel() {
      // nop
    }
  }

  private static class FlattenIterablePublisher<T, U> implements Publisher<U> {
    private final Publisher<T> source;
    private final Function<? super T, ? extends Iterable<? extends U>> mapper;

    private FlattenIterablePublisher(Publisher<T> source, Function<? super T, ? extends Iterable<? extends U>> mapper) {
      this.source = Objects.requireNonNull(source, "source");
      this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public void subscribe(Subscriber<? super U> downstream) {
      source.subscribe(new FlattenSubscriber<>(mapper, downstream));
    }

    private final static class FlattenSubscriber<T, U> implements Subscriber<T> {

      private final Function<? super T, ? extends Iterable<? extends U>> mapper;
      private final Subscriber<? super U> downstream;
      private boolean done;

      private FlattenSubscriber(Function<? super T, ? extends Iterable<? extends U>> mapper, Subscriber<? super U> downstream) {
        this.mapper = mapper;
        this.downstream = downstream;
      }

      @Override
      public void onSubscribe(Subscription s) {
        downstream.onSubscribe(s);
      }

      @Override
      public void onNext(T value) {
        if (done) {
          return;
        }

        Iterator<? extends U> it;

        try {
          it = mapper.apply(value).iterator();
        } catch (Throwable ex) {
          onError(ex);
          return;
        }

        Subscriber<? super U> d = downstream;

        for (;;) {
          boolean hasNext;

          try {
            hasNext = it.hasNext();
          } catch (Throwable ex) {
            onError(ex);
            return;
          }

          if (hasNext) {
            U v;

            try {
              v = Objects.requireNonNull(it.next(), "The iterator returned a null value");
            } catch (Throwable ex) {
              onError(ex);
              return;
            }

            d.onNext(v);
          } else {
            break;
          }
        }

      }

      @Override
      public void onError(Throwable t) {
        if (done) {
          return;
        }
        done = true;
        downstream.onError(t);
      }

      @Override
      public void onComplete() {
        if (done) {
          return;
        }
        done = true;
        downstream.onComplete();
      }
    }

  }

}
