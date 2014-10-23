package org.immutables.fixture;

import java.util.List;
import org.immutables.value.Value;
import org.immutables.json.Json;

@Value.Immutable
@Json.Marshaled
public interface JsonIgnore {
  @Value.Parameter
  int value();

  @Json.Ignore
  List<Integer> values();
}
