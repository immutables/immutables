package org.immutables.fixture.nullable;

import java.util.Arrays;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class NullableAttributesTest {
  @Test
  public void defaultValues() {
    ImmutableNullableAttributes a1 = ImmutableNullableAttributes.builder()
        .build();

    check(a1.array()).isNull();
    check(a1.integer()).isNull();
    check(a1.defArray()).isOf(Double.NaN);
    check(a1.list()).isNull();
    check(a1.set()).notNull();
    check(a1.set()).isEmpty();
    check(a1.map()).isNull();
  }

  @Test
  public void explicitNulls() {
    ImmutableNullableAttributes a1 = ImmutableNullableAttributes.builder()
        .set(null)
        .defArray((Double[]) null)
        .build();

    check(a1.defArray()).isNull();
    check(a1.set()).isNull();
  }

  @Test
  public void nonDefaultValues() {
    ImmutableNullableAttributes a1 = ImmutableNullableAttributes.builder()
        .addSet(1)
        .defArray(1.0, 2.0)
        .addList("a")
        .addAllList(Arrays.asList("b", "c"))
        .putMap("key", new Object())
        .build();

    check(a1.set()).isOf(1);
    check(a1.defArray()).isOf(1.0, 2.0);
    check(a1.list()).isOf("a", "b", "c");
    check(a1.map().keySet()).isOf("key");
  }

  @Test
  public void compactConstruction() {
    ImmutableNullableCompact c1 = ImmutableNullableCompact.builder()
        .build();

    check(ImmutableNullableCompact.of(null, null)).is(c1);
  }

  @Test(expected = NullPointerException.class)
  public void nonnullDefaultBlowupOnNull() {
    ImmutableNonnullConstruction.builder()
        .arr()
        .build();
  }

  @Test
  public void nonnullDefault() {
    check(ImmutableNonnullConstruction.builder()
        .arr()
        .addAx("a")
        .addAx("b", "c")
        .build().ax())
        .isOf("a", "b", "c");
  }
}
