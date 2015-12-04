package org.immutables.fixture.jackson;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableAnyGetterSetter.class)
public interface AnyGetterSetter {
  @JsonAnyGetter
  public Map<String, Object> getAny();
}
