package org.immutables.fixture.generics;

import static org.immutables.check.Checkers.check;

import org.junit.Test;

public class GenericCoreTest {
  @Test
  public void test() {
    final String object = "foo";
    final int id = 1;

    ImmutableGenericChild<String> child = ImmutableGenericChild.<String>builder()
        .object(object)
        .build();

    ImmutableGenericAdult<String> adultFromChild = ImmutableGenericAdult.<String>builder()
        .from(child)
        .id(id)
        .build();

    ImmutableGenericAdult<String> adultFromScratch = ImmutableGenericAdult.<String> builder()
        .object(object)
        .id(id)
        .build();

    check(adultFromChild).is(adultFromScratch);
  }
}
