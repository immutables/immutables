package org.immutables.fixture.jackson;

import javax.annotation.Nullable;
import java.util.Map;
import com.google.common.base.Optional;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.immutables.value.Value;

interface Inherited {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Nullable
  Integer getInteger();

  OptionIncludeNonEmpty withRelationships(Optional<String> number);
}

@Value.Immutable
@JsonDeserialize(as = ImmutableOptionIncludeNonEmpty.class)
public abstract class OptionIncludeNonEmpty implements Inherited {
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public abstract Optional<String> getRelationships();

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public abstract Map<String, Number> getMap();

  // used to test abstract method generation in Json class
  public abstract OptionIncludeNonEmpty withMap(Map<String, ? extends Number> number);
}
