package org.immutables.common.collect;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class OrdinalSetTest {

  Domain d1 = new Domain();
  Domain d2 = new Domain();

  Ord a0 = d1.get(0);
  Ord a1 = d1.get(1);

  Ord b0 = d2.get(0);
  Ord b1 = d2.get(1);

  @Test
  public void emptySet() {
    // not using IterableChecker to cover correct code paths
    check(ImmutableOrdinalSet.of().isEmpty());
    check(ImmutableOrdinalSet.of().size()).is(0);

    check(ImmutableOrdinalSet.of()).same(ImmutableOrdinalSet.of());
    check(ImmutableOrdinalSet.of()).asString().notEmpty();
  }

  @Test
  public void singletonSet() {
    // not using IterableChecker to cover correct code paths
    check(!ImmutableOrdinalSet.of(a0).isEmpty());
    check(ImmutableOrdinalSet.of(a0).size()).is(1);

    check(ImmutableOrdinalSet.of(a0)).isOf(a0);
    check(ImmutableOrdinalSet.of(a0)).has(a0);
    check(ImmutableOrdinalSet.of(a0)).not(ImmutableOrdinalSet.of(d1.get(4)));
    check(ImmutableOrdinalSet.of(a0).containsAll(ImmutableOrdinalSet.of(a0)));
    check(ImmutableOrdinalSet.of(a0).containsAll(ImmutableSet.of(a0)));
    check(ImmutableOrdinalSet.of(a0)).not().hasAll(ImmutableSet.of(d1.get(5)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void wrongRegularSet() {
    ImmutableOrdinalSet.of(a0, b0);
  }

  @Test
  public void regularSet() {
    // not using IterableChecker to cover correct code paths
    check(!ImmutableOrdinalSet.of(a0, a1).isEmpty());
    check(ImmutableOrdinalSet.of(b0, b1).size()).is(2);

    check(ImmutableOrdinalSet.of(b0, b1).containsAll(ImmutableOrdinalSet.of(b0)));
    check(ImmutableOrdinalSet.of(b0, b1).containsAll(ImmutableOrdinalSet.of(b0, b1)));

    check(!ImmutableOrdinalSet.of(b0, b1)
        .containsAll(ImmutableOrdinalSet.of(a0, a1)));

    check(!ImmutableOrdinalSet.of(b0, b1)
        .containsAll(ImmutableOrdinalSet.of(d2.get(120), d2.get(130))));

    check(!ImmutableOrdinalSet.of(d2.get(30), d2.get(70))
        .containsAll(ImmutableOrdinalSet.of(d2.get(30), d2.get(60))));

    check(ImmutableOrdinalSet.of(b0, b1)).hasAll(b0);
    check(ImmutableOrdinalSet.of(b0, b1)).not().hasAll(a0, a1);
    check(ImmutableOrdinalSet.of(b0, b1)).not().hasAll(b0, a1);
    check(ImmutableOrdinalSet.of(b0, b1)).not().has(a1);

    check(ImmutableOrdinalSet.of(b0, b1).containsAll(ImmutableSet.of()));
    check(ImmutableOrdinalSet.of(b0, b1).containsAll(ImmutableSet.of(b0, b1)));
    check(!ImmutableOrdinalSet.of(b0, b1).contains(1));
    check(!ImmutableOrdinalSet.of(b0, b1).containsAll(ImmutableSet.of(0, 1)));
  }
}
