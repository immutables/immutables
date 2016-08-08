package org.immutables.fixture.encoding;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.encode.fixture.OptionalMapEnabled;
import org.immutables.value.Value;

@JsonDeserialize(as = ImmutableUseOptionalMap.class)
@OptionalMapEnabled
@Value.Immutable
public interface UseOptionalMap<V> {
  // @Value.Parameter
  Optional<Map<String, V>> maybeMap();

  @Value.Default
  default @Nullable List<String> strings() {
    return null;
  }
}
