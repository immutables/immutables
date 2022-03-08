package org.immutables.fixture;

import java.util.Map;
import org.immutables.value.Value;

@Value.Style(jdkOnly = true)
@Value.Immutable
public interface EntityWithMap {
  Map<String, String> properties();
}
