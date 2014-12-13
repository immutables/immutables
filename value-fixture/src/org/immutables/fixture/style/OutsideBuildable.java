package org.immutables.fixture.style;

import org.immutables.value.Value;
import org.immutables.value.Value.Immutable.ImplementationVisibility;

/**
 * Feature combination
 * <ul>
 * <li>Builder outside generated with disambiguation pattern.
 * <li>uses factory method.
 * </ul>
 */
@Value.Immutable(visibility = ImplementationVisibility.PRIVATE)
public class OutsideBuildable {

  void use() {
    OutsideBuildableBuilder.builder().build();
  }
}

/**
 * Feature combination
 * <ul>
 * <li>Builder outside generated with disambiguation pattern.
 * <li>uses factory method.
 * </ul>
 */
@Value.Immutable(visibility = ImplementationVisibility.PRIVATE)
@Value.Style(builder = "new")
class OutsideBuildableNew {

  void use() {
    new OutsideBuildableNewBuilder().build();
  }
}
