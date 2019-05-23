package org.immutables.criteria.elasticsearch;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utils for reactive streams
 */
final class Publishers {

  private Publishers() {}

  /**
   *  Converts streaming elements into a different type using provided {@code mapper}
   */
  static <T, U> Publisher<U> map(Publisher<T> source, Function<? super T, ? extends U> mapper) {
    return new MapPublisher<>(mapper, source);
  }

  static <T> Publisher<T> error(Supplier<? extends Throwable> errorSupplier) {
    return new ErrorPublisher<>(errorSupplier);
  }

  static <T> Publisher<T> error(Throwable e) {
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

}
