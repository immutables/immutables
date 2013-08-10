package org.immutables.generate.silly;

import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class ValuesTest {

  @Test
  public void internedInstanceConstruction() {
    check(ImmutableSillyInterned.of(1, 2)).is(ImmutableSillyInterned.of(1, 2));
    check(ImmutableSillyInterned.of(1, 2)).sameInstance(ImmutableSillyInterned.of(1, 2));
    check(ImmutableSillyInterned.of(1, 2)).not(ImmutableSillyInterned.of(2, 2));

    check(ImmutableSillyInterned.builder()
        .arg1(1)
        .arg2(2)
        .build())
        .sameInstance(ImmutableSillyInterned.of(1, 2));

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
