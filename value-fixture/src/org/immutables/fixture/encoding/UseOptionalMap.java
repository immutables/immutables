package org.immutables.fixture.encoding;

import javax.annotation.Nullable;
import java.util.List;
import com.google.common.base.Optional;
import java.util.Map;
import org.immutables.encode.fixture.OptionalMapEnabled;
import org.immutables.value.Value;

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
