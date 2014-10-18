package org.immutables.value.processor.meta;

import com.google.common.collect.ImmutableList;
import org.immutables.value.processor.meta.LongBits.LongPositions;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class LongBitsTest {
  private static final String A = "a", B = "b", C = "c";
  private final LongBits longBits = new LongBits();

  @Test
  public void singleLong() {
    LongPositions positions = longBits.forIterable(ImmutableList.of(A, B), 2);
    check(positions.longsIndeces()).isOf(0);
    check(positions.apply(A).index).is(0);
    check(positions.apply(A).bit).is(0);
    check(positions.apply(A).mask).is(0b1L);

    check(positions.apply(B).index).is(0);
    check(positions.apply(B).bit).is(1);
    check(positions.apply(B).mask).is(0b10L);
  }

  @Test
  public void doubleLong() {
    LongPositions positions = longBits.forIterable(ImmutableList.of(A, B, C), 2);
    check(positions.longsIndeces()).isOf(0, 1);
    check(positions.apply(A).index).is(0);
    check(positions.apply(A).bit).is(0);
    check(positions.apply(A).mask).is(0b1L);

    check(positions.apply(B).index).is(0);
    check(positions.apply(B).bit).is(1);
    check(positions.apply(B).mask).is(0b10L);

    check(positions.apply(C).index).is(1);
    check(positions.apply(C).bit).is(0);
    check(positions.apply(C).mask).is(0b1L);
  }
}
