package org.immutables.fixture.style;

import org.immutables.value.Value;
import org.immutables.value.Value.Immutable.ImplementationVisibility;

/**
 * Feature combination
 * <ul>
 * <li>Nested Builder using constructor "new" invokation
 * </ul>
 */
@Value.Nested
@Value.Style(
    builder = "new",
    defaults = @Value.Immutable(visibility = ImplementationVisibility.PRIVATE))
public abstract class EnclosingBuilderNew {
  @Value.Immutable
  public static class Hidden {}

  void use() {
    new ImmutableEnclosingBuilderNew.HiddenBuilder().build();
  }
}
