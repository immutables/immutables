package org.immutables.fixture.nullable.typeuse;

import org.immutables.value.Value;
import org.jspecify.annotations.Nullable;

/**
 * Reproducer for TYPE_USE nullable annotations on arrays.
 *
 * Case 1: Parent declares the array methods, child overrides.
 * Case 2: Parent does NOT declare array methods, child adds them.
 */
public interface NullableArrays {

  // Parent has these
  byte @Nullable [] primitiveArray();
  String @Nullable [] referenceArray();

  // Case 1: child overrides parent's array methods
  @Value.Immutable
  @Value.Style(jdkOnly = true)
  interface ChildOverrides extends NullableArrays {
    @Override
    byte @Nullable [] primitiveArray();

    @Override
    String @Nullable [] referenceArray();
  }

  // A parent without array methods
  interface EmptyParent {
    String name();
  }

  // Case 2: child introduces array methods not in parent
  @Value.Immutable
  @Value.Style(jdkOnly = true)
  interface ChildIntroduces extends EmptyParent {
    byte @Nullable [] primitiveArray();
    String @Nullable [] referenceArray();
  }
}
