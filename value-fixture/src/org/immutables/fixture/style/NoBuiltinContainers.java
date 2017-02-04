package org.immutables.fixture.style;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builtinContainerAttributes = false)
public interface NoBuiltinContainers {
  Map<Integer, Double> a();

  List<String> b();

  Optional<String> c();
}
