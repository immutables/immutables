package org.immutables.generate.silly;

import org.junit.Test;

public class ValuesTest {
  @Test(expected = IllegalStateException.class)
  public void cantBuildWrongInvariants() {
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
  public void cantConstructWithWrongInvariants() {
    ImmutableSillyValidatedConstructedValue.of(10, true);
  }

  @Test
  public void canConstructWithCorrectInvariants() {
    ImmutableSillyValidatedConstructedValue.of(-10, true);
    ImmutableSillyValidatedConstructedValue.of(10, false);
    ImmutableSillyValidatedConstructedValue.of(-10, false);
  }
}
