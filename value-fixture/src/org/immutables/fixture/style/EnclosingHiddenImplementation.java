package org.immutables.fixture.style;

import com.google.common.base.Optional;
import org.immutables.value.Value.Immutable.ImplementationVisibility;
import org.immutables.value.Value;

@Value.Style(
    typeImmutableEnclosing = "EnclosingFactory",
    instance = "singletonInstance",
    of = "new*",
    builder = "create",
    defaults = @Value.Immutable(singleton = true, visibility = ImplementationVisibility.PRIVATE))
@interface Priv {}

/**
 * Feature combination
 * <ul>
 * <li>Nested hidded implementation
 * <li>Defaults mechanism, implementation style override
 * <li>Style meta-annotation usage to create style
 * <li>Forwarding factory methods with names customization and auto-disambiguation
 * </ul>
 */
@Value.Nested
@Priv
public abstract class EnclosingHiddenImplementation {
  @Value.Immutable
  public static class HiddenImplementation {}

  @Value.Immutable
  public static abstract class NonexposedImplementation {
    @Value.Parameter
    public abstract int cons();
  }

  @Value.Immutable(builder = false, visibility = ImplementationVisibility.SAME)
  public static abstract class VisibleImplementation {
    @Value.Parameter
    public abstract Optional<Integer> cons();
  }

  void use() {
    EnclosingFactory.HiddenImplementationBuilder.create().build();

    // Type name suffix was added to avoid name clashing
    EnclosingFactory.singletonInstanceHiddenImplementation();
    EnclosingFactory.singletonInstanceNonexposedImplementation();
    // Strictly follows naming template
    EnclosingFactory.newNonexposedImplementation(11);
    // Implementation is visible
    EnclosingFactory.VisibleImplementation.newVisibleImplementation(Optional.<Integer>absent());
  }
}
