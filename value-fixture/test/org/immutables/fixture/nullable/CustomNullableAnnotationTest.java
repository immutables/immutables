package org.immutables.fixture.nullable;

import static org.immutables.check.Checkers.check;

import org.junit.jupiter.api.Test;

public class CustomNullableAnnotationTest {

  @Test
  public void shouldAllowNulls() {
    ImmutableCustomNullableAnnotation customNullableAnnotation = ImmutableCustomNullableAnnotation.builder()
        .string1(null)
        .string2("non null")
        .build();

    check(customNullableAnnotation.string1()).isNull();
    check(customNullableAnnotation.string2()).is("non null");
  }

  @Test
  public void banNulls() {
    ImmutableCustomNullableAnnotation.Builder customNullableAnnotation =
        ImmutableCustomNullableAnnotation.builder();

    try {
      customNullableAnnotation.string2(null);
      check(false);
    } catch (NullPointerException e) {}
  }
}
