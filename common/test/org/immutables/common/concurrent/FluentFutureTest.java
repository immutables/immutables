package org.immutables.common.concurrent;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class FluentFutureTest implements Function<Integer, String> {
  AtomicInteger calledTimes = new AtomicInteger();

  @Override
  public String apply(Integer input) {
    calledTimes.incrementAndGet();
    return Integer.toString(input);
  }

  @Test
  public void transform() {
    FluentFuture<String> future =
        FluentFutures.from(Futures.immediateFuture(1))
            .transform(this);

    check(calledTimes.get()).is(1);
    check(future.getUnchecked()).is("1");
  }

  @Test
  public void lazyTransform() {
    FluentFuture<String> future =
        FluentFutures.from(Futures.immediateFuture(1))
            .lazyTransform(this);

    check(calledTimes.get()).is(0);
    check(future.getUnchecked()).is("1");
    check(calledTimes.get()).is(1);
    check(future.getUnchecked()).is("1");
    check(calledTimes.get()).is(2);
  }
}
