package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableDefaultCollection.class)
public interface DefaultCollection {
  @Nullable
  List<String> nullable();

  @Value.Default
  default List<String> defaults() {
    return Collections.singletonList("");
  }

  @Value.Default
  @Nullable
  default List<Integer> defnullable() {
    return null;
  }
}
