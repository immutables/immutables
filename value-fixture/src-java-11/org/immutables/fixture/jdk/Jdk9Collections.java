package org.immutables.fixture.jdk;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdk9Collections = true)
public interface Jdk9Collections {
  Set<String> s();
  List<String> l();
  Map<String, Integer> m();
}
