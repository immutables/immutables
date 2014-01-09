package org.immutables.generate.silly;

import java.util.concurrent.atomic.AtomicInteger;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateLazy;

@GenerateImmutable
public abstract class SillyLazy {

  AtomicInteger counter = new AtomicInteger();

  @GenerateLazy
  public int val1() {
    return counter.incrementAndGet();
  }

  @GenerateLazy
  public int val2() {
    return counter.incrementAndGet();
  }
}
