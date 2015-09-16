package org.immutables.fixture.style;

import org.junit.Test;

import nonimmutables.SampleRuntimeException;

public class SpecifiedExceptionTest {
  @Test(expected = SampleRuntimeException.class)
  public void missingAttributesCheck() {
    ImmutableSpecifiedException.builder().build();
  }

  @Test(expected = SampleRuntimeException.class)
  public void doubleSetTest() {
    ImmutableSpecifiedException.Builder builder = ImmutableSpecifiedException.builder();
    builder.someRequiredInteger(1);
    builder.someRequiredInteger(2);
  }
}
