package org.immutables.fixture.style;

import org.junit.Test;

import nonimmutables.SampleRuntimeException;

public class SpecifiedExceptionTest {
  @Test(expected = SampleRuntimeException.class)
  public void itThrowsExpectedConfiguredException() {
    ImmutableSpecifiedException.builder().build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void itThrowsSpecifiedExceptionOnBuild() {
    ImmutableSpecifiedException.builder().build(IllegalArgumentException::new);
  }
}
