package org.immutables.fixture.style;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Style(forceEqualsInWithers = true)
@Value.Immutable
public interface ForceEqualsInWithers {
  float f();
  double d();
  String s();
  boolean b();
  char c();
  Optional<String> o();
  List<Integer> l();
  @Nullable Set<String> st();
  Map<String, Integer> mp();
}
