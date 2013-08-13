package org.immutables.generate.silly;

import java.util.Arrays;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class ValuesTest {

  @Test
  public void ordinalValue() {
    SillyOrdinal a = ImmutableSillyOrdinal.of("a");
    SillyOrdinal b = ImmutableSillyOrdinal.of("b");
    SillyOrdinal c = ImmutableSillyOrdinal.of("c");

    check(Arrays.asList(a.ordinal(), b.ordinal(), c.ordinal())).isOf(0, 1, 2);

    check(a.ordinalDomain().get(1)).same(b);
    check(a.ordinalDomain().get(0)).same(a);

    check(a.ordinalDomain().length()).is(3);

    check(ImmutableSillyOrdinal.of("a")).same(a);
    check(ImmutableSillyOrdinal.of("b")).same(b);
  }

  @Test
  public void internedInstanceConstruction() {
    check(ImmutableSillyInterned.of(1, 2)).is(ImmutableSillyInterned.of(1, 2));
    check(ImmutableSillyInterned.of(1, 2)).same(ImmutableSillyInterned.of(1, 2));
    check(ImmutableSillyInterned.of(1, 2)).not(ImmutableSillyInterned.of(2, 2));

    check(ImmutableSillyInterned.builder()
        .arg1(1)
        .arg2(2)
        .build())
        .same(ImmutableSillyInterned.of(1, 2));

    check(ImmutableSillyInterned.of(1, 2).hashCode()).is(ImmutableSillyInterned.of(1, 2).hashCode());
    check(ImmutableSillyInterned.of(1, 2).hashCode()).not(ImmutableSillyInterned.of(2, 2).hashCode());
  }

  @Test(expected = IllegalStateException.class)
  public void cannotBuildWrongInvariants() {
    ImmutableSillyValidatedBuiltValue.builder()
        .value(10)
        .negativeOnly(true)
        .build();
  }

  @Test
  public void canBuildCorrectInvariants() {

    ImmutableSillyValidatedBuiltValue.builder()
        .value(-10)
        .negativeOnly(true)
        .build();

    ImmutableSillyValidatedBuiltValue.builder()
        .value(10)
        .negativeOnly(false)
        .build();

    ImmutableSillyValidatedBuiltValue.builder()
        .value(-10)
        .negativeOnly(false)
        .build();
  }

  @Test(expected = IllegalStateException.class)
  public void cannotConstructWithWrongInvariants() {
    ImmutableSillyValidatedConstructedValue.of(10, true);
  }

  @Test
  public void canConstructWithCorrectInvariants() {
    ImmutableSillyValidatedConstructedValue.of(-10, true);
    ImmutableSillyValidatedConstructedValue.of(10, false);
    ImmutableSillyValidatedConstructedValue.of(-10, false);
  }
}
