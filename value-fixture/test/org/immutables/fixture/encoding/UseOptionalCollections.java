package org.immutables.fixture.encoding;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.immutables.fixture.encoding.defs.OptionalListEnabled;
import org.immutables.fixture.encoding.defs.OptionalMapEnabled;
import org.immutables.value.Value;

@Value.Style(depluralize = true)
@OptionalMapEnabled
@OptionalListEnabled
@JsonDeserialize(as = ImmutableUseOptionalCollections.class)
@Value.Immutable
public interface UseOptionalCollections<V> {

  @Value.Parameter
  Optional<List<String>> as();

  @Value.Parameter
  Optional<List<V>> bs();

  @Value.Parameter
  Optional<Map<String, V>> maybeMap();
}
