package org.immutables.fixture.modifiable;

import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, clearBuilder = true)
public interface ClearBuilder {
  boolean a();

  String b();

  List<String> l();

  @Value.Default
  default int d() {
    return 0;
  }

  Set<RetentionPolicy> r();

  Map<String, Integer> m();
}
