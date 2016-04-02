package org.immutables.fixture;

import java.util.List;
import org.immutables.value.Value;

interface Param {
  int a();
}

/**
 * Compilation test. Cancel out parameter from allParameters
 * without this annotation, this would result
 * in compilation error: parameters should be defined on a same level
 */
@Value.Immutable
@Value.Style(allParameters = true)
public interface CancelParam extends Param {
  @Value.Parameter(false)
  List<Integer> aux();

  default void use() {
    ImmutableCancelParam.of(1).withAux(1, 2, 3);
  }
}
