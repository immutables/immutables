package org.immutables.fixture.encoding;

import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import org.immutables.fixture.encoding.defs.OptionalList2Enabled;

@Value.Style(depluralize = true)
@OptionalList2Enabled
@Value.Immutable
public interface UseOptionalCollections2<V> {

  @Value.Parameter
  Optional<List<String>> as();

  @Value.Parameter
  Optional<List<V>> bs();
}
