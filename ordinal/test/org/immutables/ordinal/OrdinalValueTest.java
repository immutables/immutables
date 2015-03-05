package org.immutables.ordinal;

import org.junit.Test;

import static org.immutables.check.Checkers.check;
import static org.immutables.check.Checkers.checkAll;

public class OrdinalValueTest {
  @Test
  public void ordinalValue() {
    ImmutableSillyOrdinal a = ImmutableSillyOrdinal.of("a");
    ImmutableSillyOrdinal b = ImmutableSillyOrdinal.of("b");
    ImmutableSillyOrdinal c = ImmutableSillyOrdinal.of("c");

    checkAll(a.ordinal(), b.ordinal(), c.ordinal()).isOf(0, 1, 2);
    check(ImmutableSillyOrdinal.of("a")).same(a);
    check(ImmutableSillyOrdinal.of("b")).same(b);

    check(a.domain().get(1)).same(b);
    check(a.domain().get(0)).same(a);
    check(a.domain().length()).is(3);
    check(a.domain()).isOf(a, b, c);
  }

  @Test
  public void ordinalDomain() {
    ImmutableSillyOrdinal.Domain domain = new ImmutableSillyOrdinal.Domain();

    ImmutableSillyOrdinal a = ImmutableSillyOrdinal.of("a");

    ImmutableSillyOrdinal a1 = ImmutableSillyOrdinal.builder()
        .domain(domain)
        .name("a")
        .build();

    ImmutableSillyOrdinal a2 = ImmutableSillyOrdinal.builder()
        .domain(domain)
        .name("a")
        .build();

    check(a.domain()).not(domain);
    check(a.domain()).same(ImmutableSillyOrdinal.Domain.get());
    check(a1.domain()).same(domain);

    check(a).not(a1);
    check(a1).same(a2);
    check(domain.length()).is(1);
  }

  @Test
  public void ordinalValueSet() {
    check(ImmutableSillyOrdinalHolder.builder()
        .addSet(ImmutableSillyOrdinal.of("a"))
        .build()
        .set())
        .isA(ImmutableOrdinalSet.class);
  }
}
