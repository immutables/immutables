package org.immutables.fixture;

import org.immutables.value.Value;

/**
 * Test how initialization is generated in a interning and nobuilder
 * combination to avoid regression in having ExceptionInitializationError.
 */
@Value.Immutable(intern = true, copy = true, builder = false)
public interface ProperInitInternNoBuilder {
  @Value.Default
  default int x() {
    return 0;
  }
}
