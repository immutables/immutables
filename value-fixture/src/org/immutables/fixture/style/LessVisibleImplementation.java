package org.immutables.fixture.style;

import org.immutables.fixture.style.ImmutableLessVisibleImplementation.LessVisibleImplementationBuilder;
import org.immutables.value.Value.Immutable.ImplementationVisibility;
import org.immutables.value.Value;

/**
 * Feature combination
 * <ul>
 * <li>Less visible implementation
 * <li>Builder returns abstract
 * </ul>
 */
@Value.Immutable(visibility = ImplementationVisibility.PACKAGE)
public class LessVisibleImplementation {

  void use() {
    LessVisibleImplementationBuilder returnsAbstract = ImmutableLessVisibleImplementation.builder();
    returnsAbstract.build();
  }
}
