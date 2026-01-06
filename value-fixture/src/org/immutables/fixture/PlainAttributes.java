package org.immutables.fixture;

import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface PlainAttributes {
  // these will be treated as special
  Optional<String> optional();
  List<Object> list();

  // these will be not any special for the annotation processor
  @Value.PlainAttribute
  Optional<String> plain();

  @Value.PlainAttribute
  List<Object> justList();
}
