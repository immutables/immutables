package org.immutables.fixture.style;

import org.junit.Test;

import nonimmutables.SampleRuntimeException;

public class SpecifiedExceptionTest {
  @Test(expected = SampleRuntimeException.class)
  public void missingAttributesCheck() {
    ImmutableSpecifiedException.builder().build();
  }
}
