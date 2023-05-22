package org.immutables.fixture.generics;

import java.util.Map;
import javax.validation.constraints.Size;
import org.immutables.value.Value;

@Value.Immutable
public interface HasTypeAnnotationWithAttributes {
  Map<@Size(min = 1, max = 99) String, Integer> map();
}
