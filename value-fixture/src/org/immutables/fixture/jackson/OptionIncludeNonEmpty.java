package org.immutables.fixture.jackson;

import java.util.Map;
import com.google.common.base.Optional;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableOptionIncludeNonEmpty.class)
public abstract class OptionIncludeNonEmpty {
  @Value.Parameter
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public abstract Optional<String> getRelationships();

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public abstract Map<String, Number> getMap();

  // used to test abstract method generation in Json class
  public abstract OptionIncludeNonEmpty withMap(Map<String, ? extends Number> number);
}
