package org.immutables.fixture.nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
@JsonDeserialize(as = ImmutableNullableCompact.class)
@Value.Style(allParameters = true)
public abstract class NullableCompact {
  @Nullable
  public abstract Integer[] array();

  @Nullable
  public abstract Map<String, Object> map();
}
