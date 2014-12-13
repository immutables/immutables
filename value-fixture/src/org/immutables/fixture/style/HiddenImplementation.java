package org.immutables.fixture.style;

import org.immutables.value.Value.Immutable.ImplementationVisibility;
import org.immutables.value.Value;

/**
 * Feature combination
 * <ul>
 * <li>Private implementation
 * <li>Outside builder
 * <li>Builder returns abstract
 * </ul>
 */
@Value.Immutable(visibility = ImplementationVisibility.PRIVATE)
public class HiddenImplementation {

  void use() {
    HiddenImplementation instance = new HiddenImplementationBuilder().build();
    instance.toString();
  }
}
