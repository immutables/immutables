package org.immutables.generate.silly;

import java.util.concurrent.atomic.AtomicInteger;
import org.immutables.value.Value;

@Value.Immutable
public abstract class SillyLazy {

  AtomicInteger counter = new AtomicInteger();

  @Value.Lazy
  public int val1() {
    return counter.incrementAndGet();
  }

  @Value.Lazy
  public int val2() {
    return counter.incrementAndGet();
  }
}
