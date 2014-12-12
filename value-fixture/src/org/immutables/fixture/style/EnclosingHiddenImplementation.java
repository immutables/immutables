package org.immutables.fixture.style;

import org.immutables.value.Value.Immutable.ImplementationVisibility;
import org.immutables.value.Value;

@Value.Style(defaults = @Value.Immutable(visibility = ImplementationVisibility.PRIVATE))
@interface Priv {}

/**
 * Feature combination
 * <ul>
 * <li>Nested hidded implementation
 * <li>Defaults mechanism, implementation style override *
 * <li>Style meta-annotation usage to create style
 * </ul>
 */
@Value.Nested
@Priv
public class EnclosingHiddenImplementation {
  @Value.Immutable
  public static class HiddenImplementation {}

  void use() {
    HiddenImplementation instance =
        new ImmutableEnclosingHiddenImplementation.HiddenImplementationBuilder().build();

    instance.toString();
  }
}
